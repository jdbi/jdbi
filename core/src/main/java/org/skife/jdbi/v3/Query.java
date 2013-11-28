/*
 * Copyright (C) 2004 - 2013 Brian McCallister
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
package org.skife.jdbi.v3;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.skife.jdbi.v3.exceptions.ResultSetException;
import org.skife.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v3.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v3.tweak.ResultSetMapper;
import org.skife.jdbi.v3.tweak.SQLLog;
import org.skife.jdbi.v3.tweak.StatementBuilder;
import org.skife.jdbi.v3.tweak.StatementCustomizer;
import org.skife.jdbi.v3.tweak.StatementLocator;
import org.skife.jdbi.v3.tweak.StatementRewriter;

/**
 * Statement providing convenience result handling for SQL queries.
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
          String sql,
          ConcreteStatementContext ctx,
          SQLLog log,
          TimingCollector timingCollector,
          Collection<StatementCustomizer> customizers,
          MappingRegistry mappingRegistry,
          Foreman foreman)
    {
        super(params, locator, statementRewriter, handle, cache, sql, ctx, log, timingCollector, customizers, foreman);
        this.mapper = mapper;
        this.mappingRegistry = new MappingRegistry(mappingRegistry);
    }

    /**
     * Executes the select
     * <p/>
     * Will eagerly load all results
     *
     * @throws UnableToCreateStatementException
     *                            if there is an error creating the statement
     * @throws UnableToExecuteStatementException
     *                            if there is an error executing the statement
     * @throws ResultSetException if there is an error dealing with the result set
     */
    @Override
    public List<ResultType> list()
    {
        try {
            return this.internalExecute(new QueryResultSetMunger<List<ResultType>>(this)
            {
                @Override
                public List<ResultType> munge(ResultSet rs) throws SQLException
                {
                    List<ResultType> result_list = new ArrayList<ResultType>();
                    int index = 0;
                    while (rs.next()) {
                        result_list.add(mapper.map(index++, rs, getContext()));
                    }
                    return result_list;
                }
            });
        }
        finally {
            cleanup();
        }
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
                                                               getContext());
            }
        });
    }

    /**
     * Executes the select.
     * <p/>
     * Specifies a maximum of one result on the JDBC statement, and map that one result
     * as the return value, or return null if there is nothing in the results
     *
     * @return first result, mapped, or null if there is no first result
     */
    @Override
    public ResultType first()
    {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Provide basic JavaBean mapping capabilities. Will instantiate an instance of resultType
     * for each row and set the JavaBean properties which match fields in the result set.
     *
     * @param resultType JavaBean class to map result set fields into the properties of, by name
     *
     * @return a Query which provides the bean property mapping
     */
    public <Type> Query<Type> map(Class<Type> resultType)
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
     * @see DBI#registerMapper(org.skife.jdbi.v3.tweak.ResultSetMapper)
     * @see DBI#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(org.skife.jdbi.v3.tweak.ResultSetMapper)
     */
    public <T> Query<T> mapTo(Class<T> resultType)
    {
        return this.map(new RegisteredMapper(resultType, mappingRegistry));
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
                            getLog(),
                            getTimingCollector(),
                            getStatementCustomizers(),
                            new MappingRegistry(mappingRegistry),
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

    public void registerMapper(ResultSetMapper m)
    {
        this.mappingRegistry.add(new InferredMapperFactory(m));
    }

    public void registerMapper(ResultSetMapperFactory m)
    {
        this.mappingRegistry.add(m);
    }
}
