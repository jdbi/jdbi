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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.ListIterator;

class HandleQuery implements Query
{
    private final RowMapper mapper;
    private final PreparedStatement statement;

    public HandleQuery(RowMapper mapper, PreparedStatement statement) {
        this.mapper = mapper;
        this.statement = statement;
    }

    public Iterator iterator()
    {
        try
        {
            ResultSet results = statement.executeQuery();
            return new ResultSetIterator(this, mapper, results);
        }
        catch (SQLException e)
        {
            throw new DBIException(e.getMessage(), e);
        }
    }

    public void close()
    {
        try
        {
            statement.close();
        }
        catch (SQLException e)
        {
            throw new DBIException("Error closing statement when query is closed", e);
        }
    }

    public ListIterator listIterator()
    {
        try
        {
            ResultSet results = statement.executeQuery();
            return new ResultSetListIterator(this, mapper, results);
        }
        catch (SQLException e)
        {
            throw new DBIException(e.getMessage(), e);
        }
    }
}
