/*
 * Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.exceptions.UnableToCloseResourceException;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class ResultSetResultIterator<Type> implements ResultIterator<Type>
{
    private final ResultSetMapper<Type> mapper;
    private final Statement stmt;
    private final ResultSet results;
    private boolean alreadyAdvanced = false;
    private int count = 0;
    private boolean hasNext = false;

    ResultSetResultIterator(ResultSetMapper<Type> mapper, Statement stmt, ResultSet results)
    {
        this.mapper = mapper;
        this.stmt = stmt;
        this.results = results;
    }

    protected ResultSet getResults()
    {
        return results;
    }

    public void close()
    {
        boolean failed_to_close_results = false;
        try
        {
            results.close();
        }
        catch (SQLException e)
        {
            failed_to_close_results = true;
        }
        try
        {
            stmt.close();
        }
        catch (SQLException e)
        {
            if (failed_to_close_results)
            {
                throw new UnableToCloseResourceException("unable to close statement", e);
            }
            else
            {
                throw new UnableToCloseResourceException("unable to close result set and statement", e);
            }
        }
    }

    public boolean hasNext()
    {
        if (alreadyAdvanced)
        {
            return hasNext;
        }

        try
        {
            hasNext = results.next();
        }
        catch (SQLException e)
        {
            throw new ResultSetException("Unable to advance result set", e);
        }
        if (!hasNext)
        {
            try
            {
                results.close();
            }
            catch (SQLException e)
            {
                throw new ResultSetException("Unable to close result set after iterating through to the end", e);
            }
        }
        alreadyAdvanced = true;
        return hasNext;
    }

    public Type next()
    {
        if (!alreadyAdvanced)
        {
            try
            {
                if (!results.next())
                {
                    throw new IllegalStateException("No element to advance to");
                }
            }
            catch (SQLException e)
            {
                throw new ResultSetException("Unable to advance result set", e);
            }
        }
        alreadyAdvanced = false;
        try
        {
            return mapper.map(count++, results);
        }
        catch (SQLException e)
        {
            throw new ResultSetException("Error thrown mapping result set into return type", e);
        }
    }

    public void remove()
    {
//        if (!allowDeletes) {
        throw new UnsupportedOperationException("Deleting from a result set iterator is not yet supported");
//                                                    "in order to call remove()");
//        }
//        try
//        {
//            if (alreadyAdvanced) {
//                results.previous();
//            }
//            results.deleteRow();
//            if (alreadyAdvanced) {
//                results.previous();
//            }
//        }
//        catch (SQLException e)
//        {
//            throw new ResultSetException("Unable to delete row from result set", e);
//        }
    }
}
