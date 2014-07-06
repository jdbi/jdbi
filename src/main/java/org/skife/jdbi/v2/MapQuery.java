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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap;

/**
 * Statement providing convenience result handling for SQL queries transformed to a {@link java.util.Map}.
 */
public class MapQuery<KeyType, ValueType> extends BaseQuery<MapQuery<KeyType, ValueType>> {

    /**
     * Functional interface for managing a result set iteration cycle
     */
    private interface Condition {
        boolean check(ResultSet rs, int index) throws SQLException;
    }

    private static final Condition GET_ALL = new Condition() {
        @Override
        public boolean check(ResultSet rs, int index) throws SQLException {
            return rs.next();
        }
    };

    private final ResultSetMapper<KeyType> keyMapper;
    private final ResultSetMapper<ValueType> valueMapper;
    private Map<KeyType, ValueType> accumulator = null;

    MapQuery(Binding params,
             ResultSetMapper<KeyType> keyMapper,
             ResultSetMapper<ValueType> valueMapper,
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
        super(params, locator, statementRewriter, handle, cache, sql, ctx, log, timingCollector, customizers,
                mappingRegistry, foreman, containerFactoryRegistry);
        this.valueMapper = valueMapper;
        this.keyMapper = keyMapper;
    }

    /**
     * Executes the select
     * <p/>
     * Will eagerly load all results
     *
     * @throws org.skife.jdbi.v2.exceptions.UnableToCreateStatementException
     *          if there is an error creating the statement
     * @throws org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException
     *          if there is an error executing the statement
     * @throws org.skife.jdbi.v2.exceptions.ResultSetException
     *          if there is an error dealing with the result set
     */
    public Map<KeyType, ValueType> get() {
        return innerGet(GET_ALL);
    }

    /**
     * Executes the select
     * <p/>
     * Will eagerly load all results up to a maximum of <code>maxRows</code>
     *
     * @param maxRows The maximum number of results to include in the result, any
     *                rows in the result set beyond this number will be ignored.
     * @throws org.skife.jdbi.v2.exceptions.UnableToCreateStatementException
     *          if there is an error creating the statement
     * @throws org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException
     *          if there is an error executing the statement
     * @throws org.skife.jdbi.v2.exceptions.ResultSetException
     *          if there is an error dealing with the result set
     */
    public Map<KeyType, ValueType> get(final int maxRows) {
        return innerGet(new Condition() {
            @Override
            public boolean check(ResultSet rs, int index) throws SQLException {
                return rs.next() && index < maxRows;
            }
        });
    }

    private Map<KeyType, ValueType> innerGet(final Condition continueCondition) {
        try {
            return this.internalExecute(new QueryResultSetMunger<Map<KeyType, ValueType>>(this) {
                public Map<KeyType, ValueType> munge(ResultSet rs) throws SQLException {
                    // Traverse a result set and fill the map with keys and values
                    Map<KeyType, ValueType> resultMap =
                            accumulator != null ? accumulator : new HashMap<KeyType, ValueType>();
                    int index = 0;
                    while (continueCondition.check(rs, index)) {
                        KeyType key = keyMapper.map(index, rs, getContext());
                        ValueType value = valueMapper.map(index, rs, getContext());
                        resultMap.put(key, value);
                        index++;
                    }
                    return resultMap;
                }
            });
        } finally {
            cleanup();
        }
    }

    /**
     * Provide basic JavaBean mapping capabilities for keys. Will instantiate an instance of keyType
     * for each row and set the JavaBean properties which match fields in the result set.
     *
     * @param keyType JavaBean class to map result set fields into the properties
     * @return a Query which provides the bean property mapping for keys
     */
    public <T> MapQuery<T, ValueType> mapKey(Class<T> keyType) {
        return this.mapKey(new BeanMapper<T>(keyType));
    }

    /**
     * Makes use of registered mappers to map the result set to the desired key type.
     *
     * @param keyType the key type of map the query results to
     * @return a new query instance which will map keys to the desired type
     * @see DBI#registerMapper(org.skife.jdbi.v2.tweak.ResultSetMapper)
     * @see DBI#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(org.skife.jdbi.v2.tweak.ResultSetMapper)
     */
    public <T> MapQuery<T, ValueType> mapKeyTo(Class<T> keyType) {
        return this.mapKey(new RegisteredMapper<T>(keyType, mappingRegistry));
    }

