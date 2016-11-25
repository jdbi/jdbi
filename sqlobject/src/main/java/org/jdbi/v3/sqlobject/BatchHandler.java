/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleSupplier;
import org.jdbi.v3.core.PreparedBatch;
import org.jdbi.v3.core.ResultBearing;
import org.jdbi.v3.core.ResultIterator;
import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.StatementExecutor;
import org.jdbi.v3.core.exception.UnableToCreateStatementException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.sqlobject.customizers.BatchChunkSize;
import org.jdbi.v3.sqlobject.exceptions.UnableToCreateSqlObjectException;

class BatchHandler extends CustomizingStatementHandler
{
    private final Class<?> sqlObjectType;
    private final SqlBatch sqlBatch;
    private final ChunkSizeFunction batchChunkSize;
    private final Function<PreparedBatch, ResultIterator<?>> batchIntermediate;
    private final ResultReturner magic;

    BatchHandler(Class<?> sqlObjectType, Method method)
    {
        super(sqlObjectType, method);
        this.sqlObjectType = sqlObjectType;

        this.sqlBatch = method.getAnnotation(SqlBatch.class);
        this.batchChunkSize = determineBatchChunkSize(sqlObjectType, method);
        final GetGeneratedKeys getGeneratedKeys = method.getAnnotation(GetGeneratedKeys.class);

        if (getGeneratedKeys == null) {
            if (!returnTypeIsValid(method.getReturnType()) ) {
                throw new UnableToCreateSqlObjectException(invalidReturnTypeMessage(method));
            }
            batchIntermediate = PreparedBatch::executeAndGetModCount;
            magic = ResultReturner.forOptionalReturn(sqlObjectType, method);
        }
        else {
            magic = ResultReturner.forMethod(sqlObjectType, method);
            final Function<StatementContext, RowMapper<?>> mapper = ctx -> ResultReturner.rowMapperFor(getGeneratedKeys, magic.elementType(ctx));
            if (getGeneratedKeys.columnName().isEmpty()) {
                batchIntermediate = batch -> batch.executeAndGenerateKeys(mapper.apply(batch.getContext())).iterator();
            } else {
                batchIntermediate = batch -> batch.executeAndGenerateKeys(mapper.apply(batch.getContext()), getGeneratedKeys.columnName()).iterator();
            }
        }
    }

    private ChunkSizeFunction determineBatchChunkSize(Class<?> sqlObjectType, Method method)
    {
        // this next big if chain determines the batch chunk size. It looks from most specific
        // scope to least, that is: as an argument, then on the method, then on the class,
        // then default to Integer.MAX_VALUE

        int batchChunkSizeParameterIndex;
        if ((batchChunkSizeParameterIndex = indexOfBatchChunkSizeParameter(method)) >= 0) {
            return new ParamBasedChunkSizeFunction(batchChunkSizeParameterIndex);
        }
        else if (method.isAnnotationPresent(BatchChunkSize.class)) {
            final int size = method.getAnnotation(BatchChunkSize.class).value();
            if (size <= 0) {
                throw new IllegalArgumentException("Batch chunk size must be >= 0");
            }
            return new ConstantChunkSizeFunction(size);
        }
        else if (sqlObjectType.isAnnotationPresent(BatchChunkSize.class)) {
            final int size = sqlObjectType.getAnnotation(BatchChunkSize.class).value();
            return new ConstantChunkSizeFunction(size);
        }
        else {
            return new ConstantChunkSizeFunction(Integer.MAX_VALUE);
        }
    }

    private int indexOfBatchChunkSizeParameter(Method method)
    {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        return IntStream.range(0, parameterAnnotations.length)
                        .filter(i -> Stream.of(parameterAnnotations[i]).anyMatch(BatchChunkSize.class::isInstance))
                        .findFirst()
                        .orElse(-1);
    }

