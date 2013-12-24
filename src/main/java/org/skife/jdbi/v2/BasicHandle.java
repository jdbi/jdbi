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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCloseResourceException;
import org.skife.jdbi.v2.exceptions.UnableToManipulateTransactionIsolationLevelException;
import org.skife.jdbi.v2.sqlobject.SqlObjectBuilder;
import org.skife.jdbi.v2.tweak.ArgumentFactory;
import org.skife.jdbi.v2.tweak.ContainerFactory;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementCustomizer;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.TransactionHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BasicHandle implements Handle
{

    private StatementRewriter statementRewriter;
    private StatementLocator  statementLocator;
    private SQLLog            log;
    private TimingCollector   timingCollector;
    private StatementBuilder  statementBuilder;

    private boolean closed = false;

    private final Map<String, Object>      globalStatementAttributes;
    private final MappingRegistry          mappingRegistry;
    private final ContainerFactoryRegistry containerFactoryRegistry;
    private final Foreman                  foreman;
    private final TransactionHandler       transactions;
    private final Connection               connection;


    BasicHandle(TransactionHandler transactions,
                StatementLocator statementLocator,
                StatementBuilder preparedStatementCache,
                StatementRewriter statementRewriter,
                Connection connection,
                Map<String, Object> globalStatementAttributes,
                SQLLog log,
                TimingCollector timingCollector,
                MappingRegistry mappingRegistry,
                Foreman foreman,
                ContainerFactoryRegistry containerFactoryRegistry)
    {
        this.statementBuilder = preparedStatementCache;
        this.statementRewriter = statementRewriter;
        this.transactions = transactions;
        this.connection = connection;
        this.statementLocator = statementLocator;
        this.log = log;
        this.timingCollector = timingCollector;
        this.mappingRegistry = mappingRegistry;
        this.foreman = foreman;
        this.globalStatementAttributes = new HashMap<String, Object>();
        this.globalStatementAttributes.putAll(globalStatementAttributes);
        this.containerFactoryRegistry = containerFactoryRegistry.createChild();
    }

    public Query<Map<String, Object>> createQuery(String sql)
    {
        return new Query<Map<String, Object>>(new Binding(),
                                              new DefaultMapper(),
                                              statementLocator,
                                              statementRewriter,
                                              this,
                                              statementBuilder,
                                              sql,
                                              new ConcreteStatementContext(globalStatementAttributes),
                                              log,
                                              timingCollector,
                                              Collections.<StatementCustomizer>emptyList(),
                                              new MappingRegistry(mappingRegistry),
                                              foreman.createChild(),
                                              containerFactoryRegistry.createChild());
    }

    /**
     * Get the JDBC Connection this Handle uses
     *
     * @return the JDBC Connection this Handle uses
     */
    public Connection getConnection()
    {
        return this.connection;
    }

    public void close()
    {
        if (!closed) {
            statementBuilder.close(getConnection());
            try {
                connection.close();
            }
            catch (SQLException e) {
                throw new UnableToCloseResourceException("Unable to close Connection", e);
            }
            finally {
                log.logReleaseHandle(this);
                closed = true;
            }
        }
    }

    boolean isClosed()
    {
        return closed;
    }

    public void define(String key, Object value)
    {
        this.globalStatementAttributes.put(key, value);
    }

    /**
     * Start a transaction
     */
    public Handle begin()
    {
        transactions.begin(this);
        log.logBeginTransaction(this);
        return this;
    }

    /**
     * Commit a transaction
     */
    public Handle commit()
    {
        final long start = System.nanoTime();
        transactions.commit(this);
        log.logCommitTransaction((System.nanoTime() - start) / 1000000L, this);
        return this;
    }

    /**
     * Rollback a transaction
     */
    public Handle rollback()
    {
        final long start = System.nanoTime();
        transactions.rollback(this);
        log.logRollbackTransaction((System.nanoTime() - start) / 1000000L, this);
        return this;
    }

    /**
     * Create a transaction checkpoint (savepoint in JDBC terminology) with the name provided.
     *
     * @param name The name of the checkpoint
     *
     * @return The same handle
     */
    public Handle checkpoint(String name)
    {
        transactions.checkpoint(this, name);
        log.logCheckpointTransaction(this, name);
        return this;
    }

    /**
     * Release the named checkpoint, making rollback to it not possible.
     *
     * @return The same handle
     */
    public Handle release(String checkpointName)
    {
        transactions.release(this, checkpointName);
        log.logReleaseCheckpointTransaction(this, checkpointName);
        return this;
    }

    public void setStatementBuilder(StatementBuilder builder)
    {
        this.statementBuilder = builder;
    }

    public void setSQLLog(SQLLog log)
    {
        this.log = log;
    }

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
    public Handle rollback(String checkpointName)
    {
        final long start = System.nanoTime();
        transactions.rollback(this, checkpointName);
        log.logRollbackToCheckpoint((System.nanoTime() - start) / 1000000L, this, checkpointName);
        return this;
    }

    public boolean isInTransaction()
    {
        return transactions.isInTransaction(this);
    }

    public Update createStatement(String sql)
    {
        return new Update(this,
                          statementLocator,
                          statementRewriter,
                          statementBuilder,
                          sql,
                          new ConcreteStatementContext(globalStatementAttributes),
                          log,
                          timingCollector,
                          foreman,
                          containerFactoryRegistry);
    }

    public Call createCall(String sql)
    {
        return new Call(this,
                        statementLocator,
                        statementRewriter,
                        statementBuilder,
                        sql,
                        new ConcreteStatementContext(globalStatementAttributes),
                        log,
                        timingCollector,
                        Collections.<StatementCustomizer>emptyList(),
                        foreman,
                        containerFactoryRegistry);
    }

    public int insert(String sql, Object... args)
    {
        return update(sql, args);
    }

    public int update(String sql, Object... args)
    {
        Update stmt = createStatement(sql);
        int position = 0;
        for (Object arg : args) {
            stmt.bind(position++, arg);
        }
        return stmt.execute();
    }

    public PreparedBatch prepareBatch(String sql)
    {
        return new PreparedBatch(statementLocator,
                                 statementRewriter,
                                 this,
                                 statementBuilder,
                                 sql,
                                 new ConcreteStatementContext(this.globalStatementAttributes),
                                 log,
                                 timingCollector,
                                 Collections.<StatementCustomizer>emptyList(),
                                 foreman,
                                 containerFactoryRegistry);
    }

    public Batch createBatch()
    {
        return new Batch(this.statementRewriter,
                         this.connection,
                         globalStatementAttributes,
                         log,
                         timingCollector,
                         foreman.createChild());
    }

    public <ReturnType> ReturnType inTransaction(TransactionCallback<ReturnType> callback)
    {
        return transactions.inTransaction(this, callback);
    }

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

    public List<Map<String, Object>> select(String sql, Object... args)
    {
        Query<Map<String, Object>> query = this.createQuery(sql);
        int position = 0;
        for (Object arg : args) {
            query.bind(position++, arg);
        }
        return query.list();
    }

    public void setStatementLocator(StatementLocator locator)
    {
        this.statementLocator = locator;
    }

    public void setStatementRewriter(StatementRewriter rewriter)
    {
        this.statementRewriter = rewriter;
    }

    public Script createScript(String name)
    {
        return new Script(this, statementLocator, name, globalStatementAttributes);
    }

    public void execute(String sql, Object... args)
    {
        this.update(sql, args);
    }

    public void registerMapper(ResultSetMapper mapper)
    {
        mappingRegistry.add(mapper);
    }

    public void registerMapper(ResultSetMapperFactory factory)
    {
        mappingRegistry.add(factory);
    }

    public <SqlObjectType> SqlObjectType attach(Class<SqlObjectType> sqlObjectType)
    {
        return SqlObjectBuilder.attach(this, sqlObjectType);
    }

    public void setTransactionIsolation(TransactionIsolationLevel level)
    {
        setTransactionIsolation(level.intValue());
    }

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

    public TransactionIsolationLevel getTransactionIsolationLevel()
    {
        try {
            return TransactionIsolationLevel.valueOf(connection.getTransactionIsolation());
        }
        catch (SQLException e) {
            throw new UnableToManipulateTransactionIsolationLevelException("unable to access current setting", e);
        }
    }

    public void registerArgumentFactory(ArgumentFactory argumentFactory)
    {
        this.foreman.register(argumentFactory);
    }

    public void registerContainerFactory(ContainerFactory<?> factory)
    {
        this.containerFactoryRegistry.register(factory);
    }
}
