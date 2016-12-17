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

import static java.util.Collections.synchronizedMap;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.array.SqlArrayType;
import org.jdbi.v3.core.array.SqlArrayTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.util.GenericType;

/**
 * A registry of {@link JdbiConfig} instances by type.
 *
 * @see Configurable
 */
public class ConfigRegistry {
    private final Map<Class<? extends JdbiConfig>, JdbiConfig<?>> cache = synchronizedMap(new WeakHashMap<>());

    /**
     * Creates a new config registry.
     */
    public ConfigRegistry() {
    }

    private ConfigRegistry(ConfigRegistry that) {
        that.cache.forEach((type, config) -> cache.put(type, config.createCopy()));
    }

    /**
     * Returns this registry's instance of the given config class. Creates an instance on-demand if this registry does
     * not have one of the given type yet.
     *
     * @param configClass the config class type.
     * @param <C>         the config class type.
     * @return the given config class instance that belongs to this registry.
     */
    public <C extends JdbiConfig<C>> C get(Class<C> configClass) {
        return configClass.cast(cache.computeIfAbsent(configClass, type -> {
            try {
                return configClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to instantiate config class " + configClass +
                        ". Is there a public no-arg constructor?", e);
            }
        }));
    }

    /**
     * Returns a copy of this config registry.
     *
     * @return a copy of this config registry
     * @see JdbiConfig#createCopy() config objects in the returned registry are copies of the corresponding
     * config objects from this registry.
     */
    public ConfigRegistry createCopy() {
        return new ConfigRegistry(this);
    }

    /**
     * Obtain an argument for given value
     *
     * @param type  the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    public Optional<Argument> findArgumentFor(Type type, Object value) {
        return get(Arguments.class).findFor(type, value, this);
    }

    /**
     * Obtain an {@link SqlArrayType} for the given array element type
     *
     * @param elementType the array element type.
     * @return an {@link SqlArrayType} for the given element type.
     */
    public Optional<SqlArrayType<?>> findSqlArrayTypeFor(Type elementType) {
        return get(SqlArrayTypes.class).findFor(elementType, this);
    }

    /**
     * Obtain a column mapper for the given type
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public <T> Optional<ColumnMapper<T>> findColumnMapperFor(Class<T> type) {
        return get(ColumnMappers.class).findFor(type, this);
    }

    /**
     * Obtain a column mapper for the given type
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public <T> Optional<ColumnMapper<T>> findColumnMapperFor(GenericType<T> type) {
        return get(ColumnMappers.class).findFor(type, this);
    }

    /**
     * Obtain a column mapper for the given type
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public Optional<ColumnMapper<?>> findColumnMapperFor(Type type) {
        return get(ColumnMappers.class).findFor(type, this);
    }

    /**
     * Obtain a row mapper for the given type
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public Optional<RowMapper<?>> findRowMapperFor(Type type) {
        return get(RowMappers.class).findFor(type, this);
    }

    /**
     * Obtain a row mapper for the given type
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public <T> Optional<RowMapper<T>> findRowMapperFor(Class<T> type) {
        return get(RowMappers.class).findFor(type, this);
    }

    /**
     * Obtain a row mapper for the given type
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public <T> Optional<RowMapper<T>> findRowMapperFor(GenericType<T> type) {
        return get(RowMappers.class).findFor(type, this);
    }

}
