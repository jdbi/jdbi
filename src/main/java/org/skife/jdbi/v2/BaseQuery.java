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


import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.StatementCustomizer;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * Base class for statements providing convenience result handling for SQL queries
 *
 * @param <SelfType> concrete query type
 */
@SuppressWarnings("unchecked")
public class BaseQuery<SelfType extends BaseQuery<SelfType>> extends SQLStatement<SelfType> {

    protected final MappingRegistry mappingRegistry;

    BaseQuery(Binding params,
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
              ContainerFactoryRegistry containerFactoryRegistry) {
        super(params, locator, statementRewriter, handle, cache, sql, ctx, log,
                timingCollector, customizers, foreman, containerFactoryRegistry);
        this.mappingRegistry = new MappingRegistry(mappingRegistry);
    }

    /**
     * Specify the fetch size for the query. This should cause the results to be
     * fetched from the underlying RDBMS in groups of rows equal to the number passed.
     * This is useful for doing chunked streaming of results when exhausting memory
     * could be a problem.
     *
     * @param fetchSize the number of rows to fetch in a bunch
     * @return the modified query
     */
    public SelfType setFetchSize(final int fetchSize) {
        this.addStatementCustomizer(new StatementCustomizers.FetchSizeCustomizer(fetchSize));
        return (SelfType) this;
    }

    /**
     * Specify the maimum number of rows the query is to return. This uses the underlying JDBC
     * {@link java.sql.Statement#setMaxRows(int)}}.
     *
     * @param maxRows maximum number of rows to return
     * @return modified query
     */
    public SelfType setMaxRows(final int maxRows) {
        this.addStatementCustomizer(new StatementCustomizers.MaxRowsCustomizer(maxRows));
        return (SelfType) this;
    }

    /**
     * Specify the maimum field size in the result set. This uses the underlying JDBC
     * {@link java.sql.Statement#setMaxFieldSize(int)}
     *
     * @param maxFields maximum field size
     * @return modified query
     */
    public SelfType setMaxFieldSize(final int maxFields) {
        this.addStatementCustomizer(new StatementCustomizers.MaxFieldSizeCustomizer(maxFields));
        return (SelfType) this;
    }

    /**
     * Specify that the fetch order should be reversed, uses the underlying
     * {@link java.sql.Statement#setFetchDirection(int)}
     *
     * @return the modified query
     */
    public SelfType fetchReverse() {
        setFetchDirection(ResultSet.FETCH_REVERSE);
        return (SelfType) this;
    }

    /**
     * Specify that the fetch order should be forward, uses the underlying
     * {@link java.sql.Statement#setFetchDirection(int)}
     *
     * @return the modified query
     */
    public BaseQuery<SelfType> fetchForward() {
        setFetchDirection(ResultSet.FETCH_FORWARD);
        return this;
    }

    public void registerMapper(ResultSetMapper m) {
        this.mappingRegistry.add(new InferredMapperFactory(m));
    }

    public void registerMapper(ResultSetMapperFactory m) {
        this.mappingRegistry.add(m);
    }
}
