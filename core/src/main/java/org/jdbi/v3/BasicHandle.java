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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.exceptions.UnableToCloseResourceException;
import org.jdbi.v3.exceptions.UnableToManipulateTransactionIsolationLevelException;
import org.jdbi.v3.tweak.ArgumentFactory;
import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.tweak.ResultSetMapper;
import org.jdbi.v3.tweak.StatementBuilder;
import org.jdbi.v3.tweak.StatementCustomizer;
import org.jdbi.v3.tweak.StatementLocator;
import org.jdbi.v3.tweak.StatementRewriter;
import org.jdbi.v3.tweak.TransactionHandler;
import org.jdbi.v3.tweak.CollectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BasicHandle implements Handle
{
    private static final Logger LOG = LoggerFactory.getLogger(BasicHandle.class);

    private StatementRewriter statementRewriter;
    private StatementLocator  statementLocator;
    private TimingCollector   timingCollector;
    private StatementBuilder  statementBuilder;

    private boolean closed = false;

    private final Map<String, Object>      globalStatementAttributes;
    private final MappingRegistry          mappingRegistry;
    private final CollectorFactoryRegistry collectorFactoryRegistry;
    private final ArgumentRegistry argumentRegistry;
    private final TransactionHandler       transactions;
    private final Connection               connection;


    BasicHandle(TransactionHandler transactions,
                StatementLocator statementLocator,
                StatementBuilder preparedStatementCache,
                StatementRewriter statementRewriter,
                Connection connection,
                Map<String, Object> globalStatementAttributes,
                TimingCollector timingCollector,
                MappingRegistry mappingRegistry,
                ArgumentRegistry argumentRegistry,
                CollectorFactoryRegistry collectorFactoryRegistry)
    {
        this.statementBuilder = preparedStatementCache;
        this.statementRewriter = statementRewriter;
        this.transactions = transactions;
        this.connection = connection;
        this.statementLocator = statementLocator;
        this.timingCollector = timingCollector;
        this.mappingRegistry = mappingRegistry;
        this.argumentRegistry = argumentRegistry;
        this.globalStatementAttributes = new HashMap<>();
        this.globalStatementAttributes.putAll(globalStatementAttributes);
        this.collectorFactoryRegistry = CollectorFactoryRegistry.copyOf(collectorFactoryRegistry);
    }

    @Override
    public Query<Map<String, Object>> createQuery(String sql)
    {
        MappingRegistry queryRegistry = MappingRegistry.copyOf(this.mappingRegistry);
        ArgumentRegistry queryArgumentRegistry = ArgumentRegistry.copyOf(argumentRegistry);
        CollectorFactoryRegistry queryCollectors = CollectorFactoryRegistry.copyOf(collectorFactoryRegistry);
        return new Query<>(
                new Binding(),
                new DefaultMapper(),
                statementLocator,
                statementRewriter,
                this,
                statementBuilder,
                sql,
                new ConcreteStatementContext(globalStatementAttributes, queryRegistry, queryArgumentRegistry, queryCollectors),
                timingCollector,
                Collections.<StatementCustomizer>emptyList(),
                queryRegistry,
                queryArgumentRegistry,
                queryCollectors);
    }

    /**
     * Get the JDBC Connection this Handle uses
     *
     * @return the JDBC Connection this Handle uses
     */
    @Override
    public Connection getConnection()
    {
        return this.connection;
    }

    @Override
    public void close()
    {
        if (!closed) {
            try {
                statementBuilder.close(getConnection());
            } finally {
                try {
                    connection.close();
                }
                catch (SQLException e) {
                    throw new UnableToCloseResourceException("Unable to close Connection", e);
                } finally {
                    LOG.trace("Handle [{}] released", this);
                    closed = true;
                }
            }
        }
    }

    boolean isClosed()
    {
        return closed;
    }

    @Override
    public void define(String key, Object value)
    {
        this.globalStatementAttributes.put(key, value);
    }

    /**
     * Start a transaction
     */
    @Override
    public Handle begin()
    {
        transactions.begin(this);
        LOG.trace("Handle [{}] begin transaction", this);
        return this;
    }

    /**
     * Commit a transaction
     */
    @Override
    public Handle commit()
    {
        final long start = System.nanoTime();
        transactions.commit(this);
        LOG.trace("Handle [{}] commit transaction in {}ms", this, (System.nanoTime() - start) / 1000000L);
        return this;
    }

    /**
     * Rollback a transaction
     */
    @Override
    public Handle rollback()
    {
        final long start = System.nanoTime();
        transactions.rollback(this);
        LOG.trace("Handle [{}] rollback transaction in {}ms", this, ((System.nanoTime() - start) / 1000000L));
        return this;
    }

    /**
     * Create a transaction checkpoint (savepoint in JDBC terminology) with the name provided.
     *
     * @param name The name of the checkpoint
     *
     * @return The same handle
     */
    @Override
    public Handle checkpoint(String name)
    {
        transactions.checkpoint(this, name);
        LOG.trace("Handle [{}] checkpoint \"{}\"", this, name);
        return this;
    }

    /**
     * Release the named checkpoint, making rollback to it not possible.
     *
     * @return The same handle
     */
    @Override
    public Handle release(String checkpointName)
    {
        transactions.release(this, checkpointName);
        LOG.trace("Handle [{}] release checkpoint \"{}\"", this, checkpointName);
        return this;
    }

    @Override
    public void setStatementBuilder(StatementBuilder builder)
    {
        this.statementBuilder = builder;
    }

    @Override
    public void setTimingCollector(final TimingCollector timingCollector)
    {
        if (timingCollector == null) {
            this.timingCollector = TimingCollector.NOP_TIMING_COLLECTOR;
        }
        else {
            this.timingCollector = timingCollector;
        }
    }


    /**
     * Rollback a transaction to a named checkpoint
     *
     * @param checkpointName the name of the checkpoint, previously declared with {@see Handle#checkpoint}
     */
    @Override
    public Handle rollback(String checkpointName)
    {
        final long start = System.nanoTime();
        transactions.rollback(this, checkpointName);
        LOG.trace("Handle [{}] rollback to checkpoint \"{}\" in {}ms", this, checkpointName, ((System.nanoTime() - start) / 1000000L));
        return this;
    }

    @Override
    public boolean isInTransaction()
    {
        return transactions.isInTransaction(this);
    }

    @Override
    public Update createStatement(String sql)
    {
        ArgumentRegistry updateArgumentRegistry = ArgumentRegistry.copyOf(argumentRegistry);
        MappingRegistry updateMappingRegistry = MappingRegistry.copyOf(this.mappingRegistry);
        CollectorFactoryRegistry updateCollectors = CollectorFactoryRegistry.copyOf(collectorFactoryRegistry);
        return new Update(this,
                          statementLocator,
                          statementRewriter,
                          statementBuilder,
                          sql,
                          new ConcreteStatementContext(globalStatementAttributes, updateMappingRegistry, updateArgumentRegistry, updateCollectors),
                          timingCollector,
                          updateArgumentRegistry,
                          updateMappingRegistry,
                          updateCollectors);
    }

    @Override
    public Call createCall(String sql)
    {
        ArgumentRegistry callArgumentRegistry = ArgumentRegistry.copyOf(argumentRegistry);
        CollectorFactoryRegistry callCollectors = CollectorFactoryRegistry.copyOf(collectorFactoryRegistry);
        return new Call(this,
                        statementLocator,
                        statementRewriter,
                        statementBuilder,
                        sql,
                        new ConcreteStatementContext(globalStatementAttributes, MappingRegistry.copyOf(mappingRegistry), callArgumentRegistry, callCollectors),
                        timingCollector,
                        Collections.<StatementCustomizer>emptyList(),
                        callArgumentRegistry,
                        callCollectors);
    }

    @Override
    public int insert(String sql, Object... args)
    {
        return update(sql, args);
    }

    @Override
    public int update(String sql, Object... args)
    {
        Update stmt = createStatement(sql);
        int position = 0;
        for (Object arg : args) {
            stmt.bind(position++, arg);
        }
        return stmt.execute();
    }

    @Override
    public PreparedBatch prepareBatch(String sql)
    {
        ArgumentRegistry batchArgumentRegistry = ArgumentRegistry.copyOf(argumentRegistry);
        MappingRegistry batchMappingRegistry = MappingRegistry.copyOf(mappingRegistry);
        CollectorFactoryRegistry batchCollectors = CollectorFactoryRegistry.copyOf(collectorFactoryRegistry);
        return new PreparedBatch(statementLocator,
                                 statementRewriter,
                                 this,
                                 statementBuilder,
                                 sql,
                                 new ConcreteStatementContext(globalStatementAttributes, batchMappingRegistry, batchArgumentRegistry, batchCollectors),
                                 timingCollector,
                                 Collections.<StatementCustomizer>emptyList(),
                                 batchArgumentRegistry,
                                 batchMappingRegistry,
                                 batchCollectors);
    }

    @Override
    public Batch createBatch()
    {
        ArgumentRegistry batchArgumentRegistry = ArgumentRegistry.copyOf(argumentRegistry);
        return new Batch(this.statementRewriter,
                         this.connection,
                         new ConcreteStatementContext(globalStatementAttributes, MappingRegistry.copyOf(mappingRegistry), batchArgumentRegistry, CollectorFactoryRegistry.copyOf(collectorFactoryRegistry)),
                         timingCollector,
                         batchArgumentRegistry);
    }

    @Override
    public <ReturnType> ReturnType inTransaction(TransactionCallback<ReturnType> callback)
    {
        return transactions.inTransaction(this, callback);
    }

    @Override
    public void useTransaction(final TransactionConsumer callback)
    {
        transactions.inTransaction(this, (handle, status) -> {
            callback.useTransaction(handle, status);
            return null;
        });
    }

    @Override
    public <ReturnType> ReturnType inTransaction(TransactionIsolationLevel level,
                                                 TransactionCallback<ReturnType> callback)
    {
        final TransactionIsolationLevel initial = getTransactionIsolationLevel();
        boolean failed = true;
        try {
            setTransactionIsolation(level);

            ReturnType result = transactions.inTransaction(this, level, callback);
            failed = false;

            return result;
        }
        finally {
            try {
                setTransactionIsolation(initial);
            }
            catch (RuntimeException e) {
                if (! failed) {
                    throw e;
                }

                // Ignore, there was already an exceptional condition and we don't want to clobber it.
            }
        }
    }

    @Override
    public void useTransaction(TransactionIsolationLevel level, final TransactionConsumer callback)
    {
        inTransaction(level, (handle, status) -> {
            callback.useTransaction(handle, status);
            return null;
        });
    }

    @Override
    public List<Map<String, Object>> select(String sql, Object... args)
    {
        Query<Map<String, Object>> query = this.createQuery(sql);
        int position = 0;
        for (Object arg : args) {
            query.bind(position++, arg);
        }
        return query.list();
    }

    @Override
    public void setStatementLocator(StatementLocator locator)
    {
        this.statementLocator = locator;
    }

    @Override
    public void setStatementRewriter(StatementRewriter rewriter)
    {
        this.statementRewriter = rewriter;
    }

    @Override
    public Script createScript(String name)
    {
        return new Script(this, statementLocator, name,
                new ConcreteStatementContext(
                        globalStatementAttributes,
                        MappingRegistry.copyOf(mappingRegistry),
                        ArgumentRegistry.copyOf(argumentRegistry),
                        CollectorFactoryRegistry.copyOf(collectorFactoryRegistry)));
    }

    @Override
    public void execute(String sql, Object... args)
    {
        this.update(sql, args);
    }

    @Override
    public void registerMapper(ResultSetMapper<?> mapper)
    {
        mappingRegistry.addMapper(mapper);
    }

    @Override
    public void registerMapper(ResultSetMapperFactory factory)
    {
        mappingRegistry.addMapper(factory);
    }

    @Override
    public void registerColumnMapper(ResultColumnMapper<?> mapper) {
        mappingRegistry.addColumnMapper(mapper);
    }

    @Override
    public void registerColumnMapper(ResultColumnMapperFactory factory) {
        mappingRegistry.addColumnMapper(factory);
    }

    @Override
    public <SqlObjectType> SqlObjectType attach(Class<SqlObjectType> sqlObjectType)
    {
        return SqlObjectBuilderBridge.attach(this, sqlObjectType);
    }

    @Override
    public void setTransactionIsolation(TransactionIsolationLevel level)
    {
        setTransactionIsolation(level.intValue());
    }

    @Override
    public void setTransactionIsolation(int level)
    {
        try {
            if (connection.getTransactionIsolation() == level) {
                // already set, noop
                return;
            }
            connection.setTransactionIsolation(level);
        }
        catch (SQLException e) {
            throw new UnableToManipulateTransactionIsolationLevelException(level, e);
        }
    }

    @Override
    public TransactionIsolationLevel getTransactionIsolationLevel()
    {
        try {
            return TransactionIsolationLevel.valueOf(connection.getTransactionIsolation());
        }
        catch (SQLException e) {
            throw new UnableToManipulateTransactionIsolationLevelException("unable to access current setting", e);
        }
    }

    @Override
    public void registerArgumentFactory(ArgumentFactory argumentFactory)
    {
        this.argumentRegistry.register(argumentFactory);
    }

    @Override
    public void registerCollectorFactory(CollectorFactory factory) {
        this.collectorFactoryRegistry.register(factory);
    }
}
