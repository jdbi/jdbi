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
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.StatementContext;

/**
 * A {@link RowMapper} implementation to easily compose existing
 * RowMappers.  As the name implies, it is most commonly used
 * for retrieving multiple tables' Java representations from a
 * joined row.
 */
public class JoinRowMapper implements RowMapper<JoinRowMapper.JoinRow>
{
    /**
     * Holder for a single joined row.
     */
    public static class JoinRow
    {
        private final Map<Type, Object> entries;

        private JoinRow(Map<Type, Object> entries) {
            this.entries = entries;
        }

        /**
         * Return the value mapped for a given class.
         * @param klass the type that was mapped
         * @return the value for that type
         */
        public <T> T get(Class<T> klass) {
            return klass.cast(get((Type)klass));
        }

        /**
         * Return the value mapped for a given type.
         * @param type the type that was mapped
         * @return the value for that type
         */
        public Object get(Type type) {
            Object result = entries.get(type);
            if (result == null && !entries.containsKey(type)) {
                throw new IllegalArgumentException("no result stored for " + type);
            }
            return result;
        }
    }

    private final Type[] types;

    private JoinRowMapper(Type[] types)
    {
        this.types = types;
    }

    @Override
    public JoinRow map(ResultSet r, StatementContext ctx)
    throws SQLException
    {
        final Map<Type, Object> entries = new HashMap<>(types.length);
        for (Type type : types) {
            entries.put(type, ctx.findRowMapperFor(type)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No row mapper registered for " + type))
                    .map(r, ctx));
        }
        return new JoinRow(entries);
    }

    @Override
    public RowMapper<JoinRow> specialize(ResultSet r, StatementContext ctx) throws SQLException {
        RowMapper[] mappers = new RowMapper[types.length];
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            mappers[i] = ctx.findRowMapperFor(type)
                    .orElseThrow(() -> new IllegalArgumentException("No row mapper registered for " + type))
                    .specialize(r, ctx);
        }

        return (rs, context) -> {
            final Map<Type, Object> entries = new HashMap<>(types.length);
            for (int i = 0; i < types.length; i++) {
                Type type = types[i];
                RowMapper<?> mapper = mappers[i];
                entries.put(type, mapper.map(r, ctx));
            }
            return new JoinRow(entries);
        };
    }

    /**
     * Create a JoinRowMapper that maps each of the given types and returns a
     * {@link JoinRowMapper.JoinRow} with the resulting values.
     * @param classes the types to extract
     * @return a JoinRowMapper that extracts the given types
     */
    public static JoinRowMapper forTypes(Type... classes)
    {
        return new JoinRowMapper(classes);
    }
}
