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
package org.jdbi.sqlobject.statement.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.AttachedExtensionHandler;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.internal.IterableLike;
import org.jdbi.core.internal.JdbiClassUtils;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.result.ResultIterator;
import org.jdbi.core.statement.PreparedBatch;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.UnableToCreateStatementException;
import org.jdbi.sqlobject.SingleValue;
import org.jdbi.sqlobject.UnableToCreateSqlObjectException;
import org.jdbi.sqlobject.statement.BatchChunkSize;
import org.jdbi.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.sqlobject.statement.UseRowReducer;

import static java.lang.String.format;

public class SqlBatchHandler extends CustomizingStatementHandler<PreparedBatch> {
    private final SqlBatch sqlBatch;
    private final SqlBatchHandler.ChunkSizeFunction batchChunkSize;
    private final Function<PreparedBatch, ResultIterator<?>> batchIntermediate;
    private final ResultReturner resultReturner;

    public SqlBatchHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);

        if (method.isAnnotationPresent(UseRowReducer.class)) {
            throw new UnsupportedOperationException("Cannot declare @UseRowReducer on a @SqlUpdate method.");
        }

        this.sqlBatch = method.getAnnotation(SqlBatch.class);
        this.batchChunkSize = determineBatchChunkSize(sqlObjectType, method);
        final GetGeneratedKeys getGeneratedKeys = method.getAnnotation(GetGeneratedKeys.class);

        if (getGeneratedKeys == null) {
            if (!returnTypeIsValid(method.getReturnType())) {
                throw new UnableToCreateSqlObjectException(invalidReturnTypeMessage(method));
            }
            Function<PreparedBatch, ResultIterator<?>> modCounts = PreparedBatch::executeAndGetModCount;
            batchIntermediate = method.getReturnType().equals(boolean[].class)
                    ? mapToBoolean(modCounts)
                    : modCounts;
            resultReturner = ResultReturner.forOptionalReturn(sqlObjectType, method);
        } else {
            String[] columnNames = getGeneratedKeys.value();
            resultReturner = ResultReturner.forMethod(sqlObjectType, method);

            if (method.isAnnotationPresent(UseRowMapper.class)) {
                RowMapper<?> mapper = rowMapperFor(method.getAnnotation(UseRowMapper.class));
                batchIntermediate = batch -> batch.executePreparedBatch(columnNames)
                        .map(mapper)
                        .iterator();
            } else {
                batchIntermediate = batch -> batch.executePreparedBatch(columnNames)
                        .mapTo(resultReturner.elementType(batch.getConfig()))
                        .iterator();
            }
        }
    }

    private Function<PreparedBatch, ResultIterator<?>> mapToBoolean(Function<PreparedBatch, ResultIterator<?>> modCounts) {
        return modCounts.andThen(iterator -> new ResultIterator<Boolean>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Boolean next() {
                return ((Integer) iterator.next()) > 0;
            }

            @Override
            public void close() {
                iterator.close();
            }

            @Override
            public StatementContext getContext() {
                return iterator.getContext();
            }
        });
    }

    private SqlBatchHandler.ChunkSizeFunction determineBatchChunkSize(Class<?> sqlObjectType, Method method) {
        // this next big if chain determines the batch chunk size. It looks from most specific
        // scope to least, that is: as an argument, then on the method, then on the class,
        // then default to Integer.MAX_VALUE

        int batchChunkSizeParameterIndex = indexOfBatchChunkSizeParameter(method);
        if (batchChunkSizeParameterIndex >= 0) {
            return new ParamBasedChunkSizeFunction(batchChunkSizeParameterIndex);
        } else if (method.isAnnotationPresent(BatchChunkSize.class)) {
            final int size = method.getAnnotation(BatchChunkSize.class).value();
            if (size <= 0) {
                throw new IllegalArgumentException("Batch chunk size must be >= 0");
            }
            return new ConstantChunkSizeFunction(size);
        } else if (sqlObjectType.isAnnotationPresent(BatchChunkSize.class)) {
            final int size = sqlObjectType.getAnnotation(BatchChunkSize.class).value();
            return new ConstantChunkSizeFunction(size);
        } else {
            return new ConstantChunkSizeFunction(Integer.MAX_VALUE);
        }
    }

    private int indexOfBatchChunkSizeParameter(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        return IntStream.range(0, parameterAnnotations.length)
                .filter(i -> Stream.of(parameterAnnotations[i]).anyMatch(BatchChunkSize.class::isInstance))
                .findFirst()
                .orElse(-1);
    }

    @Override
    PreparedBatch createStatement(Handle handle, String locatedSql) {
        return handle.prepareBatch(locatedSql);
    }

    @Override
    void configureReturner(PreparedBatch stmt, SqlObjectStatementConfiguration cfg) {}

    @Override
    Type getParameterType(Parameter parameter) {
        final Type type = super.getParameterType(parameter);

        if (parameter.isAnnotationPresent(SingleValue.class)) {
            return type;
        }

        final Class<?> erasedType = GenericTypes.getErasedType(type);

        final Type resultType;
        if (Iterable.class.isAssignableFrom(erasedType)) {
            resultType = GenericTypes.findGenericParameter(type, Iterable.class).orElse(null);
        } else if (Iterator.class.isAssignableFrom(erasedType)) {
            resultType = GenericTypes.findGenericParameter(type, Iterator.class).orElse(null);
        } else if (GenericTypes.isArray(type)) {
            resultType = ((Class<?>) type).getComponentType();
        } else {
            resultType = type;
        }

        if (resultType == null) {
            throw new IllegalStateException(format("type '%s' must have a generic parameter", type));
        }

        return resultType;
    }

    @Override
    public AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
        final AttachedExtensionHandler superInvoker = super.attachTo(config, target);
        final Supplier<String> sqlSupplier = locateSql(config);
        return new AttachedExtensionHandler() {
            @Override
            public Object invoke(HandleSupplier handleSupplier, Object... args) {
                final String sql = sqlSupplier.get();
                final Object[] safeArgs = JdbiClassUtils.safeVarargs(args);
                final Handle handle = handleSupplier.getHandle();
                final int chunkSize = batchChunkSize.call(safeArgs);
                final Iterator<Object[]> batchArgs = zipArgs(getMethod(), safeArgs);

                final class BatchChunkIterator implements ResultIterator<Object> {
                    private ResultIterator<?> batchResult = null;
                    private boolean closed = false;

                    BatchChunkIterator() {
                        if (batchArgs.hasNext()) {
                            // if arguments are present, preload the next result chunk
                            batchResult = loadChunk();
                        }
                    }

                    private ResultIterator<?> loadChunk() {
                        // execute a single chunk and buffer
                        List<Object[]> currArgs = new ArrayList<>();
                        for (int i = 0; i < chunkSize && batchArgs.hasNext(); i++) {
                            currArgs.add(batchArgs.next());
                        }
                        Supplier<PreparedBatch> preparedBatchSupplier = () -> createPreparedBatch(handle, sql, currArgs);
                        return executeBatch(handle, preparedBatchSupplier);
                    }

                    private PreparedBatch createPreparedBatch(Handle handle, String sql, List<Object[]> currArgs) {
                        PreparedBatch batch = handle.prepareBatch(sql);
                        for (Object[] currArg : currArgs) {
                            applyCustomizers(batch, currArg);
                            batch.add();
                        }
                        return batch;
                    }

                    @Override
                    public boolean hasNext() {
                        if (closed) {
                            throw new IllegalStateException("closed");
                        }
                        // first, any elements already buffered?
                        if (batchResult != null) {
                            if (batchResult.hasNext()) {
                                return true;
                            }
                            // no more in this chunk, release resources
                            batchResult.close();
                        }
                        // more chunks?
                        if (batchArgs.hasNext()) {
                            // preload the next result chunk
                            batchResult = loadChunk();

                            // recurse to ensure we actually got elements
                            return hasNext();
                        }

                        return false;
                    }

                    @Override
                    public Object next() {
                        if (closed) {
                            throw new IllegalStateException("closed");
                        }
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
                        closed = true;
                        batchResult.close();
                    }
                }

                ResultIterator<Object> result;

                if (batchArgs.hasNext()) {
                    result = new BatchChunkIterator();
                } else {
                    // only created to get access to the context.
                    PreparedBatch dummy = handle.prepareBatch(sql);
                    result = new ResultIterator<>() {
                        @Override
                        public void close() {
                            // no op
                        }

                        @Override
                        public StatementContext getContext() {
                            return dummy.getContext();
                        }

                        @Override
                        public boolean hasNext() {
                            return false;
                        }

                        @Override
                        public Object next() {
                            throw new NoSuchElementException();
                        }
                    };
                }

                ResultIterable<Object> iterable = ResultIterable.of(result);

                return resultReturner.mappedResult(iterable, result.getContext());
            }

            @Override
            public void warm(ConfigRegistry config) {
                superInvoker.warm(config);
                resultReturner.warm(config);
            }
        };
    }

    private Iterator<Object[]> zipArgs(Method method, Object[] args) {
        boolean foundIterator = false;
        List<Iterator<?>> extras = new ArrayList<>();
        for (int paramIdx = 0; paramIdx < method.getParameterCount(); paramIdx++) {
            final boolean singleValue = method.getParameters()[paramIdx].isAnnotationPresent(SingleValue.class);
            final Object arg = args[paramIdx];
            if (!singleValue && IterableLike.isIterable(arg)) {
                extras.add(IterableLike.of(arg));
                foundIterator = true;
            } else {
                extras.add(Stream.generate(() -> arg).iterator());
            }
        }

        if (!foundIterator) {
            throw new UnableToCreateStatementException("@SqlBatch method has no Iterable or array parameters,"
                    + " did you mean @SqlQuery?", null, null);
        }

        return new Iterator<>() {
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
                final Object[] argsArray = new Object[args.length];
                for (int i = 0; i < extras.size(); i++) {
                    argsArray[i] = extras.get(i).next();
                }
                return argsArray;
            }
        };
    }

    private ResultIterator<?> executeBatch(final Handle handle, final Supplier<PreparedBatch> preparedBatchSupplier) {
        if (!handle.isInTransaction() && sqlBatch.transactional()) {
            // it is safe to use same prepared preparedBatchSupplier as the inTransaction passes in the same
            // Handle instance.
            return handle.inTransaction(c -> batchIntermediate.apply(preparedBatchSupplier.get()));
        } else {
            return batchIntermediate.apply(preparedBatchSupplier.get());
        }
    }

    private static boolean returnTypeIsValid(Class<?> type) {
        if (type.equals(Void.TYPE)) {
            return true;
        }

        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            return componentType.equals(Integer.TYPE) || componentType.equals(Boolean.TYPE);
        }

        return false;
    }

    private static String invalidReturnTypeMessage(Method method) {
        return method.getDeclaringClass() + "." + method.getName()
                + " method is annotated with @SqlBatch so should return void, int[], or boolean[] but is returning: "
                + method.getReturnType();
    }

    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
    private interface ChunkSizeFunction {
        int call(Object[] args);
    }

    private static class ConstantChunkSizeFunction implements SqlBatchHandler.ChunkSizeFunction {
        private final int value;

        ConstantChunkSizeFunction(int value) {
            this.value = value;
        }

        @Override
        public int call(Object[] args) {
            return value;
        }
    }

    private static class ParamBasedChunkSizeFunction implements SqlBatchHandler.ChunkSizeFunction {
        private final int index;

        ParamBasedChunkSizeFunction(int index) {
            this.index = index;
        }

        @Override
        public int call(Object[] args) {
            return (Integer) args[index];
        }
    }
}