    @Override
    public Object invoke(Object target, Method method, Object[] args, HandleSupplier h)
    {
        final Handle handle = h.getHandle();
        final String sql = handle.getConfig().get(SqlObjects.class)
                .getSqlLocator().locate(sqlObjectType, method);
        final int chunkSize = batchChunkSize.call(args);
        final Iterator<Object[]> batchArgs = zipArgs(args);

        ResultIterator<Object> result = new ResultIterator<Object>() {
            ResultIterator<?> batchResult;

            {
                hasNext(); // Ensure our batchResult is prepared, so we can get its context
            }

            @Override
            public boolean hasNext() {
                // first, any elements already buffered?
                if (batchResult != null) {
                    if (batchResult.hasNext()) {
                        return true;
                    }
                    // no more in this chunk, release resources
                    batchResult.close();
                }
                // more chunks?
                if (!batchArgs.hasNext()) {
                    return false;
                }
                // execute a single chunk and buffer
                PreparedBatch batch = handle.prepareBatch(sql);
                applyCustomizers(batch, args);
                for (int i = 0; i < chunkSize && batchArgs.hasNext(); i++) {
                    applyBinders(batch.add(), batchArgs.next());
                }
                batchResult = executeBatch(handle, batch);
                return hasNext(); // recurse to ensure we actually got elements
            }

            @Override
            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return batchResult.next();
            }

            @Override
            public StatementContext getContext() {
                return batchResult.getContext();
            }

            @Override
            public void close() {
                batchResult.close();
            }
        };


        return magic.result(new ResultBearing<Object>() {
            @Override
            public <R> R execute(StatementExecutor<Object, R> executor) {
                throw new UnsupportedOperationException(
                        "@SqlBatch currently does not support custom execution modes like reduce");
            }

            @Override
            public ResultIterator<Object> iterator() {
                return result;
            }

            @Override
            public StatementContext getContext() {
                return result.getContext();
            }
        });
    }

    private Iterator<Object[]> zipArgs(Object[] args) {
        boolean foundIterator = false;
        List<Iterator<?>> extras = new ArrayList<>();
        for (final Object arg : args) {
            if (arg instanceof Iterable) {
                extras.add(((Iterable<?>) arg).iterator());
                foundIterator = true;
            }
            else if (arg instanceof Iterator) {
                extras.add((Iterator<?>) arg);
                foundIterator = true;
            }
            else if (arg.getClass().isArray()) {
                extras.add(Arrays.asList((Object[])arg).iterator());
                foundIterator = true;
            }
            else {
                extras.add(Stream.generate(() -> arg).iterator());
            }
        }

        if (!foundIterator) {
            throw new UnableToCreateStatementException("@SqlBatch method has no Iterable or array parameters,"
                    + " did you mean @SqlQuery?", null, null);
        }

        final Object[] sharedArg = new Object[args.length];
        return new Iterator<Object[]>() {
            @Override
            public boolean hasNext() {
                for (Iterator<?> extra : extras) {
                    if (!extra.hasNext()) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public Object[] next() {
                for (int i = 0; i < extras.size(); i++) {
                    sharedArg[i] = extras.get(i).next();
                }
                return sharedArg;
            }
        };
    }

    private ResultIterator<?> executeBatch(final Handle handle, final PreparedBatch batch)
    {
        if (!handle.isInTransaction() && sqlBatch.transactional()) {
            // it is safe to use same prepared batch as the inTransaction passes in the same
            // Handle instance.
            return handle.inTransaction((conn, status) -> batchIntermediate.apply(batch));
        }
        else {
            return batchIntermediate.apply(batch);
        }
    }

    private interface ChunkSizeFunction
    {
        int call(Object[] args);
    }

    private static class ConstantChunkSizeFunction implements ChunkSizeFunction
    {
        private final int value;

        ConstantChunkSizeFunction(int value) {
            this.value = value;
        }

        @Override
        public int call(Object[] args)
        {
            return value;
        }
    }

    private static class ParamBasedChunkSizeFunction implements ChunkSizeFunction
    {
        private final int index;

        ParamBasedChunkSizeFunction(int index) {
            this.index = index;
        }

        @Override
        public int call(Object[] args)
        {
            return (Integer)args[index];
        }
    }

    private static boolean returnTypeIsValid(Class<?> type) {
        return type.equals(Void.TYPE)
                || type.isArray() && type.getComponentType().equals(Integer.TYPE);

    }

    private static String invalidReturnTypeMessage(Method method) {
        return method.getDeclaringClass() + "." + method.getName() +
                " method is annotated with @SqlBatch so should return void or int[] but is returning: " +
                method.getReturnType();
    }
}
