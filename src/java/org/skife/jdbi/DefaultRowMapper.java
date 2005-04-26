/* Copyright 2004-2005 Brian McCallister
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
package org.skife.jdbi;

import org.skife.jdbi.unstable.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

class DefaultRowMapper implements RowMapper
{
    public Map map(String[] column_names, ResultSet results) throws SQLException
    {
        final Map row = new RowMap();
        for (int i = 0; i != column_names.length; i++)
        {
            final String column = column_names[i];
            final Object value = results.getObject(i + 1);
            row.put(column, value);
        }
        return row;
    }
}
