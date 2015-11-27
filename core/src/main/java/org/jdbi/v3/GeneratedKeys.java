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

import com.google.common.reflect.TypeToken;
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
    private final CollectorFactoryRegistry collectorFactoryRegistry;

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
                  StatementContext context,
                  CollectorFactoryRegistry collectorFactoryRegistry)
    {
        this.mapper = mapper;
        this.jdbiStatement = jdbiStatement;
        this.stmt = stmt;
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
        this.collectorFactoryRegistry = collectorFactoryRegistry.createChild();
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
        return new ResultSetResultIterator<Type>(mapper, jdbiStatement, stmt, results, context);
    }

    @Override
    public <ContainerType> ContainerType collectInto(TypeToken<ContainerType> containerType) {
        return collect(collectorFactoryRegistry.createCollectorFor(containerType));
    }
}
