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
package org.jdbi.v3.core;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.exception.UnableToCloseResourceException;
import org.jdbi.v3.core.exception.UnableToManipulateTransactionIsolationLevelException;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.NoSuchExtensionException;
import org.jdbi.v3.core.mapper.DefaultMapper;
import org.jdbi.v3.core.statement.StatementBuilder;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.core.transaction.TransactionCallback;
import org.jdbi.v3.core.transaction.TransactionConsumer;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This represents a connection to the database system. It usually is a wrapper around
 * a JDBC Connection object.
 */
public class Handle implements Closeable, Configurable<Handle>
{
    private static final Logger LOG = LoggerFactory.getLogger(Handle.class);

    private final TransactionHandler transactions;
    private final Connection connection;

    private ThreadLocal<ConfigRegistry> config;
    private ThreadLocal<ExtensionMethod> extensionMethod;
    private StatementBuilder statementBuilder;

    private boolean closed = false;

    Handle(ConfigRegistry config,
           TransactionHandler transactions,
           StatementBuilder statementBuilder,
           Connection connection) {
        this.transactions = transactions;
        this.connection = connection;

        this.config = ThreadLocal.withInitial(() -> config);
        this.extensionMethod = new ThreadLocal<>();
        this.statementBuilder = statementBuilder;
    }

    @Override
    public ConfigRegistry getConfig() {
        return config.get();
    }

    void setConfig(ConfigRegistry config) {
        this.config.set(config);
    }

    void setConfigThreadLocal(ThreadLocal<ConfigRegistry> config) {
        this.config = config;
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
     * Specify the statement builder to use for this handle.
     * @param builder StatementBuilder to be used
     * @return this
     */
    public Handle setStatementBuilder(StatementBuilder builder) {
        this.statementBuilder = builder;
        return this;
    }

    /**
     * Closes the handle, its connection, and any other database resources it is holding.
     *
     * @throws UnableToCloseResourceException if any resources throw exception while closing
     */
    @Override
    public void close() {
        extensionMethod.remove();
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

    /**
     * @return whether the Handle is closed
     */
    public boolean isClosed() {
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
        Update stmt = createUpdate(sql);
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
        ConfigRegistry batchConfig = getConfig().createCopy();
        return new Batch(batchConfig,
                         this.connection,
                         new StatementContext(batchConfig, extensionMethod.get()));
    }

    /**
     * Prepare a batch to execute. This is for efficiently executing more than one
     * of the same statements with different parameters bound.
     *
     * @param sql the batch SQL
     * @return a batch which can have "statements" added
     */
    public PreparedBatch prepareBatch(String sql) {
        ConfigRegistry batchConfig = getConfig().createCopy();
        return new PreparedBatch(batchConfig,
                                 this,
                                 statementBuilder,
                                 sql,
                                 new StatementContext(batchConfig, extensionMethod.get()),
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
        ConfigRegistry callConfig = getConfig().createCopy();
        return new Call(callConfig,
                        this,
                        statementBuilder,
                        sql,
                        new StatementContext(callConfig, extensionMethod.get()),
                        Collections.<StatementCustomizer>emptyList());
    }

    /**
     * Return a default Query instance which can be executed later, as long as this handle remains open.
     * @param sql the select sql
     *
     * @return the Query
     */
    public Query<Map<String, Object>> createQuery(String sql) {
        ConfigRegistry queryConfig = getConfig().createCopy();
        return new Query<>(queryConfig,
                new Binding(),
                new DefaultMapper(),
                this,
                statementBuilder,
                sql,
                new StatementContext(queryConfig, extensionMethod.get()),
                Collections.<StatementCustomizer>emptyList());
    }

    /**
     * Creates a Script from the given SQL script
     *
     * @param sql the SQL script
     *
     * @return the created Script.
     */
    public Script createScript(String sql) {
        return new Script(this, sql);
    }

    /**
     * Create an Insert or Update statement which returns the number of rows modified.
     *
     * @param sql The statement sql
     *
     * @return the Update
     */
    public Update createUpdate(String sql) {
        ConfigRegistry updateConfig = getConfig().createCopy();
        return new Update(updateConfig,
                          this,
                          statementBuilder,
                          sql,
                          new StatementContext(updateConfig, extensionMethod.get()));
    }

    /**
     * @return whether the handle is in a transaction. Delegates to the underlying
     *         {@link TransactionHandler}.
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
     * Rollback a transaction to a named savepoint.
     *
     * @param savepointName the name of the savepoint, previously declared with {@link Handle#savepoint}
     *
     * @return the same handle
     */
    public Handle rollbackToSavepoint(String savepointName) {
        final long start = System.nanoTime();
        transactions.rollbackToSavepoint(this, savepointName);
        LOG.trace("Handle [{}] rollback to savepoint \"{}\" in {}ms", this, savepointName, ((System.nanoTime() - start) / 1000000L));
        return this;
    }

    /**
     * Create a transaction savepoint with the name provided.
     *
     * @param name The name of the savepoint
     * @return The same handle
     */
    public Handle savepoint(String name) {
        transactions.savepoint(this, name);
        LOG.trace("Handle [{}] savepoint \"{}\"", this, name);
        return this;
    }

    /**
     * Release a previously created savepoint.
     *
     * @param savepointName the name of the savepoint to release
     * @return the same handle
     */
    public Handle release(String savepointName) {
        transactions.releaseSavepoint(this, savepointName);
        LOG.trace("Handle [{}] release savepoint \"{}\"", this, savepointName);
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
        transactions.inTransaction(this, handle -> {
            callback.useTransaction(handle);
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

        private final TransactionIsolationLevel initial;

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
        inTransaction(level, handle -> {
            callback.useTransaction(handle);
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
        return getConfig(Extensions.class)
                .findFor(extensionType, ConstantHandleSupplier.of(this))
                .orElseThrow(() -> new NoSuchExtensionException("Extension not found: " + extensionType));
    }

    public ExtensionMethod getExtensionMethod() {
        return extensionMethod.get();
    }

    void setExtensionMethod(ExtensionMethod extensionMethod) {
        this.extensionMethod.set(extensionMethod);
    }

    /* package private */
    void setExtensionMethodThreadLocal(ThreadLocal<ExtensionMethod> extensionMethod) {
        this.extensionMethod = requireNonNull(extensionMethod);
    }
}
