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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Yo dawg, I heard you like maps, so I made you a mapper that maps rows into {@code Map<String,Object>}. Map
 * keys are column names, while map values are the values in those columns. Map keys are converted to lowercase by
 * default.
 */
public class MapMapper implements RowMapper<Map<String, Object>>
{
    private final boolean foldCase;

    /**
     * Constructs a new MapMapper, with map keys converted to lowercase.
     */
    public MapMapper() {
        this(true);
    }

    /**
     * Constructs a new MapMapper
     * @param foldCase if true, column names are converted to lowercase in the mapped {@link Map}.
     */
    public MapMapper(boolean foldCase) {
        this.foldCase = foldCase;
    }

    @Override
    public Map<String, Object> map(ResultSet rs, StatementContext ctx) throws SQLException {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<Map<String, Object>> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        ResultSetMetaData m = rs.getMetaData();
        int columnCount = m.getColumnCount();
        String[] columnNames = new String[columnCount+1];

        for (int i = 1; i <= columnCount; i++) {
            String key = m.getColumnName(i);
            String alias = m.getColumnLabel(i);

            if (alias == null) {
                alias = key;
            }
            if (foldCase) {
                alias = alias.toLowerCase(Locale.ROOT);
            }

            columnNames[i] = alias;
        }

        return (r, c) -> {
            Map<String, Object> row = new LinkedHashMap<>(columnCount);

            for (int i = 1; i <= columnCount; i++) {
                row.put(columnNames[i], rs.getObject(i));
            }

            return row;
        };
    }
}
