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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementCustomizer;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.util.SingleColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
          Foreman foreman,
          ContainerFactoryRegistry containerFactoryRegistry)
    {
        super(params, locator, statementRewriter, handle, cache, sql, ctx, log, timingCollector, customizers, foreman, containerFactoryRegistry);
        this.mapper = mapper;
        this.mappingRegistry = mappingRegistry;
    }

    /**
     * Executes the select
     * <p/>
     * Will eagerly load all results
     *
     * @throws org.skife.jdbi.v2.exceptions.UnableToCreateStatementException
     *                            if there is an error creating the statement
     * @throws org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException
     *                            if there is an error executing the statement
     * @throws org.skife.jdbi.v2.exceptions.ResultSetException if there is an error dealing with the result set
     */
    @Override
    public List<ResultType> list()
    {
        return list(List.class);
    }

    @Override
    public <ContainerType> ContainerType list(Class<ContainerType> containerType)
    {
        ContainerBuilder<ContainerType> builder = getContainerMapperRegistry().createBuilderFor(containerType);
        return fold(builder, new Folder3<ContainerBuilder<ContainerType>, ResultType>()
        {
            @Override
            public ContainerBuilder<ContainerType> fold(ContainerBuilder<ContainerType> accumulator,
                                                        ResultType rs,
                                                        FoldController ctl,
                                                        StatementContext ctx) throws SQLException
            {
                accumulator.add(rs);
                return accumulator;
            }
        }).build();
    }

    /**
     * Executes the select
     * <p/>
     * Will eagerly load all results up to a maximum of <code>maxRows</code>
     *
     * @param maxRows The maximum number of results to include in the result, any
     *                rows in the result set beyond this number will be ignored.
     *
     * @throws org.skife.jdbi.v2.exceptions.UnableToCreateStatementException
     *                            if there is an error creating the statement
     * @throws org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException
     *                            if there is an error executing the statement
     * @throws org.skife.jdbi.v2.exceptions.ResultSetException if there is an error dealing with the result set
     */
    @Override
    public List<ResultType> list(final int maxRows)
    {
        try {
            return this.internalExecute(new QueryResultSetMunger<List<ResultType>>(this)
            {
                @Override
                public List<ResultType> munge(ResultSet rs) throws SQLException
                {
                    List<ResultType> result_list = new ArrayList<ResultType>();
                    int index = 0;
                    while (rs.next() && index < maxRows) {
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
     * Used to execute the query and traverse the result set with a accumulator.
     * <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Folding</a> over the
     * result involves invoking a callback for each row, passing into the callback the return value
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
        final AtomicReference<AccumulatorType> acc = new AtomicReference<AccumulatorType>(accumulator);

        try {
            this.internalExecute(new QueryResultSetMunger<Void>(this)
            {
                @Override
                public Void munge(ResultSet rs) throws SQLException
                {
                    while (rs.next()) {
                        acc.set(folder.fold(acc.get(), rs, getContext()));
                    }
                    return null;
                }
            });
            return acc.get();
        }
        finally {
            cleanup();
        }
    }

    public <AccumulatorType> AccumulatorType fold(final AccumulatorType accumulator,
                                                  final Folder3<AccumulatorType, ResultType> folder)
    {
        try {
            return this.internalExecute(new QueryResultSetMunger<AccumulatorType>(this)
            {
                private int idx = 0;
                private AccumulatorType ac = accumulator;

                @Override
                protected AccumulatorType munge(ResultSet rs) throws SQLException
                {
                    final FoldController ctl = new FoldController(rs);
                    while (!ctl.isAborted() && rs.next()) {
                        ResultType row_value = mapper.map(idx++, rs, getContext());
                        this.ac = folder.fold(ac, row_value, ctl, getContext());
                    }
                    return ac;
                }
            });
        }
        finally {
            cleanup();
        }
    }


    /**
     * Used to execute the query and traverse the result set with a accumulator.
     * <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Folding</a> over the
     * result involves invoking a callback for each row, passing into the callback the return value
     * from the previous function invocation.
     *
     * @param accumulator The initial accumulator value
     * @param folder      Defines the function which will fold over the result set.
     *
     * @return The return value from the last invocation of {@link Folder#fold(Object, java.sql.ResultSet)}
     *
     * @see org.skife.jdbi.v2.Folder
     * @deprecated Use {@link Query#fold(Object, Folder2)}
     */
    public <AccumulatorType> AccumulatorType fold(AccumulatorType accumulator, final Folder<AccumulatorType> folder)
    {
        final AtomicReference<AccumulatorType> acc = new AtomicReference<AccumulatorType>(accumulator);

        try {
            this.internalExecute(new QueryResultSetMunger<Void>(this)
            {
                @Override
                public Void munge(ResultSet rs) throws SQLException
                {
                    while (rs.next()) {
                        acc.set(folder.fold(acc.get(), rs));
                    }
                    return null;
                }
            });
            return acc.get();
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
        return (ResultType) first(UnwrappedSingleValue.class);
    }

    @Override
    public <T> T first(Class<T> containerType)
    {
        addStatementCustomizer(StatementCustomizers.MAX_ROW_ONE);
        ContainerBuilder builder = getContainerMapperRegistry().createBuilderFor(containerType);

        return (T) this.fold(builder, new Folder3<ContainerBuilder, ResultType>()
        {
            @Override
            public ContainerBuilder fold(ContainerBuilder accumulator, ResultType rs, FoldController control, StatementContext ctx) throws SQLException
            {
                accumulator.add(rs);
                control.abort();
                return accumulator;
            }
        }).build();
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
     * @see DBI#registerMapper(org.skife.jdbi.v2.tweak.ResultSetMapper)
     * @see DBI#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(org.skife.jdbi.v2.tweak.ResultSetMapper)
     */
    public <T> Query<T> mapTo(Class<T> resultType)
    {
        return this.map(new RegisteredMapper(resultType, mappingRegistry));
    }

    public <T> Query<T> map(ResultColumnMapper<T> mapper) {
        return this.map(new SingleColumnMapper(mapper));
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
                            mappingRegistry,
                            getForeman().createChild(),
                            getContainerMapperRegistry().createChild());
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

    public void registerMapper(ResultSetMapper m)
    {
        this.mappingRegistry.addMapper(new InferredMapperFactory(m));
    }

    public void registerMapper(ResultSetMapperFactory m)
    {
        this.mappingRegistry.addMapper(m);
    }

    public void registerColumnMapper(ResultColumnMapper m)
    {
        this.mappingRegistry.addColumnMapper(m);
    }

    public void registerColumnMapper(ResultColumnMapperFactory m) {
        this.mappingRegistry.addColumnMapper(m);
    }
}
