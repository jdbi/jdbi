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
import java.sql.Statement;

import org.jdbi.v3.core.exception.ResultSetException;
import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.jdbi.v3.core.mapper.RowMapper;

/**
 * Wrapper object for generated keys as returned by the {@link Statement#getGeneratedKeys()}
 *
 * @param <T> the key type returned
 */
public class GeneratedKeys<T> implements ResultBearing<T>
{
    private final RowMapper<T>             mapper;
    private final SqlStatement<?>          jdbiStatement;
    private final ResultSet                results;
    private final StatementContext         context;

    /**
     * Creates a new wrapper object for generated keys as returned by the {@link Statement#getGeneratedKeys()}
     * method for update and insert statement for drivers that support this function.
     *
     * @param mapper        Maps the generated keys result set to an object
     * @param jdbiStatement The original jDBI statement
     * @param stmt          The corresponding sql statement
     * @param context       The statement context
     */
    GeneratedKeys(RowMapper<T> mapper,
                  SqlStatement<?> jdbiStatement,
                  Statement stmt,
                  StatementContext context)
    {
        this.mapper = mapper;
        this.jdbiStatement = jdbiStatement;
        try {
            this.results = stmt.getGeneratedKeys();
        } catch (SQLException e) {
            try {
                stmt.close();
            } catch (SQLException e1) {
                e.addSuppressed(e1);
            }
            throw new ResultSetException("Could not get generated keys", e, context);
        }
        this.context = context;
        this.jdbiStatement.addCleanable(Cleanables.forResultSet(results));
    }

    /**
     * Returns a iterator over all generated keys.
     *
     * @return The key iterator
     */
    @Override
    public ResultIterator<T> iterator()
    {
        if (results == null) {
            return new EmptyResultIterator<>(context);
        }

        try {
            return new ResultSetResultIterator<>(mapper, results, context);
        }
        catch (SQLException e) {
            throw new UnableToExecuteStatementException(e, context);
        }
    }

    @Override
    public <R> R execute(StatementExecutor<T, R> executor)
    {
        try {
            return executor.execute(mapper, results, context);
        } catch (SQLException e) {
            throw new UnableToExecuteStatementException(e, context);
        }
    }

    @Override
    public StatementContext getContext()
    {
        return context;
    }
}
