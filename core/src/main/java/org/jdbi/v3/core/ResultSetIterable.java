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
package org.jdbi.v3.core;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jdbi.v3.core.exception.ResultSetException;
import org.jdbi.v3.core.exception.UnableToProduceResultException;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.MapMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.util.GenericType;

/**
 * Provides access to the contents of a {@link ResultSet}
 */
public interface ResultSetIterable {
    /**
     * Returns a ResultSetIterable backed by the given result set supplier and context.
     *
     * @param resultSetSupplier result set supplier
     * @param ctx the statement context
     * @return a ResultSetIterable
     */
    static ResultSetIterable of(Supplier<ResultSet> resultSetSupplier, StatementContext ctx) {
        return new ResultSetIterable() {
            @Override
            public <R> R withResultSet(ResultSetCallback<R> callback) {
                try {
                    return callback.withResultSet(resultSetSupplier, ctx);
                }
                catch (SQLException e) {
                    throw new ResultSetException("Error reading result set", e, ctx);
                }
            }
        };
    }

    /**
     * Invokes the callback with the result set, and returns the value returned by the callback.
     * @param callback the callback
     * @param <R> result type returned by the callback.
     * @return the value returned by the callback.
     */
    <R> R withResultSet(ResultSetCallback<R> callback);

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @param <T>  the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(RowMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    @SuppressWarnings("unchecked")
    default <T> ResultIterable<T> mapTo(Class<T> type) {
        return (ResultIterable<T>) mapTo((Type) type);
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @param <T>  the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(RowMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    @SuppressWarnings("unchecked")
    default <T> ResultIterable<T> mapTo(GenericType<T> type) {
        return (ResultIterable<T>) mapTo(type.getType());
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(RowMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default ResultIterable<?> mapTo(Type type) {
        return withResultSet((supplier, ctx) -> {
            RowMapper<?> mapper = ctx.findRowMapperFor(type)
                    .orElseThrow(() -> new UnsupportedOperationException("No mapper registered for type " + type));
            return ResultIterable.of(supplier, mapper, ctx);
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
        return map(new BeanMapper<>(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of {@code Map&lt;String,Object&gt;}. Keys are column names, and
     * values are column values.
     *
     * @return a {@link ResultIterable ResultSetIterable&lt;Map&lt;String,Object&gt;&gt;}.
     */
    default ResultIterable<Map<String, Object>> mapToMap() {
        return map(new MapMapper());
    }

    /**
     * Maps this result set to a {@link ResultIterable}, using the given column mapper.
     *
     * @param mapper column mapper used to map the first column of each row
     * @param <T>    the type to map the result set rows to
     * @return a {@link ResultIterable} of type {@code &lt;T&gt;}.
     */
    default <T> ResultIterable<T> map(ColumnMapper<T> mapper) {
        return map(new SingleColumnMapper<>(mapper));
    }

    /**
     * Maps this result set to a {@link ResultIterable}, using the given row mapper.
     *
     * @param mapper mapper used to map each row
     * @param <T>    the type to map the result set rows to
     * @return a {@link ResultIterable} of type {@code &lt;T&gt;}.
     */
    default <T> ResultIterable<T> map(RowMapper<T> mapper) {
        return withResultSet((supplier, ctx) -> ResultIterable.of(supplier, mapper, ctx));
    }

    /**
     * Reduce the results.  Using a {@code BiFunction<U, RowView, U>}, repeatedly
     * combine query results until only a single value remains.
     *
     * @param seed        the {@code U} to combine with the first result
     * @param accumulator the function to apply repeatedly
     * @return the final {@code U}
     */
    default <U> U reduceRows(U seed, BiFunction<U, RowView, U> accumulator) {
        return withResultSet((supplier, ctx) -> {
            try (ResultSet rs = supplier.get()) {
                RowView rv = new RowView(rs, ctx);
                U result = seed;
                while (rs.next()) {
                    result = accumulator.apply(result, rv);
                }
                return result;
            }
            catch (SQLException e) {
                throw new UnableToProduceResultException(e, ctx);
            }
            finally {
                ctx.close();
            }
        });
    }

    /**
     * Reduce the results.  Using a {@code ResultSetAccumulator}, repeatedly
     * combine query results until only a single value remains.
     *
     * @param seed        the {@code U} to combine with the first result
     * @param accumulator the function to apply repeatedly
     * @return the final {@code U}
     */
    default <U> U reduceResultSet(U seed, ResultSetAccumulator<U> accumulator) {
        return withResultSet((supplier, ctx) -> {
            try (ResultSet rs = supplier.get()) {
                U result = seed;
                while (rs.next()) {
                    result = accumulator.apply(result, rs, ctx);
                }
                return result;
            }
            catch (SQLException e) {
                throw new UnableToProduceResultException(e, ctx);
            }
            finally {
                ctx.close();
            }
        });
    }
}
