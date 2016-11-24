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
package org.jdbi.v3.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.jdbi.v3.core.exception.NoResultsException;
import org.jdbi.v3.core.exception.ResultSetException;
import org.jdbi.v3.core.mapper.RowMapper;

class ResultSetResultIterator<Type> implements ResultIterator<Type>
{
    private final RowMapper<Type> mapper;
    private final ResultSet results;
    private final StatementContext context;

    private volatile boolean alreadyAdvanced = false;
    private volatile boolean hasNext = false;
    private volatile boolean closed = false;

    ResultSetResultIterator(RowMapper<Type> mapper,
                            ResultSet results,
                            StatementContext context)
    {
        this.mapper = mapper;
        this.context = context;
        this.results = results;

        if (results == null) {
            throw new NoResultsException("No results to iterate over", context);
        }

        this.context.getCleanables().add(Cleanables.forResultSet(results));
    }

    @Override
    public void close()
    {
        closed = true;
        context.close();
    }

    @Override
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

    @Override
    public Type next()
    {
        if (closed) {
            throw new IllegalStateException("iterator is closed");
        }

        if (!hasNext()) {
            close();
            throw new NoSuchElementException("No element to advance to");
        }

        try {
            return mapper.map(results, context);
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

    @Override
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
