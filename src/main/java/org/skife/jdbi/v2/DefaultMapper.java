/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DefaultMapper implements ResultSetMapper<Map<String, Object>>
{
    @Override
    public Map<String, Object> map(int index, ResultSet r, StatementContext ctx)
    {
        Map<String, Object> row = new DefaultResultMap();
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
                row.put(alias != null ? alias : key, value);
            }
        }
        catch (SQLException e)
        {
            throw new ResultSetException("Unable to access specific metadata from " +
                                         "result set metadata", e, ctx);
        }
        return row;
    }

    private static class DefaultResultMap extends HashMap<String, Object>
    {
        public static final long serialVersionUID = 1L;

        @Override
        public Object get(Object o)
        {
            return super.get(((String)o).toLowerCase());
        }

        @Override
        public Object put(String key, Object value)
        {
            return super.put(key.toLowerCase(), value);
        }

        @Override
        public boolean containsKey(Object key)
        {
            return super.containsKey(((String)key).toLowerCase());
        }
    }
}
