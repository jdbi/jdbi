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

import org.jdbi.v3.core.statement.StatementContext;

/**
 * A {@link RowMapper} implementation to easily compose existing
 * RowMappers.  As the name implies, it is most commonly used
 * for retrieving multiple tables' Java representations from a
 * joined row.
 */
public class JoinRowMapper implements RowMapper<JoinRow>
{

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
        RowMapper<?>[] mappers = new RowMapper[types.length];
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
     * {@link JoinRow} with the resulting values.
     * @param classes the types to extract
     * @return a JoinRowMapper that extracts the given types
     */
    public static JoinRowMapper forTypes(Type... classes)
    {
        return new JoinRowMapper(classes);
    }
}
