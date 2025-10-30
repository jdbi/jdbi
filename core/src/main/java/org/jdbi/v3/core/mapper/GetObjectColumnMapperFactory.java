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

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Factory that uses {@link java.sql.ResultSet#getObject(int, Class)} to fetch values.
 */
public class GetObjectColumnMapperFactory implements ColumnMapperFactory {

    private final Set<Class<?>> supportedTypes;

    protected GetObjectColumnMapperFactory(Class<?>... types) {
        this(Arrays.asList(types));
    }

    protected GetObjectColumnMapperFactory(Collection<Class<?>> types) {
        this.supportedTypes = new HashSet<>(types);
    }

    /**
     * Creates a {@link ColumnMapperFactory} that accepts multiple types and maps them by calling {@link ResultSet#getObject(int, Class)}.
     *
     * @param types One or more types that should be mapped by this factory
     * @return A {@link ColumnMapperFactory}
     */
    public static ColumnMapperFactory forClasses(Class<?>... types) {
        return new GetObjectColumnMapperFactory(Arrays.asList(types));
    }

    /**
     * Creates a {@link ColumnMapperFactory} that accepts multiple types and maps them by calling {@link ResultSet#getObject(int, Class)}.
     *
     * @param types One or more types that should be mapped by this factory
     * @return A {@link ColumnMapperFactory}
     */
    public static ColumnMapperFactory forClasses(Set<Class<?>> types) {
        return new GetObjectColumnMapperFactory(types);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        return Optional.of(type)
            .filter(Class.class::isInstance)
            .map(Class.class::cast)
            .filter(supportedTypes::contains)
            .map(GetObjectColumnMapper::new);
    }

    private static class GetObjectColumnMapper<T> implements ColumnMapper<T> {

        private final Class<T> clazz;

        private GetObjectColumnMapper(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return r.getObject(columnNumber, clazz);
        }
    }
}
