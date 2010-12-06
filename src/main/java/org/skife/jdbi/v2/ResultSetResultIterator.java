/*
 * Copyright 2004-2007 Brian McCallister
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
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class ResultSetResultIterator<Type> implements ResultIterator<Type>
{
    private final ResultSetMapper<Type> mapper;
    private final SQLStatement jdbiStatement;
    private final ResultSet results;
    private final StatementContext context;
    private boolean alreadyAdvanced = false;
    private int count = 0;
    private boolean hasNext = false;

    ResultSetResultIterator(ResultSetMapper<Type> mapper,
                            SQLStatement jdbiStatement,
                            Statement stmt,
                            StatementContext context)
            throws SQLException
    {
        this.mapper = mapper;
        this.jdbiStatement = jdbiStatement;
        this.results = stmt.getResultSet();
        this.context = context;
    }

    protected ResultSet getResults()
    {
        return results;
    }

    public void close()
    {
        QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY.cleanup(jdbiStatement, null, results);
    }

    public boolean hasNext()
    {
        if (alreadyAdvanced) {
            return hasNext;
        }

        try {
            hasNext = results.next();
        }
        catch (SQLException e) {
            throw new ResultSetException("Unable to advance result set", e, context);
        }
        if (!hasNext) {
            try {
                results.close();
            }
            catch (SQLException e) {
                throw new ResultSetException("Unable to close result set after iterating through to the end", e, context);
            }
        }
        alreadyAdvanced = true;
        return hasNext;
    }

    public Type next()
    {
        if (!alreadyAdvanced) {
            try {
                if (!results.next()) {
                    throw new IllegalStateException("No element to advance to");
                }
            }
            catch (SQLException e) {
                throw new ResultSetException("Unable to advance result set", e, context);
            }
        }
        alreadyAdvanced = false;
        try {
            return mapper.map(count++, results, context);
        }
        catch (SQLException e) {
            throw new ResultSetException("Error thrown mapping result set into return type", e, context);
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
