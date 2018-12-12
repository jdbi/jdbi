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

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.Configurable;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.NoSuchExtensionException;
import org.jdbi.v3.core.statement.Batch;
import org.jdbi.v3.core.statement.Call;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Script;
import org.jdbi.v3.core.statement.StatementBuilder;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.core.transaction.UnableToManipulateTransactionIsolationLevelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * This represents a connection to the database system. It is a wrapper around
 * a JDBC Connection object.  Handle provides essential methods for transaction
 * management, statement creation, and other operations tied to the database session.
 */
public class Handle implements Closeable, Configurable<Handle> {
    private static final Logger LOG = LoggerFactory.getLogger(Handle.class);

    private final ConnectionCloser closer;
    private final TransactionHandler transactions;
    private final Connection connection;
    private final boolean forceEndTransactions;

    private ThreadLocal<ConfigRegistry> config;
    private ThreadLocal<ExtensionMethod> extensionMethod;
    private StatementBuilder statementBuilder;

    private boolean closed = false;

    Handle(ConfigRegistry config,
           ConnectionCloser closer,
           TransactionHandler transactions,
           StatementBuilder statementBuilder,
           Connection connection) {
        this.closer = closer;
        this.transactions = transactions;
        this.connection = connection;

        this.config = ThreadLocal.withInitial(() -> config);
        this.extensionMethod = new ThreadLocal<>();
        this.statementBuilder = statementBuilder;
        this.forceEndTransactions = !transactions.isInTransaction(this);
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
     * @return the current {@link StatementBuilder}
     */
    public StatementBuilder getStatementBuilder() {
        return statementBuilder;
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
     * @throws CloseException if any resources throw exception while closing
     * @throws TransactionException if called while the handle has a transaction open. The open transaction will be
     * rolled back.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        boolean wasInTransaction = isInTransaction()
            && forceEndTransactions
            && config.get().get(Handles.class).isForceEndTransactions();

        extensionMethod.remove();
        config.remove();

        if (wasInTransaction) {
            rollback();
        }

        Exception suppressed = null;
        try {
            statementBuilder.close(getConnection());
        } catch (Exception e) {
            suppressed = e;
        }

        try {
            closer.close(connection);

            if (wasInTransaction) {
                TransactionException txe = new TransactionException("Improper transaction handling detected: A Handle with an open "
                    + "transaction was closed. Transactions must be explicitly committed or rolled back "
                    + "before closing the Handle. "
                    + "Jdbi has rolled back this transaction automatically. "
                    + "This check may be disabled by calling getConfig(Handles.class).setForceEndTransactions(false).");
                if (suppressed != null) {
                    txe.addSuppressed(suppressed);
                }
                throw txe;
            }
        } catch (SQLException e) {
            CloseException ce = new CloseException("Unable to close Connection", e);
            if (suppressed != null) {
                ce.addSuppressed(suppressed);
            }
            throw ce;
        } finally {
            LOG.trace("Handle [{}] released", this);
            closed = true;
        }
    }

    /**
     * @return whether the Handle is closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Convenience method which creates a query with the given positional arguments
     * @param sql SQL or named statement
     * @param args arguments to bind positionally
     * @return query object
     */
    public Query select(String sql, Object... args) {
        Query query = this.createQuery(sql);
        int position = 0;
        for (Object arg : args) {
            query.bind(position++, arg);
        }
        return query;
    }

    /**
     * Execute a SQL statement, and return the number of rows affected by the statement.
     *
     * @param sql the SQL statement to execute, using positional parameters (if any)
     * @param args positional arguments
     *
     * @return the number of rows affected
     */
    public int execute(String sql, Object... args) {
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
        return new Batch(this);
    }

    /**
     * Prepare a batch to execute. This is for efficiently executing more than one
     * of the same statements with different parameters bound.
     *
     * @param sql the batch SQL
     * @return a batch which can have "statements" added
     */
    public PreparedBatch prepareBatch(String sql) {
        return new PreparedBatch(this, sql);
    }

    /**
     * Create a call to a stored procedure.
     *
     * @param sql the stored procedure sql
     *
     * @return the Call
     */
    public Call createCall(String sql) {
        return new Call(this, sql);
    }

    /**
     * Return a Query instance that executes a statement
     * with bound parameters and maps the result set into Java types.
     * @param sql SQL that may return results
     * @return a Query builder
     */
    public Query createQuery(String sql) {
        return new Query(this, sql);
    }

    /**
     * Creates a Script from the given SQL script.
     *
     * @param sql the SQL script
     *
     * @return the created Script
     */
    public Script createScript(String sql) {
        return new Script(this, sql);
    }

    /**
     * Create an Insert or Update statement which returns the number of rows modified.
     *
     * @param sql the statement sql
     *
     * @return the Update builder
     */
    public Update createUpdate(String sql) {
        return new Update(this, sql);
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
        LOG.trace("Handle [{}] rollback transaction in {}ms", this, (System.nanoTime() - start) / 1000000L);
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
        LOG.trace("Handle [{}] rollback to savepoint \"{}\" in {}ms", this, savepointName, (System.nanoTime() - start) / 1000000L);
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
     * @see Connection#isReadOnly()
     * @return whether the connection is in read-only mode
     */
    public boolean isReadOnly() {
        try {
            return connection.isReadOnly();
        } catch (SQLException e) {
            throw new UnableToManipulateTransactionIsolationLevelException("Could not getReadOnly", e);
        }
    }

    /**
     * Set the Handle readOnly.
     * This acts as a hint to the database to improve performance or concurrency.
     *
     * May not be called in an active transaction!
     *
     * @see Connection#setReadOnly(boolean)
     * @param readOnly whether the Handle is readOnly
     * @return this Handle
     */
    public Handle setReadOnly(boolean readOnly) {
        try {
            connection.setReadOnly(readOnly);
        } catch (SQLException e) {
            throw new UnableToManipulateTransactionIsolationLevelException("Could not setReadOnly", e);
        }
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
    public <R, X extends Exception> R inTransaction(HandleCallback<R, X> callback) throws X {
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
    public <X extends Exception> void useTransaction(final HandleConsumer<X> callback) throws X {
        transactions.inTransaction(this, handle -> {
            callback.useHandle(handle);
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
    public <R, X extends Exception> R inTransaction(TransactionIsolationLevel level, HandleCallback<R, X> callback) throws X {
        try (TransactionResetter tr = new TransactionResetter(getTransactionIsolationLevel())) {
            setTransactionIsolation(level);
            return transactions.inTransaction(this, level, callback);
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
    public <X extends Exception> void useTransaction(TransactionIsolationLevel level, HandleConsumer<X> callback) throws X {
        inTransaction(level, handle -> {
            callback.useHandle(handle);
            return null;
        });
    }

    /**
     * Set the transaction isolation level on the underlying connection.
     *
     * @param level the isolation level to use
     */
    public void setTransactionIsolation(TransactionIsolationLevel level) {
        if (level != TransactionIsolationLevel.UNKNOWN) {
            setTransactionIsolation(level.intValue());
        }
    }

    /**
     * Set the transaction isolation level on the underlying connection.
     *
     * @param level the isolation level to use
     */
    public void setTransactionIsolation(int level) {
        try {
            if (connection.getTransactionIsolation() != level) {
                connection.setTransactionIsolation(level);
            }
        } catch (SQLException e) {
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
        } catch (SQLException e) {
            throw new UnableToManipulateTransactionIsolationLevelException("unable to access current setting", e);
        }
    }

    /**
     * Create a Jdbi extension object of the specified type bound to this handle. The returned extension's lifecycle is
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

    /**
     * @return the extension method currently bound to the handle's context
     */
    public ExtensionMethod getExtensionMethod() {
        return extensionMethod.get();
    }

    void setExtensionMethod(ExtensionMethod extensionMethod) {
        this.extensionMethod.set(extensionMethod);
    }

    void setExtensionMethodThreadLocal(ThreadLocal<ExtensionMethod> extensionMethod) {
        this.extensionMethod = requireNonNull(extensionMethod);
    }

    interface ConnectionCloser {
        void close(Connection conn) throws SQLException;
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
}
