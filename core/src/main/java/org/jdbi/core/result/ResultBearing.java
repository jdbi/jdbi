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
import java.util.function.Supplier;

import org.jdbi.core.config.Configurable;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.GenericMapMapperFactory;
import org.jdbi.core.mapper.MapMapper;
import org.jdbi.core.mapper.NoSuchMapperException;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.RowViewMapper;
import org.jdbi.core.mapper.SingleColumnMapper;
import org.jdbi.core.mapper.reflect.BeanMapper;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.statement.StatementContext;

/**
 * Provides access to the contents of a {@link ResultSet} by mapping to Java types.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ResultBearing extends ResultScannable {
    /**
     * Returns a ResultBearing backed by the given result set supplier and context.
     *
     * @param resultSetSupplier result set supplier
     * @param ctx the statement context
     * @return a ResultBearing
     */
    static ResultBearing of(final Supplier<ResultSet> resultSetSupplier, final StatementContext ctx) {
        return new ResultBearing() {
            @Override
            public <R> R scanResultSet(final ResultSetScanner<R> resultSetScanner) {
                try {
                    return resultSetScanner.scanResultSet(resultSetSupplier, ctx);
                } catch (final SQLException e) {
                    throw new ResultSetException("Error reading result set", e, ctx);
                }
            }
        };
    }


    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @param <T>  the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <T> ResultIterable<T> mapTo(final Class<T> type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @param <T>  the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <T> ResultIterable<T> mapTo(final GenericType<T> type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default ResultIterable<?> mapTo(final Type type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given qualified element type.
     *
     * @param type the qualified type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <T> ResultIterable<T> mapTo(final QualifiedType<T> type) {
        return scanResultSet((resultSetSupplier, ctx) -> {
            final RowMapper<T> rowMapper = ctx.findMapperFor(type)
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
    default <T> ResultIterable<T> mapToBean(final Class<T> type) {
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
    default <T> ResultIterable<Map<String, T>> mapToMap(final Class<T> valueType) {
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
    default <T> ResultIterable<Map<String, T>> mapToMap(final GenericType<T> valueType) {
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
    default <T> ResultIterable<T> map(final ColumnMapper<T> mapper) {
        return map(new SingleColumnMapper<>(mapper));
    }

    /**
     * Maps this result set to a {@link ResultIterable}, using the given row mapper.
     *
     * @param mapper mapper used to map each row
     * @param <T>    the type to map the result set rows to
     * @return a {@link ResultIterable} of type {@code <T>}.
     */
    default <T> ResultIterable<T> map(final RowMapper<T> mapper) {
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
    default <T> ResultIterable<T> map(final RowViewMapper<T> mapper) {
        return map((RowMapper<T>) mapper);
    }
}
