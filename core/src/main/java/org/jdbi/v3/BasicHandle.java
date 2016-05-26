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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jdbi.v3.exceptions.UnableToCloseResourceException;
import org.jdbi.v3.exceptions.UnableToManipulateTransactionIsolationLevelException;
import org.jdbi.v3.extension.ExtensionConfig;
import org.jdbi.v3.extension.ExtensionFactory;
import org.jdbi.v3.extension.NoSuchExtensionException;
import org.jdbi.v3.rewriter.StatementRewriter;
import org.jdbi.v3.tweak.ArgumentFactory;
import org.jdbi.v3.tweak.CollectorFactory;
import org.jdbi.v3.tweak.ColumnMapper;
import org.jdbi.v3.tweak.RowMapper;
import org.jdbi.v3.tweak.StatementBuilder;
import org.jdbi.v3.tweak.StatementCustomizer;
import org.jdbi.v3.tweak.StatementLocator;
import org.jdbi.v3.tweak.TransactionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BasicHandle implements Handle
{
    private static final Logger LOG = LoggerFactory.getLogger(BasicHandle.class);

    private final JdbiConfig config;

    private StatementBuilder statementBuilder;

    private boolean closed = false;

    private final TransactionHandler       transactions;
    private final Connection               connection;


    BasicHandle(JdbiConfig config,
                TransactionHandler transactions,
                StatementBuilder preparedStatementCache,
                Connection connection)
    {
        this.config = config;
        this.statementBuilder = preparedStatementCache;
        this.transactions = transactions;
        this.connection = connection;
    }

    @Override
    public Query<Map<String, Object>> createQuery(String sql)
    {
        JdbiConfig queryConfig = JdbiConfig.copyOf(config);
        return new Query<>(queryConfig,
                new Binding(),
                new DefaultMapper(),
                this,
                statementBuilder,
                sql,
                new StatementContext(queryConfig),
                Collections.<StatementCustomizer>emptyList());
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
        config.statementAttributes.put(key, value);
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
            config.timingCollector = TimingCollector.NOP_TIMING_COLLECTOR;
        }
        else {
            config.timingCollector = timingCollector;
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
        JdbiConfig updateConfig = JdbiConfig.copyOf(config);
        return new Update(updateConfig,
                          this,
                          statementBuilder,
                          sql,
                          new StatementContext(updateConfig));
    }

    @Override
    public Call createCall(String sql)
    {
        JdbiConfig callConfig = JdbiConfig.copyOf(config);
        return new Call(callConfig,
                        this,
                        statementBuilder,
                        sql,
                        new StatementContext(callConfig),
                        Collections.<StatementCustomizer>emptyList());
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
        JdbiConfig batchConfig = JdbiConfig.copyOf(config);
        return new PreparedBatch(batchConfig,
                                 this,
                                 statementBuilder,
                                 sql,
                                 new StatementContext(batchConfig),
                                 Collections.<StatementCustomizer>emptyList());
    }

    @Override
    public Batch createBatch()
    {
        JdbiConfig batchConfig = JdbiConfig.copyOf(config);
        return new Batch(batchConfig,
                         this.connection,
                         new StatementContext(batchConfig));
    }

    @Override
    public <R, X extends Exception> R inTransaction(TransactionCallback<R, X> callback) throws X
    {
        return transactions.inTransaction(this, callback);
    }

    @Override
    public <X extends Exception> void useTransaction(final TransactionConsumer<X> callback) throws X
    {
        transactions.inTransaction(this, (handle, status) -> {
            callback.useTransaction(handle, status);
            return null;
        });
    }

    @Override
    public <R, X extends Exception> R inTransaction(TransactionIsolationLevel level,
                                                    TransactionCallback<R, X> callback) throws X
    {
        final TransactionIsolationLevel initial = getTransactionIsolationLevel();
        boolean failed = true;
        try {
            setTransactionIsolation(level);

            R result = transactions.inTransaction(this, level, callback);
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
    public <X extends Exception> void useTransaction(TransactionIsolationLevel level,
                                                     TransactionConsumer<X> callback) throws X
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
        config.statementLocator = locator;
    }

    @Override
    public void setStatementRewriter(StatementRewriter rewriter)
    {
        config.statementRewriter = rewriter;
    }

    @Override
    public Script createScript(String name)
    {
        JdbiConfig scriptConfig = JdbiConfig.copyOf(config);
        return new Script(scriptConfig, this, name, new StatementContext(scriptConfig));
    }

    @Override
    public void execute(String sql, Object... args)
    {
        this.update(sql, args);
    }

    @Override
    public void registerRowMapper(RowMapper<?> mapper)
    {
        config.mappingRegistry.addRowMapper(mapper);
    }

    @Override
    public void registerRowMapper(RowMapperFactory factory)
    {
        config.mappingRegistry.addRowMapper(factory);
    }

    @Override
    public void registerColumnMapper(ColumnMapper<?> mapper) {
        config.mappingRegistry.addColumnMapper(mapper);
    }

    @Override
    public void registerColumnMapper(ColumnMapperFactory factory) {
        config.mappingRegistry.addColumnMapper(factory);
    }

    @Override
    public <T> T attach(Class<T> extensionType)
    {
        return (T) config.extensionRegistry.findExtensionFor(extensionType, () -> this)
                .orElseThrow(() -> new NoSuchExtensionException("Extension not found: " + extensionType));
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
        config.argumentRegistry.register(argumentFactory);
    }

    @Override
    public void registerCollectorFactory(CollectorFactory factory) {
        config.collectorRegistry.register(factory);
    }

    @Override
    public void registerExtension(ExtensionFactory factory) {
        config.extensionRegistry.register(factory);
    }

    @Override
    public <C extends ExtensionConfig<C>> void configureExtension(Class<C> configClass, Consumer<C> consumer) {
        config.extensionRegistry.configure(configClass, consumer);
    }
}
