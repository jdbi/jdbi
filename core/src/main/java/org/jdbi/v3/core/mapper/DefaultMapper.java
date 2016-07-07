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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.exception.ResultSetException;

public class DefaultMapper implements RowMapper<Map<String, Object>>
{
    @Override
    public Map<String, Object> map(ResultSet r, StatementContext ctx)
    {
        Map<String, Object> row = new HashMap<>();
        ResultSetMetaData m;
        try
        {
            m = r.getMetaData();
        }
        catch (SQLException e)
        {
            throw new ResultSetException("Unable to obtain metadata from result set", e, ctx);
        }

        try
        {
            for (int i = 1; i <= m.getColumnCount(); i ++)
            {
                String key = m.getColumnName(i);
                String alias = m.getColumnLabel(i);
                Object value = r.getObject(i);
                row.put((alias == null ? key : alias).toLowerCase(Locale.ROOT), value);
            }
        }
        catch (SQLException e)
        {
            throw new ResultSetException("Unable to access specific metadata from " +
                                         "result set metadata", e, ctx);
        }
        return row;
    }
}
