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
import java.util.ArrayList;
import java.util.List;

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
    private final ContainerFactoryRegistry containerFactoryRegistry;

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
                  ContainerFactoryRegistry containerFactoryRegistry) throws SQLException
    {
        this.mapper = mapper;
        this.jdbiStatement = jdbiStatement;
        this.stmt = stmt;
        this.results = stmt.getGeneratedKeys();
        this.context = context;
        this.containerFactoryRegistry = containerFactoryRegistry.createChild();
        this.jdbiStatement.addCleanable(Cleanables.forResultSet(results));
    }

    /**
     * Returns the first generated key.
     *
     * @return The key or null if no keys were returned
     */
    public Type first()
    {
        try {
            if (results != null && results.next()) {
                return mapper.map(0, results, context);
            }
            else {
                // no result matches
                return null;
            }
        }
        catch (SQLException e) {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e, context);
        }
        finally {
            jdbiStatement.cleanup();
        }
    }

    public <T> T first(Class<T> containerType)
    {
//        return containerFactoryRegistry.lookup(containerType).create(Arrays.asList(first()));
        throw new UnsupportedOperationException("Not Yet Implemented!");
    }

    public <ContainerType> ContainerType list(Class<ContainerType> containerType)
    {
//        return containerFactoryRegistry.lookup(containerType).create(Arrays.asList(list()));
        throw new UnsupportedOperationException("Not Yet Implemented!");
    }

    public List<Type> list(int maxRows)
    {
        try {
            int idx = 0;
            List<Type> resultList = new ArrayList<Type>();

            if (results != null && ++idx <= maxRows && !results.isClosed()) {
                int index = 0;
                while (results.next()) {
                    resultList.add(mapper.map(index++, results, context));
                }
            }
            return resultList;
        }
        catch (SQLException e) {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e, context);
        }
        finally {
            jdbiStatement.cleanup();
        }
    }

    /**
     * Returns a list of all generated keys.
     *
     * @return The list of keys or an empty list if no keys were returned
     */
    public List<Type> list()
    {
        return list(Integer.MAX_VALUE);
    }

    /**
     * Returns a iterator over all generated keys.
     *
     * @return The key iterator
     */
    public ResultIterator<Type> iterator()
    {
        try {
            return new ResultSetResultIterator<Type>(mapper, jdbiStatement, stmt, context);
        }
        catch (SQLException e) {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e, context);
        }
    }

    /**
     * Used to execute the query and traverse the generated keys with a accumulator.
     * <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Folding</a> over the
     * keys involves invoking a callback for each row, passing into the callback the return value
     * from the previous function invocation.
     *
     * @param accumulator The initial accumulator value
     * @param folder      Defines the function which will fold over the result set.
     *
     * @return The return value from the last invocation of {@link Folder#fold(Object, java.sql.ResultSet)}
     *
     * @see org.skife.jdbi.v2.Folder
     */
    public <AccumulatorType> AccumulatorType fold(AccumulatorType accumulator, final Folder2<AccumulatorType> folder)
    {
        try {
            AccumulatorType value = accumulator;

            if (results != null && !results.isClosed()) {
                while (results.next()) {
                    value = folder.fold(value, results, context);
                }
            }
            return value;
        }
        catch (SQLException e) {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e, context);
        }
        finally {
            jdbiStatement.cleanup();
        }
    }
}
