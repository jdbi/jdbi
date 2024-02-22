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
import java.util.Optional;
import java.util.stream.Collector;

import org.jdbi.core.argument.Argument;
import org.jdbi.core.argument.Arguments;
import org.jdbi.core.array.SqlArrayArgumentStrategy;
import org.jdbi.core.array.SqlArrayType;
import org.jdbi.core.array.SqlArrayTypes;
import org.jdbi.core.collector.JdbiCollectors;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.mapper.Mappers;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.RowMappers;
import org.jdbi.core.qualifier.QualifiedType;

public interface ConfigReader {
    /**
     * Gets the configuration object of the given type, associated with this context.
     *
     * @param configClass the configuration type
     * @param <C>         the configuration type
     * @return the configuration object of the given type, associated with this context.
     */
    default <C extends JdbiConfig<C>> C getConfig(final Class<C> configClass) {
        return getConfig().get(configClass);
    }

    /**
     * Returns the {@code ConfigRegistry}.
     *
     * @return the {@code ConfigRegistry} used by this context.
     */
    ConfigRegistry getConfig();

    /**
     * Obtain an argument for given value in this context
     *
     * @param type  the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    default Optional<Argument> findArgumentFor(final Type type, final Object value) {
        return getConfig(Arguments.class).findFor(type, value);
    }

    /**
     * Obtain an argument for given value in this context
     *
     * @param type  the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    default Optional<Argument> findArgumentFor(final QualifiedType<?> type, final Object value) {
        return getConfig(Arguments.class).findFor(type, value);
    }

    /**
     * Returns the strategy used by this context to bind array-type arguments to SQL statements.
     *
     * @return the strategy used to bind array-type arguments to SQL statements
     */
    default SqlArrayArgumentStrategy getSqlArrayArgumentStrategy() {
        return getConfig(SqlArrayTypes.class).getArgumentStrategy();
    }

    /**
     * Obtain an {@link SqlArrayType} for the given array element type in this context
     *
     * @param elementType the array element type.
     * @return an {@link SqlArrayType} for the given element type.
     */
    default Optional<SqlArrayType<?>> findSqlArrayTypeFor(final Type elementType) {
        return getConfig(SqlArrayTypes.class).findFor(elementType);
    }

    /**
     * Obtain a mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mappers
     * is registered for the given type.
     */
    default <T> Optional<RowMapper<T>> findMapperFor(final Class<T> type) {
        return getConfig(Mappers.class).findFor(type);
    }

    /**
     * Obtain a mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mappers
     * is registered for the given type.
     */
    default <T> Optional<RowMapper<T>> findMapperFor(final GenericType<T> type) {
        return getConfig(Mappers.class).findFor(type);
    }

    /**
     * Obtain a mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mappers
     * is registered for the given type.
     */
    default Optional<RowMapper<?>> findMapperFor(final Type type) {
        return getConfig(Mappers.class).findFor(type);
    }

    /**
     * Obtain a mapper for the given qualified type in this context.
     *
     * @param type the target qualified type to map to
     * @return a mapper for the given qualified type, or empty if no row or column mappers
     * is registered for the given type.
     */
    default <T> Optional<RowMapper<T>> findMapperFor(final QualifiedType<T> type) {
        return getConfig(Mappers.class).findFor(type);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    default <T> Optional<ColumnMapper<T>> findColumnMapperFor(final Class<T> type) {
        return getConfig(ColumnMappers.class).findFor(type);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    default <T> Optional<ColumnMapper<T>> findColumnMapperFor(final GenericType<T> type) {
        return getConfig(ColumnMappers.class).findFor(type);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    default Optional<ColumnMapper<?>> findColumnMapperFor(final Type type) {
        return getConfig(ColumnMappers.class).findFor(type);
    }

    /**
     * Obtain a column mapper for the given qualified type in this context.
     *
     * @param type the qualified target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    default <T> Optional<ColumnMapper<T>> findColumnMapperFor(final QualifiedType<T> type) {
        return getConfig(ColumnMappers.class).findFor(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    default Optional<RowMapper<?>> findRowMapperFor(final Type type) {
        return getConfig(RowMappers.class).findFor(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    default <T> Optional<RowMapper<T>> findRowMapperFor(final Class<T> type) {
        return getConfig(RowMappers.class).findFor(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    default <T> Optional<RowMapper<T>> findRowMapperFor(final GenericType<T> type) {
        return getConfig(RowMappers.class).findFor(type);
    }

    /**
     * Obtain a collector for the given type.
     *
     * @param containerType the container type.
     * @return a Collector for the given container type, or empty null if no collector is registered for the given type.
     */
    default Optional<Collector<?, ?, ?>> findCollectorFor(final Type containerType) {
        return getConfig(JdbiCollectors.class).findFor(containerType);
    }

    /**
     * Returns the element type for the given container type.
     *
     * @param containerType the container type.
     * @return the element type for the given container type, if available.
     */
    default Optional<Type> findElementTypeFor(final Type containerType) {
        return getConfig(JdbiCollectors.class).findElementTypeFor(containerType);
    }

    /**
     * Returns the attributes applied in this context.
     *
     * @return the defined attributes.
     */
    default Map<String, Object> getAttributes() {
        return getConfig(SqlStatements.class).getAttributes();
    }

    /**
     * Obtain the value of an attribute
     *
     * @param key the name of the attribute
     * @return the value of the attribute
     */
    default Object getAttribute(final String key) {
        return getConfig(SqlStatements.class).getAttribute(key);
    }
}
