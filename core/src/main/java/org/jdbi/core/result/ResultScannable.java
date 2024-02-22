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
package org.jdbi.core.result;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.core.collector.ElementTypeNotFoundException;
import org.jdbi.core.collector.NoSuchCollectorException;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.NoSuchMapperException;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.result.internal.RowViewImpl;
import org.jdbi.core.statement.StatementContext;

public interface ResultScannable {

    /**
     * Invokes the mapper with a result set supplier, and returns the value returned by the mapper.
     * @param resultSetScanner result set scanner
     * @param <R> result type returned by the mapper.
     * @return the value returned by the mapper.
     */
    <R> R scanResultSet(ResultSetScanner<R> resultSetScanner);

    /**
     * Reduce the result rows using the given row reducer.
     *
     * @param rowReducer the row reducer.
     * @param <C> Mutable result container type
     * @param <R> Result element type
     * @return the stream of result elements
     * @see RowReducer
     */
    default <C, R> Stream<R> reduceRows(final RowReducer<C, R> rowReducer) {
        return scanResultSet((resultSetSupplier, context) -> {
            try (StatementContext ctx = context) {
                final ResultSet resultSet = resultSetSupplier.get();
                final RowView rowView = new RowViewImpl(resultSet, ctx);

                final C container = rowReducer.container();
                while (resultSet.next()) {
                    rowReducer.accumulate(container, rowView);
                }
                return rowReducer.stream(container).onClose(ctx::close);
            } catch (final SQLException e) {
                throw new UnableToProduceResultException(e, context);
            }
        });
    }

    /**
     * Reduce the result rows using a {@link Map Map&lt;K, V&gt;} as the
     * result container.
     *
     * @param accumulator accumulator function which gathers data from each
     *                    {@link RowView} into the result map.
     * @param <K>         map key type
     * @param <V>         map value type
     * @return the stream of elements in the container's {@link Map#values()}
     *         collection, in the order they were inserted.
     */
    default <K, V> Stream<V> reduceRows(final BiConsumer<Map<K, V>, RowView> accumulator) {
        return reduceRows((LinkedHashMapRowReducer<K, V>) accumulator::accept);
    }

    /**
     * Reduce the results.  Using a {@code BiFunction<U, RowView, U>}, repeatedly
     * combine query results until only a single value remains.
     *
     * @param <U>         the type of the accumulator
     * @param seed        the {@code U} to combine with the first result
     * @param accumulator the function to apply repeatedly
     * @return the final {@code U}
     */
    default <U> U reduceRows(final U seed, final BiFunction<U, RowView, U> accumulator) {
        return scanResultSet((resultSetSupplier, context) -> {
            try (StatementContext ctx = context) {
                final ResultSet resultSet = resultSetSupplier.get();
                final RowView rowView = new RowViewImpl(resultSet, ctx);

                U result = seed;
                while (resultSet.next()) {
                    result = accumulator.apply(result, rowView);
                }
                return result;
            } catch (final SQLException e) {
                throw new UnableToProduceResultException(e, context);
            }
        });
    }

    /**
     * Reduce the results.  Using a {@code ResultSetAccumulator}, repeatedly
     * combine query results until only a single value remains.
     *
     * @param <U>         the accumulator type
     * @param seed        the {@code U} to combine with the first result
     * @param accumulator the function to apply repeatedly
     * @return the final {@code U}
     */
    default <U> U reduceResultSet(final U seed, final ResultSetAccumulator<U> accumulator) {
        return scanResultSet((resultSetSupplier, context) -> {
            try (StatementContext ctx = context) {
                final ResultSet resultSet = resultSetSupplier.get();

                U result = seed;
                while (resultSet.next()) {
                    result = accumulator.apply(result, resultSet, ctx);
                }
                return result;
            } catch (final SQLException e) {
                throw new UnableToProduceResultException(e, context);
            }
        });
    }

    /**
     * Collect the results using the given collector. Do not attempt to accumulate the
     * {@link RowView} objects into the result--they are only valid within the
     * {@link Collector#accumulator()} function. Instead, extract mapped types from the
     * RowView by calling {@code RowView.getRow()} or {@code RowView.getColumn()}.
     *
     * @param collector the collector to collect the result rows.
     * @param <A> the mutable accumulator type used by the collector.
     * @param <R> the result type returned by the collector.
     * @return the result of the collection
     */
    default <A, R> R collectRows(final Collector<RowView, A, R> collector) {
        return scanResultSet((resultSetSupplier, context) -> {
            try (StatementContext ctx = context) {
                final ResultSet resultSet = resultSetSupplier.get();
                final RowView rowView = new RowViewImpl(resultSet, ctx);

                final A accumulator = collector.supplier().get();

                final BiConsumer<A, RowView> consumer = collector.accumulator();
                while (resultSet.next()) {
                    consumer.accept(accumulator, rowView);
                }

                return collector.finisher().apply(accumulator);
            } catch (final SQLException e) {
                throw new UnableToProduceResultException(e, context);
            }
        });
    }

    /**
     * Collect the results into a container of the given type. A collector
     * must be registered for the container type, which knows the element type
     * for the container. A mapper must be registered for the element type.
     * <p>
     * This method is equivalent to {@code ResultBearing.mapTo(elementType).collect(containerCollector)}.
     * </p>
     * @param containerType the container type into which results will be collected
     * @param <R>           the result container type
     * @return a container into which result rows have been collected
     */
    @SuppressWarnings("unchecked")
    default <R> R collectInto(final Class<R> containerType) {
        return (R) collectInto((Type) containerType);
    }

    /**
     * Collect the results into a container of the given generic type. A collector
     * must be registered for the container type, which knows the element type
     * for the container. A mapper must be registered for the element type.
     * <p>
     * This method is equivalent to {@code ResultBearing.mapTo(elementType).collect(containerCollector)}.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * Map&lt;Long, User&gt; usersById = handle.createQuery("select * from user")
     *     .configure(MapEntryMappers.class, cfg -&gt; cfg.setKeyColumn("id"))
     *     .collectInto(new GenericType&lt;Map&lt;Long, User&gt;&gt;() {});
     * </pre>
     *
     * @param containerType the container type into which results will be collected
     * @param <R>           the result container type
     * @return a container into which result rows have been collected
     */
    @SuppressWarnings("unchecked")
    default <R> R collectInto(final GenericType<R> containerType) {
        return (R) collectInto(containerType.getType());
    }

    /**
     * Collect the results into a container of the given type. A collector
     * must be registered for the container type, which knows the element type
     * for the container. A mapper must be registered for the element type.
     * <p>
     * This method is equivalent to {@code ResultBearing.mapTo(elementType).collect(containerCollector)}.
     * </p>
     *
     * @param containerType the container type into which results will be collected
     * @return a container into which result rows have been collected
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    default Object collectInto(final Type containerType) {
        return scanResultSet((resultSetSupplier, ctx) -> {
            final Collector collector = ctx.findCollectorFor(containerType)
                    .orElseThrow(() -> new NoSuchCollectorException("No collector registered for container type " + containerType));
            final Type elementType = ctx.findElementTypeFor(containerType)
                    .orElseThrow(() -> new ElementTypeNotFoundException("Unknown element type for container type " + containerType));
            final RowMapper<?> rowMapper = ctx.findMapperFor(elementType)
                    .orElseThrow(() -> new NoSuchMapperException("No mapper registered for element type " + elementType));

            return ResultIterable.of(resultSetSupplier, rowMapper, ctx).collect(collector);
        });
    }
}
