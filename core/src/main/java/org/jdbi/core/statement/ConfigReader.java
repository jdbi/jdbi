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
import org.jdbi.core.argument.ArgumentResolver;
import org.jdbi.core.array.ArrayTypeResolver;
import org.jdbi.core.array.SqlArrayArgumentStrategy;
import org.jdbi.core.array.SqlArrayType;
import org.jdbi.core.array.SqlArrayTypes;
import org.jdbi.core.collector.CollectorResolver;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.ConfigView;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.MapperResolver;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.qualifier.QualifiedType;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
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
     * Returns a read-only view of the configuration used by this context. It is a {@link ConfigView}, not a
     * {@link ConfigRegistry}: read-only contexts (a {@code Jdbi} or a {@code Handle}) do not expose configuration
     * mutation. Mutation lives on {@code ConfigRegistry}, which is available on {@link org.jdbi.core.config.Configurable
     * Configurable} contexts (statements, the {@code Jdbi.Builder}) whose {@code getConfig()} covariantly returns it.
     *
     * @return the read-only configuration view used by this context.
     */
    ConfigView getConfig();

    /**
     * Obtain an argument for given value in this context
     *
     * @param type  the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    default Optional<Argument> findArgumentFor(final Type type, final Object value) {
        return ArgumentResolver.forRegistry(getConfig()).findFor(type, value);
    }

    /**
     * Obtain an argument for given value in this context
     *
     * @param type  the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    default Optional<Argument> findArgumentFor(final QualifiedType<?> type, final Object value) {
        return ArgumentResolver.forRegistry(getConfig()).findFor(type, value);
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
        return ArrayTypeResolver.forRegistry(getConfig()).findFor(elementType);
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
        return MapperResolver.forRegistry(getConfig()).findMapper(type);
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
        return MapperResolver.forRegistry(getConfig()).findMapper(type);
    }

    /**
     * Obtain a mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mappers
     * is registered for the given type.
     */
    default Optional<RowMapper<?>> findMapperFor(final Type type) {
        return MapperResolver.forRegistry(getConfig()).findMapper(type);
    }

    /**
     * Obtain a mapper for the given qualified type in this context.
     *
     * @param type the target qualified type to map to
     * @return a mapper for the given qualified type, or empty if no row or column mappers
     * is registered for the given type.
     */
    default <T> Optional<RowMapper<T>> findMapperFor(final QualifiedType<T> type) {
        return MapperResolver.forRegistry(getConfig()).findMapper(type);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    default <T> Optional<ColumnMapper<T>> findColumnMapperFor(final Class<T> type) {
        return MapperResolver.forRegistry(getConfig()).findColumnMapper(type);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    default <T> Optional<ColumnMapper<T>> findColumnMapperFor(final GenericType<T> type) {
        return MapperResolver.forRegistry(getConfig()).findColumnMapper(type);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    default Optional<ColumnMapper<?>> findColumnMapperFor(final Type type) {
        return MapperResolver.forRegistry(getConfig()).findColumnMapper(type);
    }

    /**
     * Obtain a column mapper for the given qualified type in this context.
     *
     * @param type the qualified target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    default <T> Optional<ColumnMapper<T>> findColumnMapperFor(final QualifiedType<T> type) {
        return MapperResolver.forRegistry(getConfig()).findColumnMapper(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    default Optional<RowMapper<?>> findRowMapperFor(final Type type) {
        return MapperResolver.forRegistry(getConfig()).findRowMapper(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    default <T> Optional<RowMapper<T>> findRowMapperFor(final Class<T> type) {
        return MapperResolver.forRegistry(getConfig()).findRowMapper(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    default <T> Optional<RowMapper<T>> findRowMapperFor(final GenericType<T> type) {
        return MapperResolver.forRegistry(getConfig()).findRowMapper(type);
    }

    /**
     * Obtain a collector for the given type.
     *
     * @param containerType the container type.
     * @return a Collector for the given container type, or empty null if no collector is registered for the given type.
     */
    default Optional<Collector<?, ?, ?>> findCollectorFor(final Type containerType) {
        return CollectorResolver.forRegistry(getConfig()).findFor(containerType);
    }

    /**
     * Returns the element type for the given container type.
     *
     * @param containerType the container type.
     * @return the element type for the given container type, if available.
     */
    default Optional<Type> findElementTypeFor(final Type containerType) {
        return CollectorResolver.forRegistry(getConfig()).findElementTypeFor(containerType);
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
