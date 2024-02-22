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
package org.jdbi.core.statement;

import java.lang.reflect.Type;
import java.util.Map;

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
import org.jdbi.core.result.ResultIterable;

public interface QueryTemplateBuilder extends QueryCustomizerMixin<QueryTemplateBuilder> {
    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @param <R>  the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.v3.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.v3.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <R> QueryTemplate<R> mapTo(final Class<R> type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type.
     *
     * @param type the type to map the result set rows to
     * @param <R>  the type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     * @see Configurable#registerRowMapper(RowMapper)
     * @see Configurable#registerRowMapper(org.jdbi.v3.core.mapper.RowMapperFactory)
     * @see Configurable#registerColumnMapper(org.jdbi.v3.core.mapper.ColumnMapperFactory)
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <R> QueryTemplate<R> mapTo(final GenericType<R> type) {
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
    default QueryTemplate<?> mapTo(final Type type) {
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
    default <R> QueryTemplate<R> mapTo(final QualifiedType<R> type) {
        return new QueryTemplate<R>(this, (resultSetSupplier, ctx) -> {
            final RowMapper<R> rowMapper = ctx.findMapperFor(type)
                    .orElseThrow(() -> new NoSuchMapperException("No mapper registered for type " + type));
            return ResultIterable.of(resultSetSupplier, rowMapper, ctx);
        });
    }

    /**
     * Maps this result set to a {@link ResultIterable} of the given element type, using {@link BeanMapper}.
     *
     * @param type the bean type to map the result set rows to
     * @param <R>  the bean type to map the result set rows to
     * @return a {@link ResultIterable} of the given type.
     */
    default <R> QueryTemplate<R> mapToBean(final Class<R> type) {
        return map(BeanMapper.of(type));
    }

    /**
     * Maps this result set to a {@link ResultIterable} of {@code Map<String,Object>}. Keys are column names, and
     * values are column values.
     *
     * @return a {@link ResultIterable ResultIterable&lt;Map&lt;String,Object&gt;&gt;}.
     */
    default QueryTemplate<Map<String, Object>> mapToMap() {
        return map(new MapMapper());
    }

    /**
     * Maps this result set to a {@link Map} of {@link String} and the given value class.
     *
     * @param <R> the value type
     * @param valueType the class to map the resultset columns to
     * @return a {@link Map} of String and the given type.
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <R> QueryTemplate<Map<String, R>> mapToMap(final Class<R> valueType) {
        return new QueryTemplate<>(this, (resultSetSupplier, ctx) ->
                ResultIterable.of(resultSetSupplier, GenericMapMapperFactory.getMapperForValueType(valueType, ctx.getConfig()), ctx));
    }

    /**
     * Maps this result set to a {@link Map} of {@link String} and the given value type.
     *
     * @param <R> the value type
     * @param valueType the type to map the resultset columns to
     * @return a {@link Map} of String and the given type.
     * @see Configurable#registerColumnMapper(ColumnMapper)
     */
    default <R> QueryTemplate<Map<String, R>> mapToMap(final GenericType<R> valueType) {
        return new QueryTemplate<>(this, (resultSetSupplier, ctx) ->
                ResultIterable.of(resultSetSupplier, GenericMapMapperFactory.getMapperForValueType(valueType, ctx.getConfig()), ctx));
    }

    /**
     * Maps this result set to a {@link ResultIterable}, using the given column mapper.
     *
     * @param mapper column mapper used to map the first column of each row
     * @param <R>    the type to map the result set rows to
     * @return a {@link ResultIterable} of type {@code <R>}.
     */
    default <R> QueryTemplate<R> map(final ColumnMapper<R> mapper) {
        return map(new SingleColumnMapper<>(mapper));
    }

    /**
     * Maps this result set to a {@link ResultIterable}, using the given row mapper.
     *
     * @param mapper mapper used to map each row
     * @param <R>    the type to map the result set rows to
     * @return a {@link ResultIterable} of type {@code <R>}.
     */
    default <R> QueryTemplate<R> map(final RowMapper<R> mapper) {
        return new QueryTemplate<>(this, (resultSetSupplier, ctx) ->
                ResultIterable.of(resultSetSupplier, mapper, ctx));
    }

    /**
     * Maps this result set to a {@link ResultIterable}, using the given {@link RowViewMapper}.
     * This overload only exists to allow RowViewMapper as the type of a lambda expression.
     *
     * @param mapper RowViewMapper used to map each row
     * @param <R>    the type to map the result set rows to
     * @return a {@link ResultIterable} of type {@code <R>}.
     */
    default <R> QueryTemplate<R> map(final RowViewMapper<R> mapper) {
        return map((RowMapper<R>) mapper);
    }

    String getSql();
}
