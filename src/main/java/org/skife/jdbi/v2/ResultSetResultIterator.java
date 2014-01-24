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
import java.sql.SQLException;
import java.sql.Statement;

class ResultSetResultIterator<Type> implements ResultIterator<Type>
{
    private final ResultSetMapper<Type> mapper;
    private final SQLStatement jdbiStatement;
    private final ResultSet results;
    private final StatementContext context;

    private volatile boolean alreadyAdvanced = false;
    private volatile int count = 0;
    private volatile boolean hasNext = false;
    private volatile boolean closed = false;

    ResultSetResultIterator(ResultSetMapper<Type> mapper,
                            SQLStatement jdbiStatement,
                            Statement stmt,
                            StatementContext context)
            throws SQLException
    {
        this.mapper = mapper;
        this.context = context;
        this.jdbiStatement = jdbiStatement;
        this.results = stmt.getResultSet();

        this.jdbiStatement.addCleanable(Cleanables.forResultSet(results));
    }

    public void close()
    {
        closed = true;
        jdbiStatement.cleanup();
    }

    public boolean hasNext()
    {
        if (closed) {
            return false;
        }

        if (alreadyAdvanced) {
            return hasNext;
        }

        hasNext = safeNext();

        if (hasNext) {
            alreadyAdvanced = true;
        }
        else {
            close();
        }

        return hasNext;
    }

    public Type next()
    {
        if (closed) {
            throw new IllegalStateException("iterator is closed");
        }

        if (!hasNext()) {
            close();
            throw new IllegalStateException("No element to advance to");
        }

        try {
            return mapper.map(count++, results, context);
        }
        catch (SQLException e) {
            throw new ResultSetException("Error thrown mapping result set into return type", e, context);
        }
        finally {
            alreadyAdvanced = safeNext();
            if (!alreadyAdvanced) {
                close();
            }
        }
    }

    public void remove()
    {
        throw new UnsupportedOperationException("Deleting from a result set iterator is not yet supported");
    }

    private boolean safeNext()
    {
        try {
            return results.next();
        }
        catch (SQLException e) {
            throw new ResultSetException("Unable to advance result set", e, context);
        }
    }
}
