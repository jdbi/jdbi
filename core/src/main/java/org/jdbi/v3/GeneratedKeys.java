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
package org.jdbi.v3;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jdbi.v3.exceptions.ResultSetException;
import org.jdbi.v3.tweak.ResultSetMapper;

/**
 * Wrapper object for generated keys as returned by the {@link Statement#getGeneratedKeys()}
 *
 * @param <Type> the key type returned
 */
public class GeneratedKeys<Type> implements ResultBearing<Type>
{
    private final ResultSetMapper<Type>    mapper;
    private final SQLStatement<?>          jdbiStatement;
    private final Statement                stmt;
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
    GeneratedKeys(ResultSetMapper<Type> mapper,
                  SQLStatement<?> jdbiStatement,
                  Statement stmt,
                  StatementContext context) throws SQLException
    {
        this.mapper = mapper;
        this.jdbiStatement = jdbiStatement;
        this.stmt = stmt;
        this.results = stmt.getGeneratedKeys();
        this.context = context;
        this.jdbiStatement.addCleanable(Cleanables.forResultSet(results));
    }

    /**
     * Returns a iterator over all generated keys.
     *
     * @return The key iterator
     */
    @Override
    public ResultIterator<Type> iterator()
    {
        if (results == null) {
            return new EmptyResultIterator<Type>();
        }

        try {
            return new ResultSetResultIterator<Type>(mapper, jdbiStatement, stmt, results, context);
        }
        catch (SQLException e) {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e, context);
        }
    }
}
