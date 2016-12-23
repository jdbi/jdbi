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
package org.jdbi.v3.core.statement;

import static org.jdbi.v3.core.result.ResultProducers.returningResults;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.ResultProducer;
import org.jdbi.v3.core.result.ResultSetCallback;
import org.jdbi.v3.core.result.ResultSetIterable;
import org.jdbi.v3.core.result.UnableToProduceResultException;

/**
 * Statement providing convenience result handling for SQL queries.
 * The default mapping to {@code Map<String, Object>} unfortunately
 * does not mesh well with SQL's case-insensitivity, so all
 * identifiers are converted to lower-case using {@link Locale#ROOT}.
 * If you require different behavior, implement a custom mapper.
 * The default mapper also carries a performance penalty because it must
 * inspect metadata for each row.
 */
public class Query extends SqlStatement<Query> implements ResultBearing, ResultSetIterable
{
    public Query(Handle handle, String sql)
    {
        super(handle, sql);
    }

    @Override
    @SuppressWarnings("resource")
    public <R> R execute(ResultProducer<R> producer)
    {
        try {
            return producer.produce(this::internalExecute, getContext());
        } catch (SQLException e) {
            try {
                close();
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }
            throw new UnableToProduceResultException(e, getContext());
        }
    }

    @Override
    public <R> R withResultSet(ResultSetCallback<R> callback) {
        return execute(returningResults()).withResultSet(callback);
    }

    /**
     * Specify the fetch size for the query. This should cause the results to be
     * fetched from the underlying RDBMS in groups of rows equal to the number passed.
     * This is useful for doing chunked streaming of results when exhausting memory
     * could be a problem.
     *
     * @param fetchSize the number of rows to fetch in a bunch
     *
     * @return the modified query
     */
    public Query setFetchSize(final int fetchSize)
    {
        return addCustomizer(new StatementCustomizers.FetchSizeCustomizer(fetchSize));
    }

    /**
     * Specify the maximum number of rows the query is to return. This uses the underlying JDBC
     * {@link Statement#setMaxRows(int)}}.
     *
     * @param maxRows maximum number of rows to return
     *
     * @return modified query
     */
    public Query setMaxRows(final int maxRows)
    {
        return addCustomizer(new StatementCustomizers.MaxRowsCustomizer(maxRows));
    }

    /**
     * Specify the maximum field size in the result set. This uses the underlying JDBC
     * {@link Statement#setMaxFieldSize(int)}
     *
     * @param maxFields maximum field size
     *
     * @return modified query
     */
    public Query setMaxFieldSize(final int maxFields)
    {
        return addCustomizer(new StatementCustomizers.MaxFieldSizeCustomizer(maxFields));
    }

    /**
     * Specify that the fetch order should be reversed, uses the underlying
     * {@link Statement#setFetchDirection(int)}
     *
     * @return the modified query
     */
    public Query fetchReverse()
    {
        return setFetchDirection(ResultSet.FETCH_REVERSE);
    }

    /**
     * Specify that the fetch order should be forward, uses the underlying
     * {@link Statement#setFetchDirection(int)}
     *
     * @return the modified query
     */
    public Query fetchForward()
    {
        return setFetchDirection(ResultSet.FETCH_FORWARD);
    }

    /**
     * Specify that the result set should be concurrent updatable.
     *
     * This will allow the update methods to be called on the result set produced by this
     * Query.
     *
     * @return the modified query
     */
    public Query concurrentUpdatable() {
        getConfig(StatementConfiguration.class).setConcurrentUpdatable(true);
        return this;
    }
}
