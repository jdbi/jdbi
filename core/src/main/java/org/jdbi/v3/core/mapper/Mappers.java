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
package org.jdbi.v3.core.mapper;

import static org.jdbi.v3.core.internal.JdbiOptionals.findFirstPresent;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.generic.GenericType;

/**
 * Configuration class for obtaining row or column mappers.
 * <p>
 * This configuration is merely a convenience class, and does not have any
 * configuration of its own. All methods delegate to {@link RowMappers} or
 * {@link ColumnMappers}.
 */
public class Mappers implements JdbiConfig<Mappers> {
    private RowMappers rowMappers;
    private ColumnMappers columnMappers;

    public Mappers() {
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.rowMappers = registry.get(RowMappers.class);
        this.columnMappers = registry.get(ColumnMappers.class);
    }

    /**
     * Obtain a mapper for the given type. If a row mapper is registered for the
     * given type, it is returned. If a column mapper is registered for the
     * given type, it is adapted into a row mapper, mapping the first column of
     * the result set. If neither a row or column mapper is registered, empty is
     * returned.
     *
     * @param <T>  the type of the mapper to find
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mapper
     * is registered for the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<RowMapper<T>> findFor(Class<T> type) {
        RowMapper<T> mapper = (RowMapper<T>) findFor((Type) type).orElse(null);
        return Optional.ofNullable(mapper);
    }

    /**
     * Obtain a mapper for the given type. If a row mapper is registered for the
     * given type, it is returned. If a column mapper is registered for the
     * given type, it is adapted into a row mapper, mapping the first column of
     * the result set. If neither a row or column mapper is registered, empty is
     * returned.
     *
     * @param <T>  the type of the mapper to find
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mapper
     * is registered for the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<RowMapper<T>> findFor(GenericType<T> type) {
        RowMapper<T> mapper = (RowMapper<T>) findFor(type.getType()).orElse(null);
        return Optional.ofNullable(mapper);
    }

    /**
     * Obtain a mapper for the given type. If a row mapper is registered for the
     * given type, it is returned. If a column mapper is registered for the
     * given type, it is adapted into a row mapper, mapping the first column of
     * the result set. If neither a row or column mapper is registered, empty is
     * returned.
     *
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mapper
     * is registered for the given type.
     */
    public Optional<RowMapper<?>> findFor(Type type) {
        return findFirstPresent(
                () -> rowMappers.findFor(type),
                () -> columnMappers.findFor(type).map(SingleColumnMapper::new));
    }

    @Override
    public Mappers createCopy() {
        return new Mappers();
    }
}
