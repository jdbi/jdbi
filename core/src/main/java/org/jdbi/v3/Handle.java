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

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jdbi.v3.argument.ArgumentFactory;
import org.jdbi.v3.collector.CollectorFactory;
import org.jdbi.v3.exception.UnableToCloseResourceException;
import org.jdbi.v3.exception.UnableToManipulateTransactionIsolationLevelException;
import org.jdbi.v3.extension.ExtensionConfig;
import org.jdbi.v3.extension.ExtensionFactory;
import org.jdbi.v3.extension.NoSuchExtensionException;
import org.jdbi.v3.mapper.ColumnMapper;
import org.jdbi.v3.mapper.ColumnMapperFactory;
import org.jdbi.v3.mapper.DefaultMapper;
import org.jdbi.v3.mapper.RowMapper;
import org.jdbi.v3.mapper.RowMapperFactory;
import org.jdbi.v3.rewriter.StatementRewriter;
import org.jdbi.v3.statement.StatementBuilder;
import org.jdbi.v3.statement.StatementCustomizer;
import org.jdbi.v3.statement.StatementLocator;
import org.jdbi.v3.transaction.TransactionCallback;
import org.jdbi.v3.transaction.TransactionConsumer;
import org.jdbi.v3.transaction.TransactionHandler;
import org.jdbi.v3.transaction.TransactionIsolationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This represents a connection to the database system. It usually is a wrapper around
 * a JDBC Connection object.
 */
