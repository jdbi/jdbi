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
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleSupplier;
import org.jdbi.v3.core.PreparedBatch;
import org.jdbi.v3.core.PreparedBatchPart;
import org.jdbi.v3.core.exception.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.customizers.BatchChunkSize;
import org.jdbi.v3.sqlobject.exceptions.UnableToCreateSqlObjectException;

class BatchHandler extends CustomizingStatementHandler
{
    private final Class<?> sqlObjectType;
    private final SqlBatch sqlBatch;
    private final ChunkSizeFunction batchChunkSize;
    private final Function<PreparedBatch, int[]> returner;

    BatchHandler(Class<?> sqlObjectType, Method method)
    {
        super(sqlObjectType, method);
        this.sqlObjectType = sqlObjectType;

        if(!returnTypeIsValid(method.getReturnType()) ) {
            throw new UnableToCreateSqlObjectException(invalidReturnTypeMessage(method));
        }

        this.sqlBatch = method.getAnnotation(SqlBatch.class);
        this.batchChunkSize = determineBatchChunkSize(sqlObjectType, method);
        final GetGeneratedKeys getGeneratedKeys = method.getAnnotation(GetGeneratedKeys.class);
        if (getGeneratedKeys == null) {
            returner = PreparedBatch::execute;
        }
        else if (getGeneratedKeys.columnName().isEmpty()) {
            returner = batch -> toPrimitiveArray(
                    batch.executeAndGenerateKeys(int.class).list());
        }
        else {
            returner = batch -> toPrimitiveArray(
                    batch.executeAndGenerateKeys(int.class, getGeneratedKeys.columnName()).list());
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
    public Object invoke(Object target, Method method, Object[] args, SqlObjectConfig config, HandleSupplier h)
    {
        boolean foundIterator = false;
        Handle handle = h.get();

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

        int processed = 0;
        List<int[]> rs_parts = new ArrayList<>();

        String sql = config.getSqlLocator().locate(sqlObjectType, method);
        PreparedBatch batch = handle.prepareBatch(sql);
        applyCustomizers(batch, args);
        Object[] _args;
        int chunk_size = batchChunkSize.call(args);

        while ((_args = next(extras)) != null) {
            PreparedBatchPart part = batch.add();
            applyBinders(part, _args);

            if (++processed == chunk_size) {
                // execute this chunk
                processed = 0;
                rs_parts.add(executeBatch(handle, batch));
                batch = handle.prepareBatch(sql);
                applyCustomizers(batch, args);
            }
        }

        //execute the rest
        rs_parts.add(executeBatch(handle, batch));

        // combine results
        int end_size = 0;
        for (int[] rs_part : rs_parts) {
            end_size += rs_part.length;
        }
        int[] rs = new int[end_size];
        int offset = 0;
        for (int[] rs_part : rs_parts) {
            System.arraycopy(rs_part, 0, rs, offset, rs_part.length);
            offset += rs_part.length;
        }

        return rs;
    }

    private int[] executeBatch(final Handle handle, final PreparedBatch batch)
    {
        if (!handle.isInTransaction() && sqlBatch.transactional()) {
            // it is safe to use same prepared batch as the inTransaction passes in the same
            // Handle instance.
            return handle.inTransaction((conn, status) -> returner.apply(batch));
        }
        else {
            return returner.apply(batch);
        }
    }

    private static int[] toPrimitiveArray(List<Integer> list)
    {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }

    private static Object[] next(List<Iterator<?>> args)
    {
        List<Object> rs = new ArrayList<>();
        for (Iterator<?> arg : args) {
            if (arg.hasNext()) {
                rs.add(arg.next());
            }
            else {
                return null;
            }
        }
        return rs.toArray();
    }

    private interface Returner
    {
        int[] value(PreparedBatch batch);
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
