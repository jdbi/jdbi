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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.ListIterator;

public class ResultSetListIterator implements ListIterator, Iterator
{
    private final Query query;
    private final RowMapper mapper;
    private final ResultSet results;
    private final String[] columns;


    private boolean lastNext;
    private boolean lastPrevious;
    private boolean hasNextCalled = false;
    private boolean hasPreviousCalled = false;


    public ResultSetListIterator(HandleQuery query, RowMapper mapper, ResultSet results) throws SQLException
    {
        this.query = query;
        this.mapper = mapper;
        this.results = results;
        final ResultSetMetaData metadata = results.getMetaData();
        final int count = metadata.getColumnCount();
        columns = new String[count];
        for (int i = 1; i != count + 1; ++i)
        {
            final String column_name = metadata.getColumnName(i);
            final String column_label = metadata.getColumnLabel(i);
            columns[i - 1] = column_label != null ? column_label : column_name;
        }
    }

    public boolean hasPrevious()
    {
        hasPreviousCalled = true;
        try
        {
            lastPrevious = results.previous();
        }
        catch (SQLException e)
        {
            throw new DBIException("Exception while moving to previous row in result set", e);
        }
        return lastPrevious;
    }

    public Object previous()
    {
        if (!hasPreviousCalled)
        {
            try
            {
                results.previous();
            }
            catch (SQLException e)
            {
                throw new DBIException("Exception while moving to previous row of resultset", e);
            }
        }
        hasNextCalled = false;
        hasPreviousCalled = false;
        try
        {
            return mapper.map(columns, results);
        }
        catch (SQLException e)
        {
            throw new DBIException("Exception while extracting row values", e);
        }
    }

    public int nextIndex()
    {
        return 0;
    }

    public int previousIndex()
    {
        return 0;
    }

    public void set(Object o)
    {
        throw new UnsupportedOperationException("Cannot insert into result set");
    }

    public void add(Object o)
    {
        throw new UnsupportedOperationException("Cannot insert into a result set");
    }

    public boolean hasNext()
    {
        if (hasNextCalled)
        {
            return lastNext;
        }
        else
        {
            hasNextCalled = true;
            try
            {
                lastNext = results.next();
                return lastNext;
            }
            catch (SQLException e)
            {
                throw new DBIException(e.getMessage(), e);
            }
        }
    }

    public Object next()
    {
        if (!hasNextCalled)
        {
            try
            {
                results.next();
            }
            catch (SQLException e)
            {
                throw new DBIException("Exception while trying to advance result set", e);
            }
        }
        hasNextCalled = false;

        try
        {
            return mapper.map(columns, results);
        }
        catch (SQLException e)
        {
            throw new DBIException("Exception while extracting data from a row", e);
        }
    }

    public void remove()
    {
        try
        {
            results.deleteRow();
        }
        catch (SQLException e)
        {
            throw new DBIException(e.getMessage(), e);
        }
    }

    public void close()
    {
        this.query.close();
        try
        {
            this.results.close();
        }
        catch (SQLException e)
        {
            throw new DBIException("Error while closing result set from an iterator", e);
        }
    }
}
