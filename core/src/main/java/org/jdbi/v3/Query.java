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

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Locale;

import org.jdbi.v3.exception.ResultSetException;
import org.jdbi.v3.mapper.BeanMapper;
import org.jdbi.v3.mapper.ColumnMapper;
import org.jdbi.v3.mapper.ColumnMapperFactory;
import org.jdbi.v3.mapper.InferredRowMapperFactory;
import org.jdbi.v3.mapper.RowMapper;
import org.jdbi.v3.mapper.RowMapperFactory;
import org.jdbi.v3.statement.StatementBuilder;
import org.jdbi.v3.statement.StatementCustomizer;
import org.jdbi.v3.util.GenericType;

/**
 * Statement providing convenience result handling for SQL queries.
 * The default mapping to {@code Map<String, Object>} unfortunately
 * does not mesh well with SQL's case-insensitivity, so all
 * identifiers are converted to lower-case using {@link Locale#ROOT}.
 * If you require different behavior, implement a custom mapper.
 * The default mapper also carries a performance penalty because it must
 * inspect metadata for each row.
 */
public class Query<ResultType> extends SqlStatement<Query<ResultType>> implements ResultBearing<ResultType>
{
    private final RowMapper<ResultType> mapper;

    Query(JdbiConfig config,
          Binding params,
          RowMapper<ResultType> mapper,
          Handle handle,
          StatementBuilder cache,
          String sql,
          StatementContext ctx,
          Collection<StatementCustomizer> customizers)
    {
        super(config, params, handle, cache, sql, ctx, customizers);
        this.mapper = mapper;
    }

    /**
     * Obtain a forward-only result set iterator. Note that you must explicitely close
     * the iterator to close the underlying resources.
     */
    @Override
    public ResultIterator<ResultType> iterator()
    {
        final PreparedStatement stmt = internalExecute();
        try {
            return new ResultSetResultIterator<>(mapper,
                    Query.this,
                    stmt,
                    stmt.getResultSet(),
                    getContext());
        } catch (SQLException e) {
            try {
                stmt.close();
            } catch (SQLException e1) {
                e.addSuppressed(e1);
            }
            throw new ResultSetException("Could not get result set", e, getContext());
        }
    }

    /**
     * Provide basic JavaBean mapping capabilities. Will instantiate an instance of resultType
     * for each row and set the JavaBean properties which match fields in the result set.
     *
     * @param resultType JavaBean class to map result set fields into the properties of, by name
     * @param <T> the JavaBean class to map result rows to.
     *
     * @return a Query which provides the bean property mapping
     */
    public <T> Query<T> mapToBean(Class<T> resultType)
    {
        return this.map(new BeanMapper<>(resultType));
    }

    /**
     * Makes use of registered mappers to map the result rows to the desired type.
     *
     * @param resultType the type to map the query result rows to
     * @param <T> the type to map result rows to
     *
     * @return a new query instance which will map to the desired type
     *
     * @see Jdbi#registerRowMapper(RowMapper)
     * @see Jdbi#registerRowMapper(RowMapperFactory)
     * @see Handle#registerRowMapper(RowMapperFactory)
     * @see Handle#registerRowMapper(RowMapper)
     */
    @SuppressWarnings("unchecked")
    public <T> Query<T> mapTo(Class<T> resultType)
    {
        return (Query<T>) this.mapTo((Type) resultType);
    }

    /**
     * Makes use of registered mappers to map the result rows to the desired type.
     *
     * @param resultType the type to map the query result rows to
     * @param <T> the type to map result rows to
     *
     * @return a new query instance which will map rows to the desired type
     *
     * @see Jdbi#registerRowMapper(RowMapper)
     * @see Jdbi#registerRowMapper(RowMapperFactory)
     * @see Handle#registerRowMapper(RowMapperFactory)
     * @see Handle#registerRowMapper(RowMapper)
     */
    @SuppressWarnings("unchecked")
    public <T> Query<T> mapTo(GenericType<T> resultType)
    {
        return (Query<T>) this.mapTo(resultType.getType());
    }

    /**
     * Makes use of registered mappers to map the result rows to the desired type.
     *
     * @param resultType the type to map the query result rows to
     *
     * @return a new query instance which will map to the desired type
     *
     * @see Jdbi#registerRowMapper(RowMapper)
     * @see Jdbi#registerRowMapper(RowMapperFactory)
     * @see Handle#registerRowMapper(RowMapperFactory)
     * @see Handle#registerRowMapper(RowMapper)
     */
    public Query<?> mapTo(Type resultType) {
        return this.map(rowMapperForType(resultType));
    }

    public <T> Query<T> map(RowMapper<T> mapper)
    {
        return new Query<>(config,
                getParams(),
                mapper,
                getHandle(),
                getStatementBuilder(),
                getSql(),
                getContext(),
                getStatementCustomizers());
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
    public Query<ResultType> setFetchSize(final int fetchSize)
    {
        this.addStatementCustomizer(new StatementCustomizers.FetchSizeCustomizer(fetchSize));
        return this;
    }

    /**
     * Specify the maximum number of rows the query is to return. This uses the underlying JDBC
     * {@link Statement#setMaxRows(int)}}.
     *
     * @param maxRows maximum number of rows to return
     *
     * @return modified query
     */
    public Query<ResultType> setMaxRows(final int maxRows)
    {
        this.addStatementCustomizer(new StatementCustomizers.MaxRowsCustomizer(maxRows));
        return this;
    }

    /**
     * Specify the maximum field size in the result set. This uses the underlying JDBC
     * {@link Statement#setMaxFieldSize(int)}
     *
     * @param maxFields maximum field size
     *
     * @return modified query
     */
    public Query<ResultType> setMaxFieldSize(final int maxFields)
    {
        this.addStatementCustomizer(new StatementCustomizers.MaxFieldSizeCustomizer(maxFields));
        return this;
    }

    /**
     * Specify that the fetch order should be reversed, uses the underlying
     * {@link Statement#setFetchDirection(int)}
     *
     * @return the modified query
     */
    public Query<ResultType> fetchReverse()
    {
        setFetchDirection(ResultSet.FETCH_REVERSE);
        return this;
    }

    /**
     * Specify that the fetch order should be forward, uses the underlying
     * {@link Statement#setFetchDirection(int)}
     *
     * @return the modified query
     */
    public Query<ResultType> fetchForward()
    {
        setFetchDirection(ResultSet.FETCH_FORWARD);
        return this;
    }

    /**
     * Specify that the result set should be concurrent updatable.
     *
     * This will allow the update methods to be called on the result set produced by this
     * Query.
     *
     * @return the modified query
     */
    public Query<ResultType> concurrentUpdatable() {
        getContext().setConcurrentUpdatable(true);
        return this;
    }

    public void registerRowMapper(RowMapper<?> m)
    {
        config.mappingRegistry.addRowMapper(new InferredRowMapperFactory(m));
    }

    public void registerRowMapper(RowMapperFactory m)
    {
        config.mappingRegistry.addRowMapper(m);
    }

    public void registerColumnMapper(ColumnMapper<?> m)
    {
        config.mappingRegistry.addColumnMapper(m);
    }

    public void registerColumnMapper(ColumnMapperFactory m)
    {
        config.mappingRegistry.addColumnMapper(m);
    }
}
