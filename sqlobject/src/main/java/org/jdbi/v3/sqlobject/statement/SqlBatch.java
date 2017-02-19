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
package org.jdbi.v3.sqlobject.statement;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.internal.IterableLike;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.SqlMethodAnnotation;
import org.jdbi.v3.sqlobject.UnableToCreateSqlObjectException;

/**
 * Annotate a method to indicate that it will create and execute a SQL batch. At least one
 * bound argument must be an Iterator or Iterable, values from this will be taken and applied
 * to each row of the batch. Non iterable bound arguments will be treated as constant values and
 * bound to each row.
 * <p>
 * Unfortunately, because of how batches work, statement customizers and sql statement customizers
 * which affect SQL generation will *not* work with batches. This primarily effects statement location
 * and rewriting, which will always use the values defined on the bound Handle.
 * <p>
 * If you want to chunk up the logical batch into a number of smaller batches (say around 1000 rows at
 * a time in order to not wreck havoc on the transaction log, you should see
 * {@link BatchChunkSize}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SqlMethodAnnotation(SqlBatch.Impl.class)
public @interface SqlBatch {
    /**
     * @return the SQL string (or name)
     */
    String value() default "";

    /**
     * @return whether to execute the batch chunks in a transaction. Default is true (and it will be strange if you
     * want otherwise).
     */
    boolean transactional() default true;

    class Impl extends CustomizingStatementHandler<PreparedBatch> {
        private final SqlBatch sqlBatch;
        private final ChunkSizeFunction batchChunkSize;
        private final Function<PreparedBatch, ResultIterator<?>> batchIntermediate;
        private final ResultReturner magic;

        public Impl(Class<?> sqlObjectType, Method method) {
            super(sqlObjectType, method);

            this.sqlBatch = method.getAnnotation(SqlBatch.class);
            this.batchChunkSize = determineBatchChunkSize(sqlObjectType, method);
            final GetGeneratedKeys getGeneratedKeys = method.getAnnotation(GetGeneratedKeys.class);

            if (getGeneratedKeys == null) {
                if (!returnTypeIsValid(method.getReturnType())) {
                    throw new UnableToCreateSqlObjectException(invalidReturnTypeMessage(method));
                }
                batchIntermediate = PreparedBatch::executeAndGetModCount;
                magic = ResultReturner.forOptionalReturn(sqlObjectType, method);
            } else {
                String[] columnNames = getGeneratedKeys.value();
                magic = ResultReturner.forMethod(sqlObjectType, method);

                if (method.isAnnotationPresent(UseRowMapper.class)) {
                    RowMapper<?> mapper = rowMapperFor(method.getAnnotation(UseRowMapper.class));
                    batchIntermediate = batch -> batch.executeAndReturnGeneratedKeys(columnNames)
                            .map(mapper)
                            .iterator();
                }
                else {
                    batchIntermediate = batch -> batch.executeAndReturnGeneratedKeys(columnNames)
                            .mapTo(magic.elementType(batch.getContext()))
                            .iterator();
                }
            }
        }

        private ChunkSizeFunction determineBatchChunkSize(Class<?> sqlObjectType, Method method) {
            // this next big if chain determines the batch chunk size. It looks from most specific
            // scope to least, that is: as an argument, then on the method, then on the class,
            // then default to Integer.MAX_VALUE

            int batchChunkSizeParameterIndex;
            if ((batchChunkSizeParameterIndex = indexOfBatchChunkSizeParameter(method)) >= 0) {
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
        void configureReturner(PreparedBatch stmt, SqlObjectStatementConfiguration cfg) {
        }

        @Override
        public Object invoke(Object target, Object[] args, HandleSupplier h) {
            final Handle handle = h.getHandle();
            final String sql = locateSql(handle);
            final int chunkSize = batchChunkSize.call(args);
            final Iterator<Object[]> batchArgs = zipArgs(getMethod(), args);

            ResultIterator<Object> result;

            if (batchArgs.hasNext()) {
                result = new ResultIterator<Object>() {
                    ResultIterator<?> batchResult;
                    boolean closed = false;

                    {
                        hasNext(); // Ensure our batchResult is prepared, so we can get its context
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
                        if (!batchArgs.hasNext()) {
                            return false;
                        }
                        // execute a single chunk and buffer
                        PreparedBatch batch = handle.prepareBatch(sql);
                        for (int i = 0; i < chunkSize && batchArgs.hasNext(); i++) {
                            applyCustomizers(batch, batchArgs.next());
                            batch.add();
                        }
                        batchResult = executeBatch(handle, batch);
                        return hasNext(); // recurse to ensure we actually got elements
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
                };
            }
            else {
                PreparedBatch dummy = handle.prepareBatch(sql);
                result = new ResultIterator<Object>() {
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

            return magic.result(iterable, result.getContext());
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

        private ResultIterator<?> executeBatch(final Handle handle, final PreparedBatch batch) {
            if (!handle.isInTransaction() && sqlBatch.transactional()) {
                // it is safe to use same prepared batch as the inTransaction passes in the same
                // Handle instance.
                return handle.inTransaction(c -> batchIntermediate.apply(batch));
            } else {
                return batchIntermediate.apply(batch);
            }
        }

        private interface ChunkSizeFunction {
            int call(Object[] args);
        }

        private static class ConstantChunkSizeFunction implements ChunkSizeFunction {
            private final int value;

            ConstantChunkSizeFunction(int value) {
                this.value = value;
            }

            @Override
            public int call(Object[] args) {
                return value;
            }
        }

        private static class ParamBasedChunkSizeFunction implements ChunkSizeFunction {
            private final int index;

            ParamBasedChunkSizeFunction(int index) {
                this.index = index;
            }

            @Override
            public int call(Object[] args) {
                return (Integer) args[index];
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
}
