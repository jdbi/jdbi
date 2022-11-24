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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.collector.ElementTypeNotFoundException;
import org.jdbi.v3.core.collector.NoSuchCollectorException;
import org.jdbi.v3.core.config.Configurable;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.GenericMapMapperFactory;
import org.jdbi.v3.core.mapper.MapMapper;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowViewMapper;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.result.internal.RowViewImpl;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Provides access to the contents of a {@link ResultSet} by mapping to Java types.
 */
public interface ResultBearing {
    /**
     * Returns a ResultBearing backed by the given result set supplier and context.
     *
     * @param resultSetSupplier result set supplier
     * @param ctx the statement context
     * @return a ResultBearing
     */
    static ResultBearing of(Supplier<ResultSet> resultSetSupplier, StatementContext ctx) {
        return new ResultBearing() {
            @Override
            public <R> R scanResultSet(ResultSetScanner<R> resultSetScanner) {
                try {
                    return resultSetScanner.scanResultSet(resultSetSupplier, ctx);
                } catch (SQLException e) {
                    throw new ResultSetException("Error reading result set", e, ctx);
                }
            }
        };
    }

    /**
     * Invokes the mapper with a result set supplier, and returns the value returned by the mapper.
     * @param resultSetScanner result set scanner
     * @param <R> result type returned by the mapper.
     * @return the value returned by the mapper.
     */
    <R> R scanResultSet(ResultSetScanner<R> resultSetScanner);

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @param <T>  the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.v3.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.v3.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <T> ResultIterable<T> mapTo(Class<T> type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @param <T>  the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.v3.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.v3.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <T> ResultIterable<T> mapTo(GenericType<T> type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.v3.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.v3.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default ResultIterable<?> mapTo(Type type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given qualified element type.
     *
     * @param type the qualified type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.v3.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.v3.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <T> ResultIterable<T> mapTo(QualifiedType<T> type) {
        return scanResultSet((resultSetSupplier, ctx) -> {
            RowMapper<T> rowMapper = ctx.findMapperFor(type)
                    .orElseThrow(() -> new NoSuchMapperException("No mapper registered for type " + type));
            return ResultIterable.of(resultSetSupplier, rowMapper, ctx);
        });
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type, using {@link BeanMapper}.
     *
     * @param type the bean type to map the result set rows to
     * @param <T>  the bean type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     */
    default <T> ResultIterable<T> mapToBean(Class<T> type) {
        return map(BeanMapper.of(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of {@code Map<String,Object>}. Keys are column names, and
     * values are column values.
     *
     * @return a {@link ResultIterable ResultIterable&lt;Map&lt;String,Object&gt;&gt;}.
     */
    default ResultIterable<Map<String, Object>> mapToMap() {
        return map(new MapMapper());
    }

    /**
     * Maps this result set to a {@link Map} of {@link String} and the given value class.
     *
     * @param <T> the value type
     * @param valueType the class to map the resultset columns to
     * @return a {@link Map} of String and the given type.
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <T> ResultIterable<Map<String, T>> mapToMap(Class<T> valueType) {
        return scanResultSet((resultSetSupplier, ctx) ->
                ResultIterable.of(resultSetSupplier, GenericMapMapperFactory.getMapperForValueType(valueType, ctx.getConfig()), ctx));
    }

    /**
     * Maps this result set to a {@link Map} of {@link String} and the given value type.
     *
     * @param <T> the value type
     * @param valueType the type to map the resultset columns to
     * @return a {@link Map} of String and the given type.
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <T> ResultIterable<Map<String, T>> mapToMap(GenericType<T> valueType) {
        return scanResultSet((resultSetSupplier, ctx) ->
                ResultIterable.of(resultSetSupplier, GenericMapMapperFactory.getMapperForValueType(valueType, ctx.getConfig()), ctx));
    }

    /**
     * Maps this result set to a {@link ResultIterable}, using the given column mapper.
     *
     * @param mapper column mapper used to map the first column of each row
     * @param <T>    the type to map the result set rows to
     * @return a {@link ResultIterable} of type {@code <T>}.
     */
    default <T> ResultIterable<T> map(ColumnMapper<T> mapper) {
        return map(new SingleColumnMapper<>(mapper));
    }

    /**
     * Maps this result set to a {@link ResultIterable}, using the given row mapper.
     *
     * @param mapper mapper used to map each row
     * @param <T>    the type to map the result set rows to
     * @return a {@link ResultIterable} of type {@code <T>}.
     */
    default <T> ResultIterable<T> map(RowMapper<T> mapper) {
        return scanResultSet((resultSetSupplier, ctx) -> ResultIterable.of(resultSetSupplier, mapper, ctx));
    }

    /**
     * Maps this result set to a {@link ResultIterable}, using the given {@link RowViewMapper}.
     * This overload only exists to allow RowViewMapper as the type of a lambda expression.
     *
     * @param mapper RowViewMapper used to map each row
     * @param <T>    the type to map the result set rows to
     * @return a {@link ResultIterable} of type {@code <T>}.
     */
    default <T> ResultIterable<T> map(RowViewMapper<T> mapper) {
        return map((RowMapper<T>) mapper);
    }

    /**
     * Reduce the result rows using the given row reducer.
     *
     * @param rowReducer the row reducer.
     * @param <C> Mutable result container type
     * @param <R> Result element type
     * @return the stream of result elements
     * @see RowReducer
     */
    default <C, R> Stream<R> reduceRows(RowReducer<C, R> rowReducer) {
        return scanResultSet((resultSetSupplier, context) -> {
            try (StatementContext ctx = context) {
                ResultSet resultSet = resultSetSupplier.get();
                RowView rowView = new RowViewImpl(resultSet, ctx);

                C container = rowReducer.container();
                while (resultSet.next()) {
                    rowReducer.accumulate(container, rowView);
                }
                return rowReducer.stream(container).onClose(ctx::close);
            } catch (SQLException e) {
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
    default <K, V> Stream<V> reduceRows(BiConsumer<Map<K, V>, RowView> accumulator) {
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
    default <U> U reduceRows(U seed, BiFunction<U, RowView, U> accumulator) {
        return scanResultSet((resultSetSupplier, context) -> {
            try (StatementContext ctx = context) {
                ResultSet resultSet = resultSetSupplier.get();
                RowView rowView = new RowViewImpl(resultSet, ctx);

                U result = seed;
                while (resultSet.next()) {
                    result = accumulator.apply(result, rowView);
                }
                return result;
            } catch (SQLException e) {
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
    default <U> U reduceResultSet(U seed, ResultSetAccumulator<U> accumulator) {
        return scanResultSet((resultSetSupplier, context) -> {
            try (StatementContext ctx = context) {
                ResultSet resultSet = resultSetSupplier.get();

                U result = seed;
                while (resultSet.next()) {
                    result = accumulator.apply(result, resultSet, ctx);
                }
                return result;
            } catch (SQLException e) {
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
    default <A, R> R collectRows(Collector<RowView, A, R> collector) {
        return scanResultSet((resultSetSupplier, context) -> {
            try (StatementContext ctx = context) {
                ResultSet resultSet = resultSetSupplier.get();
                RowView rowView = new RowViewImpl(resultSet, ctx);

                A accumulator = collector.supplier().get();

                BiConsumer<A, RowView> consumer = collector.accumulator();
                while (resultSet.next()) {
                    consumer.accept(accumulator, rowView);
                }

                return collector.finisher().apply(accumulator);
            } catch (SQLException e) {
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
    default <R> R collectInto(Class<R> containerType) {
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
    default <R> R collectInto(GenericType<R> containerType) {
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
    default Object collectInto(Type containerType) {
        return scanResultSet((resultSetSupplier, ctx) -> {
            Collector collector = ctx.findCollectorFor(containerType)
                    .orElseThrow(() -> new NoSuchCollectorException("No collector registered for container type " + containerType));
            Type elementType = ctx.findElementTypeFor(containerType)
                    .orElseThrow(() -> new ElementTypeNotFoundException("Unknown element type for container type " + containerType));
            RowMapper<?> rowMapper = ctx.findMapperFor(elementType)
                    .orElseThrow(() -> new NoSuchMapperException("No mapper registered for element type " + elementType));

            return ResultIterable.of(resultSetSupplier, rowMapper, ctx).collect(collector);
        });
    }
}