    /**
     * Provide basic JavaBean mapping capabilities for values. Will instantiate an instance of valueType
     * for each row and set the JavaBean properties which match fields in the result set.
     *
     * @param valueType JavaBean class to map result set fields into the properties
     * @return a Query which provides the bean property mapping for values
     */
    public <T> MapQuery<KeyType, T> mapValue(Class<T> valueType) {
        return this.mapValue(new BeanMapper<T>(valueType));
    }

    /**
     * Makes use of registered mappers to map the result set to the desired value type.
     *
     * @param valueType the value type of map the query results to
     * @return a new query instance which will map values to the desired type
     * @see DBI#registerMapper(org.skife.jdbi.v2.tweak.ResultSetMapper)
     * @see DBI#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(ResultSetMapperFactory)
     * @see Handle#registerMapper(org.skife.jdbi.v2.tweak.ResultSetMapper)
     */
    public <T> MapQuery<KeyType, T> mapValueTo(Class<T> valueType) {
        return this.mapValue(new RegisteredMapper<T>(valueType, mappingRegistry));
    }


    /**
     * Makes use of the mapper to map the result set to the desired key type.
     *
     * @param keyMapper the mapper to keys of map the query results to
     * @return a new query instance which will map keys to the desired type
     */
    public <T> MapQuery<T, ValueType> mapKey(ResultSetMapper<T> keyMapper) {
        return new MapQuery<T, ValueType>(getParameters(),
                keyMapper,
                valueMapper,
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
                getForeman().createChild(),
                getContainerMapperRegistry().createChild());
    }

    /**
     * Makes use of the mapper to map the result set to the desired value type.
     *
     * @param valueMapper the mapper to values of map the query results to
     * @return a new query instance which will map values to the desired type
     */
    public <V> MapQuery<KeyType, V> mapValue(ResultSetMapper<V> valueMapper) {
        return new MapQuery<KeyType, V>(getParameters(),
                keyMapper,
                valueMapper,
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
                getForeman().createChild(),
                getContainerMapperRegistry().createChild());
    }

    /**
     * Obtain a forward-only result set iterator. Note that you must explicitely close
     * the iterator to close the underlying resources.
     */
    public ResultIterator<Map.Entry<KeyType, ValueType>> iterator() {
        return this.internalExecute(new QueryResultMunger<ResultIterator<Map.Entry<KeyType, ValueType>>>() {
            public ResultIterator<Map.Entry<KeyType, ValueType>> munge(Statement stmt) throws SQLException {
                return new ResultSetResultIterator<Map.Entry<KeyType, ValueType>>(new ResultSetMapper<Map.Entry<KeyType, ValueType>>() {
                    @Override
                    public Map.Entry<KeyType, ValueType> map(int index, ResultSet r, StatementContext ctx) throws SQLException {
                        return new AbstractMap.SimpleEntry<KeyType, ValueType>(keyMapper.map(index, r, ctx),
                                valueMapper.map(index, r, ctx));
                    }
                }, MapQuery.this, stmt, getContext());
            }
        });
    }

    /**
     * Used to execute the query and traverse the result with gathering data to the accumulator.
     * The link to the accumulator doesn't change. So the return object is the same object as passed as an argument,
     * but with a changed state. Callback is used to change a state of the accumulator.
     *
     * @param accumulator Empty accumulator object
     * @param folder      Defines the function which will fold over the result set.
     * @return The return value from the last invocation of {@link MapFolder#fold(Object, Object, Object)}}
     */
    public <AccumulatorType> AccumulatorType fold(final AccumulatorType accumulator,
                                                  final MapFolder<AccumulatorType, KeyType, ValueType> folder) {
        try {
            this.internalExecute(new QueryResultSetMunger<Void>(this) {
                public Void munge(ResultSet rs) throws SQLException {
                    int index = 0;
                    while (rs.next()) {
                        folder.fold(accumulator, keyMapper.map(index, rs, getContext()),
                                valueMapper.map(index, rs, getContext()));
                        index++;
                    }
                    return null;
                }
            });
            return accumulator;
        } finally {
            cleanup();
        }

    }

    /**
     * Accumulator (empty {@link java.util.Map} implementation) to which keys and values would be placed.
     * If it's not specified, it would be a plain {@link java.util.HashMap}
     * <p/>
     * This method should be called after key and value mappers are set, otherwise the accumulator will be overridden
     * by another builder because type safety reasons.
     *
     * @param accumulator Empty accumulator map
     * @return The same builder
     */
    public MapQuery<KeyType, ValueType> accumulator(Map<KeyType, ValueType> accumulator) {
        this.accumulator = accumulator;
        return this;
    }
}
