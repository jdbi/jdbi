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
package org.jdbi.v3.core.result;

import java.lang.reflect.Type;
import java.sql.Statement;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowViewMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;

/**
 * Extends the {@link ResultBearing} class to provide access to the per-batch row modification counts.
 */
public final class BatchResultBearing implements ResultBearing {

    private final ResultBearing delegate;
    private final Supplier<int[]> modifiedRowCountsSupplier;

    public BatchResultBearing(ResultBearing delegate, Supplier<int[]> modifiedRowCountsSupplier) {
        this.delegate = delegate;
        this.modifiedRowCountsSupplier = modifiedRowCountsSupplier;
    }

    @Override
    public <R> R scanResultSet(ResultSetScanner<R> mapper) {
        return delegate.scanResultSet(mapper);
    }

    @Override
    public <T> BatchResultIterable<T> mapTo(Class<T> type) {
        return BatchResultIterable.of(delegate.mapTo(type), modifiedRowCountsSupplier);
    }

    @Override
    public <T> BatchResultIterable<T> mapTo(GenericType<T> type) {
        return BatchResultIterable.of(delegate.mapTo(type), modifiedRowCountsSupplier);
    }

    @Override
    public BatchResultIterable<?> mapTo(Type type) {
        return BatchResultIterable.of(delegate.mapTo(type), modifiedRowCountsSupplier);
    }

    @Override
    public <T> BatchResultIterable<T> mapTo(QualifiedType<T> type) {
        return BatchResultIterable.of(delegate.mapTo(type), modifiedRowCountsSupplier);
    }

    @Override
    public <T> BatchResultIterable<T> mapToBean(Class<T> type) {
        return BatchResultIterable.of(delegate.mapToBean(type), modifiedRowCountsSupplier);
    }

    @Override
    public BatchResultIterable<Map<String, Object>> mapToMap() {
        return BatchResultIterable.of(delegate.mapToMap(), modifiedRowCountsSupplier);
    }

    @Override
    public <T> BatchResultIterable<Map<String, T>> mapToMap(Class<T> valueType) {
        return BatchResultIterable.of(delegate.mapToMap(valueType), modifiedRowCountsSupplier);
    }

    @Override
    public <T> BatchResultIterable<Map<String, T>> mapToMap(GenericType<T> valueType) {
        return BatchResultIterable.of(delegate.mapToMap(valueType), modifiedRowCountsSupplier);
    }

    @Override
    public <T> BatchResultIterable<T> map(ColumnMapper<T> mapper) {
        return BatchResultIterable.of(delegate.map(mapper), modifiedRowCountsSupplier);
    }

    @Override
    public <T> BatchResultIterable<T> map(RowMapper<T> mapper) {
        return BatchResultIterable.of(delegate.map(mapper), modifiedRowCountsSupplier);
    }

    @Override
    public <T> BatchResultIterable<T> map(RowViewMapper<T> mapper) {
        return BatchResultIterable.of(delegate.map(mapper), modifiedRowCountsSupplier);
    }

    @Override
    public <C, R> Stream<R> reduceRows(RowReducer<C, R> reducer) {
        return delegate.reduceRows(reducer);
    }

    @Override
    public <K, V> Stream<V> reduceRows(BiConsumer<Map<K, V>, RowView> accumulator) {
        return delegate.reduceRows(accumulator);
    }

    @Override
    public <U> U reduceRows(U seed, BiFunction<U, RowView, U> accumulator) {
        return delegate.reduceRows(seed, accumulator);
    }

    @Override
    public <U> U reduceResultSet(U seed, ResultSetAccumulator<U> accumulator) {
        return delegate.reduceResultSet(seed, accumulator);
    }

    @Override
    public <A, R> R collectRows(Collector<RowView, A, R> collector) {
        return delegate.collectRows(collector);
    }

    @Override
    public <R> R collectInto(Class<R> containerType) {
        return delegate.collectInto(containerType);
    }

    @Override
    public <R> R collectInto(GenericType<R> containerType) {
        return delegate.collectInto(containerType);
    }

    @Override
    public Object collectInto(Type containerType) {
        return delegate.collectInto(containerType);
    }

    /**
     * Returns the mod counts for the executed {@link org.jdbi.v3.core.statement.PreparedBatch}
     * Note that some database drivers might return special values like {@link Statement#SUCCESS_NO_INFO}
     * or {@link Statement#EXECUTE_FAILED}.
     * <br>
     * <b>Note that the result is only available after the statement was executed (eg. by calling map())</b>. Calling this method before execution
     * will return an empty array.
     *
     * @return the number of modified rows per batch part for the executed {@link org.jdbi.v3.core.statement.PreparedBatch}.
     */
    public int[] modifiedRowCounts() {
        return modifiedRowCountsSupplier.get();
    }
}
