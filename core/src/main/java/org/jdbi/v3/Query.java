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
import java.util.Collection;
import java.util.Locale;

import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.tweak.ResultSetMapper;
import org.jdbi.v3.tweak.StatementBuilder;
import org.jdbi.v3.tweak.StatementCustomizer;
import org.jdbi.v3.tweak.StatementLocator;
import org.jdbi.v3.tweak.StatementRewriter;

/**
 * Statement providing convenience result handling for SQL queries.
 * The default mapping to {@code Map<String, Object>} unfortunately
 * does not mesh well with SQL's case-insensitivity, so all
 * identifiers are converted to lower-case using {@link Locale#ROOT}.
 * If you require different behavior, implement a custom mapper.
 * The default mapper also carries a performance penalty because it must
 * inspect metadata for each row.
 */
public class Query<ResultType> extends SQLStatement<Query<ResultType>> implements ResultBearing<ResultType>
{
    private final ResultSetMapper<ResultType> mapper;
    private final MappingRegistry             mappingRegistry;

    Query(Binding params,
          ResultSetMapper<ResultType> mapper,
          StatementLocator locator,
          StatementRewriter statementRewriter,
          Handle handle,
          StatementBuilder cache,
          SqlName sql,
          ConcreteStatementContext ctx,
          TimingCollector timingCollector,
          Collection<StatementCustomizer> customizers,
          MappingRegistry mappingRegistry,
          Foreman foreman)
    {
        super(params, locator, statementRewriter, handle, cache, sql, ctx, timingCollector, customizers, foreman);
        this.mapper = mapper;
        this.mappingRegistry = mappingRegistry;
    }

    /**
     * Obtain a forward-only result set iterator. Note that you must explicitely close
     * the iterator to close the underlying resources.
     */
    @Override
    public ResultIterator<ResultType> iterator()
    {
        return this.internalExecute(new QueryResultMunger<ResultIterator<ResultType>>()
        {
            @Override
            public ResultIterator<ResultType> munge(Statement stmt) throws SQLException
            {
                return new ResultSetResultIterator<ResultType>(mapper,
                                                               Query.this,
                                                               stmt,
                                                               stmt.getResultSet(),
                                                               getContext());
            }
        });
    }

    /**
     * Provide basic JavaBean mapping capabilities. Will instantiate an instance of resultType
     * for each row and set the JavaBean properties which match fields in the result set.
     *
     * @param resultType JavaBean class to map result set fields into the properties of, by name
     *
     * @return a Query which provides the bean property mapping
     */
    public <Type> Query<Type> mapToBean(Class<Type> resultType)
    {
        return this.map(new BeanMapper<Type>(resultType));
    }

    /**
     * Makes use of registered mappers to map the result set to the desired type.
     *
     * @param resultType the type to map the query results to
     *
     * @return a new query instance which will map to the desired type
     *
     * @see DBI#registerMapper(org.jdbi.v3.tweak.ResultSetMapper)
     * @see DBI#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(org.jdbi.v3.tweak.ResultSetMapper)
     */
    public <T> Query<T> mapTo(Class<T> resultType)
    {
        return this.map(new RegisteredMapper<T>(resultType, mappingRegistry));
    }

    public <T> Query<T> map(ResultSetMapper<T> mapper)
    {
        return new Query<T>(getParameters(),
                            mapper,
                            getStatementLocator(),
                            getRewriter(),
                            getHandle(),
                            getStatementBuilder(),
                            getSql(),
                            getConcreteContext(),
                            getTimingCollector(),
                            getStatementCustomizers(),
                            MappingRegistry.copyOf(mappingRegistry),
                            getForeman().createChild());
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
     * Specify the maimum number of rows the query is to return. This uses the underlying JDBC
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
     * Specify the maimum field size in the result set. This uses the underlying JDBC
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
        getConcreteContext().setConcurrentUpdatable(true);
        return this;
    }

    public void registerMapper(ResultSetMapper<?> m)
    {
        this.mappingRegistry.addMapper(new InferredMapperFactory<>(m));
    }

    public void registerMapper(ResultSetMapperFactory m)
    {
        this.mappingRegistry.addMapper(m);
    }

    public void registerColumnMapper(ResultColumnMapper<?> m)
    {
        this.mappingRegistry.addColumnMapper(m);
    }

    public void registerColumnMapper(ResultColumnMapperFactory m)
    {
        this.mappingRegistry.addColumnMapper(m);
    }
}
