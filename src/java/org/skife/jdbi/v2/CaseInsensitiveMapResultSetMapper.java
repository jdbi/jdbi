/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.ResultSetException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CaseInsensitiveMapResultSetMapper<Result extends Map<String, Object>>
        implements ResultSetMapper<Map<String, Object>>
{
    public Map<String, Object> map(int index, ResultSet r)
    {
        Map<String, Object> row = new CaseInsensitiveMap<String, Object>();
        ResultSetMetaData m;
        try
        {
            m = r.getMetaData();
        }
        catch (SQLException e)
        {
            throw new ResultSetException("Unable to obtain metadata from result set", e);
        }

        try
        {
            for (int i = 0; i < m.getColumnCount(); i ++)
            {
                String key = m.getColumnName(i);
                String alias = m.getColumnLabel(i);
                Object value = r.getObject(i);
                row.put(alias != null ? alias : key, value);
            }
        }
        catch (SQLException e)
        {
            throw new ResultSetException("Unable to access specific metadata from " +
                                         "result set metadata", e);
        }
        return row;
    }

    private static class CaseInsensitiveMap<Key extends String, Value> extends HashMap<Key, Value>
    {
        public Value get(String o)
        {
            return super.get(o.toLowerCase());
        }
    }
}