public class Handle implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(Handle.class);

    protected final JdbiConfig config;
    protected StatementBuilder statementBuilder;
    private boolean closed = false;
    protected final TransactionHandler transactions;
    protected final Connection connection;

    Handle(JdbiConfig config,
            TransactionHandler transactions,
            StatementBuilder preparedStatementCache,
            Connection connection)
    {
        this.config = config;
        this.statementBuilder = preparedStatementCache;
        this.transactions = transactions;
        this.connection = connection;
    }

    /**
     * Get the JDBC Connection this Handle uses.
     *
     * @return the JDBC Connection this Handle uses
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * Define a statement attribute which will be applied to all {@link StatementContext}
     * instances for statements created from this handle.
     *
     * @param key Attribute name
     * @param value Attribute value
     */
    public Handle define(String key, Object value) {
        config.statementAttributes.put(key, value);
        return this;
    }

    public Handle registerArgumentFactory(ArgumentFactory argumentFactory) {
        config.argumentRegistry.register(argumentFactory);
        return this;
    }

    public Handle registerCollectorFactory(CollectorFactory factory) {
        config.collectorRegistry.register(factory);
        return this;
    }

    /**
     * Register a column mapper which will have its parameterized type inspected to determine what it maps to.
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * The parameter must be concretely parameterized, we use the type argument T to
     * determine if it applies to a given type.
     *
     * @param mapper the column mapper
     * @throws UnsupportedOperationException if the ColumnMapper is not a concretely parameterized type
     */
    public Handle registerColumnMapper(ColumnMapper<?> mapper) {
        config.mappingRegistry.addColumnMapper(mapper);
        return this;
    }

    /**
     * Register a column mapper factory.
     *
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the column mapper factory
     */
    public Handle registerColumnMapper(ColumnMapperFactory factory) {
        config.mappingRegistry.addColumnMapper(factory);
        return this;
    }

    public Handle registerExtension(ExtensionFactory<?> factory) {
        config.extensionRegistry.register(factory);
        return this;
    }

    /**
     * Register a row mapper which will have its parameterized type inspected to determine what it maps to.
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     *
     * The parameter must be concretely parameterized, we use the type argument T to
     * determine if it applies to a given type.
     *
     * @param mapper the row mapper
     * @throws UnsupportedOperationException if the RowMapper is not a concretely parameterized type
     */
    public Handle registerRowMapper(RowMapper<?> mapper) {
        config.mappingRegistry.addRowMapper(mapper);
        return this;
    }

    /**
     * Register a row mapper factory.
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     *
     * @param factory the row mapper factory
     */
    public Handle registerRowMapper(RowMapperFactory factory) {
        config.mappingRegistry.addRowMapper(factory);
        return this;
    }

    /**
     * Specify the statement builder to use for this handle.
     * @param builder StatementBuilder to be used
     */
    public Handle setStatementBuilder(StatementBuilder builder) {
        this.statementBuilder = builder;
        return this;
    }

    /**
     * Allows for overriding the default statement locator. The default searches the
     * classpath for named statements
     *
     * @param locator the statement locator
     */
    public Handle setStatementLocator(StatementLocator locator) {
        config.statementLocator = locator;
        return this;
    }

    /**
     * Allows for overiding the default statement rewriter. The default handles
     * named parameter interpolation.
     *
     * @param rewriter the statement rewriter.
     */
    public Handle setStatementRewriter(StatementRewriter rewriter) {
        config.statementRewriter = rewriter;
        return this;
    }

    /**
     * Specify the class used to collect timing information. The default is inherited from the DBI used
     * to create this Handle.
     *
     * @param timingCollector the timing collector
     */
    public Handle setTimingCollector(final TimingCollector timingCollector) {
        if (timingCollector == null) {
            config.timingCollector = TimingCollector.NOP_TIMING_COLLECTOR;
        }
        else {
            config.timingCollector = timingCollector;
        }
        return this;
    }

    /**
     * Closes the handle, its connection, and any other database resources it is holding.
     *
     * @throws org.jdbi.v3.exception.UnableToCloseResourceException if any resources throw exception while closing
     */
    @Override
    public void close() {
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

    protected boolean isClosed() {
        return closed;
    }

    /**
     * Execute some SQL with no return value
     * @param sql the sql to execute
     * @param args arguments to bind to the sql
     */
    public void execute(String sql, Object... args) {
        this.update(sql, args);
    }

    /**
     * Execute a simple insert statement.
     *
     * @param sql the insert SQL
     * @param args positional arguments
     *
     * @return the number of rows inserted
     */
    public int insert(String sql, Object... args) {
        return update(sql, args);
    }

    /**
     * Convenience method which executes a select with purely positional arguments
     * @param sql SQL or named statement
     * @param args arguments to bind positionally
     * @return results of the query
     */
    public List<Map<String, Object>> select(String sql, Object... args) {
        Query<Map<String, Object>> query = this.createQuery(sql);
        int position = 0;
        for (Object arg : args) {
            query.bind(position++, arg);
        }
        return query.list();
    }

    /**
     * Execute a simple update statement
     *
     * @param sql the update SQL
     * @param args positional arguments
     *
     * @return the number of updated inserted
     */
    public int update(String sql, Object... args) {
        Update stmt = createStatement(sql);
        int position = 0;
        for (Object arg : args) {
            stmt.bind(position++, arg);
        }
        return stmt.execute();
    }

    /**
     * Create a non-prepared (no bound parameters, but different SQL) batch statement.
     * @return empty batch
     * @see Handle#prepareBatch(String)
     */
    public Batch createBatch() {
        JdbiConfig batchConfig = JdbiConfig.copyOf(config);
        return new Batch(batchConfig,
                         this.connection,
                         new StatementContext(batchConfig));
    }

    /**
     * Prepare a batch to execute. This is for efficiently executing more than one
     * of the same statements with different parameters bound.
     *
     * @param sql the batch SQL
     * @return a batch which can have "statements" added
     */
    public PreparedBatch prepareBatch(String sql) {
        JdbiConfig batchConfig = JdbiConfig.copyOf(config);
        return new PreparedBatch(batchConfig,
                                 this,
                                 statementBuilder,
                                 sql,
                                 new StatementContext(batchConfig),
                                 Collections.<StatementCustomizer>emptyList());
    }

    /**
     * Create a call to a stored procedure
     *
     * @param sql the stored procedure sql
     *
     * @return the Call
     */
    public Call createCall(String sql) {
        JdbiConfig callConfig = JdbiConfig.copyOf(config);
        return new Call(callConfig,
                        this,
                        statementBuilder,
                        sql,
                        new StatementContext(callConfig),
                        Collections.<StatementCustomizer>emptyList());
    }

    /**
     * Return a default Query instance which can be executed later, as long as this handle remains open.
     * @param sql the select sql
     *
     * @return the Query
     */
    public Query<Map<String, Object>> createQuery(String sql) {
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
     * Creates an SQL script, looking for the source of the script using the
     * current statement locator (which defaults to searching the classpath).
     *
     * @param name the script name (passed to the statement locator)
     *
     * @return the created Script.
     */
    public Script createScript(String name) {
        JdbiConfig scriptConfig = JdbiConfig.copyOf(config);
        return new Script(scriptConfig, this, name, new StatementContext(scriptConfig));
    }

    /**
     * Create an Insert or Update statement which returns the number of rows modified.
     *
     * @param sql The statement sql
     *
     * @return the Update
     */
    public Update createStatement(String sql) {
        JdbiConfig updateConfig = JdbiConfig.copyOf(config);
        return new Update(updateConfig,
                          this,
                          statementBuilder,
                          sql,
                          new StatementContext(updateConfig));
    }

    /**
     * @return whether the handle is in a transaction. Delegates to the underlying
     *         {@link org.jdbi.v3.transaction.TransactionHandler}.
     */
    public boolean isInTransaction() {
        return transactions.isInTransaction(this);
    }

    /**
     * Start a transaction.
     *
     * @return the same handle
     */
    public Handle begin() {
        transactions.begin(this);
        LOG.trace("Handle [{}] begin transaction", this);
        return this;
    }

    /**
     * Commit a transaction.
     *
     * @return the same handle
     */
    public Handle commit() {
        final long start = System.nanoTime();
        transactions.commit(this);
        LOG.trace("Handle [{}] commit transaction in {}ms", this, (System.nanoTime() - start) / 1000000L);
        return this;
    }

    /**
     * Rollback a transaction.
     *
     * @return the same handle
     */
    public Handle rollback() {
        final long start = System.nanoTime();
        transactions.rollback(this);
        LOG.trace("Handle [{}] rollback transaction in {}ms", this, ((System.nanoTime() - start) / 1000000L));
        return this;
    }

    /**
     * Rollback a transaction to a named checkpoint.
     *
     * @param checkpointName the name of the checkpoint, previously declared with {@link Handle#checkpoint}
     *
     * @return the same handle
     */
    public Handle rollback(String checkpointName) {
        final long start = System.nanoTime();
        transactions.rollback(this, checkpointName);
        LOG.trace("Handle [{}] rollback to checkpoint \"{}\" in {}ms", this, checkpointName, ((System.nanoTime() - start) / 1000000L));
        return this;
    }

    /**
     * Create a transaction checkpoint (savepoint in JDBC terminology) with the name provided.
     *
     * @param name The name of the checkpoint
     * @return The same handle
     */
    public Handle checkpoint(String name) {
        transactions.checkpoint(this, name);
        LOG.trace("Handle [{}] checkpoint \"{}\"", this, name);
        return this;
    }

    /**
     * Release a previously created checkpoint / savepoint.
     *
     * @param checkpointName the name of the checkpoint to release
     *
     * @return the same handle
     */
    public Handle release(String checkpointName) {
        transactions.release(this, checkpointName);
        LOG.trace("Handle [{}] release checkpoint \"{}\"", this, checkpointName);
        return this;
    }

    /**
     * Executes <code>callback</code> in a transaction, and returns the result of the callback.
     *
     * @param callback a callback which will receive an open handle, in a transaction.
     * @param <R> type returned by callback
     * @param <X> exception type thrown by the callback, if any
     *
     * @return value returned from the callback
     *
     * @throws X any exception thrown by the callback
     */
    public <R, X extends Exception> R inTransaction(TransactionCallback<R, X> callback) throws X {
        return transactions.inTransaction(this, callback);
    }

    /**
     * Executes <code>callback</code> in a transaction.
     *
     * @param callback a callback which will receive an open handle, in a transaction.
     * @param <X> exception type thrown by the callback, if any
     *
     * @throws X any exception thrown by the callback
     */
    public <X extends Exception> void useTransaction(final TransactionConsumer<X> callback) throws X {
        transactions.inTransaction(this, (handle, status) -> {
            callback.useTransaction(handle, status);
            return null;
        });
    }

    /**
     * Executes <code>callback</code> in a transaction, and returns the result of the callback.
     *
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     * @param level the transaction isolation level which will be applied to the connection for the scope of this
     *              transaction, after which the original isolation level will be restored.
     * @param callback a callback which will receive an open handle, in a transaction.
     * @param <R> type returned by callback
     * @param <X> exception type thrown by the callback, if any
     *
     * @return value returned from the callback
     *
     * @throws X any exception thrown by the callback
     */
    public <R, X extends Exception> R inTransaction(TransactionIsolationLevel level, TransactionCallback<R, X> callback) throws X {
        try (TransactionResetter tr = new TransactionResetter(getTransactionIsolationLevel())) {
            setTransactionIsolation(level);
            return transactions.inTransaction(this, level, callback);
        }
    }

    private class TransactionResetter implements Closeable {

        private TransactionIsolationLevel initial;

        TransactionResetter(TransactionIsolationLevel initial) {
            this.initial = initial;
        }

        @Override
        public void close() {
            setTransactionIsolation(initial);
        }
    }

    /**
     * Executes <code>callback</code> in a transaction.
     *
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     * @param level the transaction isolation level which will be applied to the connection for the scope of this
     *              transaction, after which the original isolation level will be restored.
     * @param callback a callback which will receive an open handle, in a transaction.
     * @param <X> exception type thrown by the callback, if any
     * @throws X any exception thrown by the callback
     */
    public <X extends Exception> void useTransaction(TransactionIsolationLevel level, TransactionConsumer<X> callback) throws X {
        inTransaction(level, (handle, status) -> {
            callback.useTransaction(handle, status);
            return null;
        });
    }

    /**
     * Set the transaction isolation level on the underlying connection.
     *
     * @param level the isolation level to use
     */
    public void setTransactionIsolation(TransactionIsolationLevel level) {
        setTransactionIsolation(level.intValue());
    }

    /**
     * Set the transaction isolation level on the underlying connection.
     *
     * @param level the isolation level to use
     */
    public void setTransactionIsolation(int level) {
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

    /**
     * Obtain the current transaction isolation level.
     *
     * @return the current isolation level on the underlying connection
     */
    public TransactionIsolationLevel getTransactionIsolationLevel() {
        try {
            return TransactionIsolationLevel.valueOf(connection.getTransactionIsolation());
        }
        catch (SQLException e) {
            throw new UnableToManipulateTransactionIsolationLevelException("unable to access current setting", e);
        }
    }

    /**
     * Create a JDBI extension object of the specified type bound to this handle. The returned extension's lifecycle is
     * coupled to the lifecycle of this handle. Closing the handle will render the extension unusable.
     *
     * @param extensionType the extension class
     * @param <T> the extension type
     * @return the new extension object bound to this handle
     */
    public <T> T attach(Class<T> extensionType) {
        return config.extensionRegistry.findExtensionFor(extensionType, () -> this)
                .orElseThrow(() -> new NoSuchExtensionException("Extension not found: " + extensionType));
    }

    public <C extends ExtensionConfig<C>> void configureExtension(Class<C> configClass, Consumer<C> consumer) {
        config.extensionRegistry.configure(configClass, consumer);
    }
}
