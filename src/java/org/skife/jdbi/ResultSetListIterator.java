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

import java.util.ListIterator;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetListIterator implements ListIterator
{
    private final HandleQuery query;
    private final RowMapper mapper;
    private final ResultSet results;

    public ResultSetListIterator(HandleQuery query, RowMapper mapper, ResultSet results)
    {
        this.query = query;
        this.mapper = mapper;

        this.results = results;
    }

    public boolean hasNext()
    {
        return false;
    }

    public Object next()
    {
        return null;
    }

    public boolean hasPrevious()
    {
        return false;
    }

    public Object previous()
    {
        return null;
    }

    public int nextIndex()
    {
        return 0;
    }

    public int previousIndex()
    {
        return 0;
    }

    public void remove()
    {
        try
        {
            results.deleteRow();
        }
        catch (SQLException e)
        {
            throw new DBIException("Error trying to delete a row in a result set", e);
        }
    }

    public void set(Object o)
    {
        throw new UnsupportedOperationException("Cannot insert into result set");
    }

    public void add(Object o)
    {
        throw new UnsupportedOperationException("Cannot insert into a result set");
    }

    public void close()
    {
        try
        {
            results.close();
        }
        catch (SQLException e)
        {
            throw new DBIException("Exception closing result set in list iterator", e);
        }
        query.close();
    }
}
