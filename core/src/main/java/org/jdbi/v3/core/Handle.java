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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.concurrent.GuardedBy;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.Configurable;
import org.jdbi.v3.core.extension.ExtensionContext;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.NoSuchExtensionException;
import org.jdbi.v3.core.internal.exceptions.ThrowableSuppressor;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.statement.Batch;
import org.jdbi.v3.core.statement.Call;
import org.jdbi.v3.core.statement.Cleanable;
import org.jdbi.v3.core.statement.MetaData;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Script;
import org.jdbi.v3.core.statement.StatementBuilder;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.core.transaction.UnableToManipulateTransactionIsolationLevelException;
import org.jdbi.v3.meta.Beta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * This represents a connection to the database system. It is a wrapper around
 * a JDBC Connection object.  Handle provides essential methods for transaction
 * management, statement creation, and other operations tied to the database session.
 */
public class Handle implements Closeable, Configurable<Handle> {

    private static final Logger LOG = LoggerFactory.getLogger(Handle.class);

    private final Jdbi jdbi;
    private final Cleanable connectionCleaner;
    private final TransactionHandler transactionHandler;
    private final Connection connection;
    private final boolean forceEndTransactions;

    private StatementBuilder statementBuilder;

    // the fallback context. It is used when resetting the Handle state.
    private final ExtensionContext defaultExtensionContext;

    private ExtensionContext currentExtensionContext;

    @GuardedBy("transactionCallbacks")
    private final List<TransactionCallback> transactionCallbacks = new ArrayList<>();

    private final Set<Cleanable> cleanables = new LinkedHashSet<>();

    private final Set<HandleListener> handleListeners;

    private final AtomicBoolean closed = new AtomicBoolean();

    static Handle createHandle(Jdbi jdbi,
            Cleanable connectionCleaner,
            TransactionHandler transactionHandler,
            StatementBuilder statementBuilder,
            Connection connection) throws SQLException {
        Handle handle = new Handle(jdbi, connectionCleaner, transactionHandler, statementBuilder, connection);

        handle.notifyHandleCreated();
        return handle;
    }

    private Handle(Jdbi jdbi,
            Cleanable connectionCleaner,
            TransactionHandler transactionHandler,
            StatementBuilder statementBuilder,
            Connection connection) throws SQLException {
        this.jdbi = jdbi;
        this.connectionCleaner = connectionCleaner;
        this.connection = connection;

        // create a copy to detach config from the jdbi to allow local changes.
        this.defaultExtensionContext = ExtensionContext.forConfig(jdbi.getConfig().createCopy());
        this.currentExtensionContext = defaultExtensionContext;

        this.statementBuilder = statementBuilder;
        this.handleListeners = getConfig().get(Handles.class).copyListeners();

        // both of these methods are bad because they leak a reference to this handle before the c'tor finished.
        this.transactionHandler = transactionHandler.specialize(this);
        this.forceEndTransactions = !transactionHandler.isInTransaction(this);

        addCleanable(() -> {
            // shut down statement builder
            if (checkConnectionIsLive()) {
                statementBuilder.close(connection);
            }
        });
    }

    /**
     * Returns the {@link Jdbi} object used to create this handle.
     *
     * @return The {@link Jdbi} object used to create this handle.
     */
    public Jdbi getJdbi() {
        return jdbi;
    }

    /**
     * The current configuration object associated with this handle.
     *
     * @return A {@link ConfigRegistry} object that is associated with the handle.
     */
    @Override
    public ConfigRegistry getConfig() {
        return currentExtensionContext.getConfig();
    }

    /**
     * Get the JDBC {@link Connection} this Handle uses.
     *
     * @return the JDBC {@link Connection} this Handle uses.
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * Returns the current {@link StatementBuilder} which is used to create new JDBC {@link java.sql.Statement} objects.
     *
     * @return the current {@link StatementBuilder}.
     */
    public StatementBuilder getStatementBuilder() {
        return statementBuilder;
    }

    /**
     * Set the statement builder for this handle.
     *
     * @param builder StatementBuilder to be used. Must not be null.
     * @return this
     */
    public Handle setStatementBuilder(StatementBuilder builder) {
        this.statementBuilder = builder;
        return this;
    }

    /**
     * Add a specific {@link HandleListener} which is called for specific events for this Handle. Note that
     * it is not possible to add a listener that wants to implement {@link HandleListener#handleCreated} this way
     * as the handle has already been created. Use {@link Handles#addListener} in this case.
     * <br>
     * A listener added through this call is specific to the handle and not shared with other handles.
     *
     * @param handleListener A {@link HandleListener} object.
     * @return The handle itself.
     */
    public Handle addHandleListener(HandleListener handleListener) {
        handleListeners.add(handleListener);

        return this;
    }

    /**
     * Remove a {@link HandleListener} from this handle.
     * <br>
     * Removing the listener only affects the current handle. To remove a listener for all future handles, use {@link Handles#removeListener}.
     *
     * @param handleListener A {@link HandleListener} object.
     * @return The handle itself.
     */
    public Handle removeHandleListener(HandleListener handleListener) {
        handleListeners.remove(handleListener);

        return this;
    }

    /**
     * Registers a {@code Cleanable} to be invoked when the handle is closed. Any cleanable registered here will only be cleaned once.
     * <p>
     * Resources cleaned up by Jdbi include {@link java.sql.ResultSet}, {@link java.sql.Statement}, {@link java.sql.Array}, and {@link StatementBuilder}.
     *
     * @param cleanable the Cleanable to clean on close
     */
    public final void addCleanable(Cleanable cleanable) {

        synchronized (cleanables) {
            cleanables.add(cleanable);
        }
    }

    /**
     * Unregister a {@code Cleanable} from the Handle.
     *
     * @param cleanable the Cleanable to be unregistered.
     */
    public final void removeCleanable(Cleanable cleanable) {

        synchronized (cleanables) {
            cleanables.remove(cleanable);
        }
    }

    /**
     * Closes the handle, its connection, and any other database resources it is holding.
     *
     * @throws CloseException       if any resources throw exception while closing
     * @throws TransactionException if called while the handle has a transaction open. The open transaction will be
     *                              rolled back.
     */
    @Override
    public void close() {

        if (closed.getAndSet(true)) {
            return;
        }

        // do this at call time, otherwise running the cleanables may affect the state of the other handle objects (e.g. the config)
        final boolean doForceEndTransactions = this.forceEndTransactions && getConfig().get(Handles.class).isForceEndTransactions();

        try {
            ThrowableSuppressor throwableSuppressor = new ThrowableSuppressor();

            doClean(throwableSuppressor);

            try {
                cleanConnection(doForceEndTransactions);
            } catch (Throwable t) {
                throwableSuppressor.attachToThrowable(t);
                throw t;
            }

            throwableSuppressor.throwIfNecessary(t -> new CloseException("While closing handle", t));
        } finally {
            LOG.trace("Handle [{}] released", this);

            notifyHandleClosed();
        }
    }

    /**
     * Release any database resource that may be held by the handle. This affects
     * any statement that was created from the Handle.
     */
    public void clean() {
        ThrowableSuppressor throwableSuppressor = new ThrowableSuppressor();

        doClean(throwableSuppressor);

        throwableSuppressor.throwIfNecessary();
    }

    /**
     * Returns true if the Handle currently holds no database resources.
     * <br>
     * Note that this method will return <code>false</code> right after statement creation
     * as every statement registers its statement context with the handle. Once
     *
     * @return True if the handle holds no database resources.
     */
    public boolean isClean() {
        synchronized (cleanables) {
            return cleanables.isEmpty();
        }
    }

    private void doClean(ThrowableSuppressor throwableSuppressor) {
        List<Cleanable> cleanablesCopy;

        synchronized (cleanables) {
            cleanablesCopy = new ArrayList<>(cleanables);
            cleanables.clear();
        }

        Collections.reverse(cleanablesCopy);

        for (Cleanable cleanable : cleanablesCopy) {
            throwableSuppressor.suppressAppend(cleanable::close);
        }
    }

    /**
     * Returns true if the {@link Handle} has been closed.
     *
     * @return True if the Handle is closed.
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Convenience method which creates a query with the given positional arguments.
     *
     * @param sql  SQL or named statement
     * @param args arguments to bind positionally
     * @return query object
     */
    public Query select(CharSequence sql, Object... args) {
        Query query = this.createQuery(sql);
        int position = 0;
        for (Object arg : args) {
            query.bind(position++, arg);
        }
        return query;
    }

    /**
     * Convenience method which creates a query with the given positional arguments. Takes a string argument for backwards compatibility reasons.
     *
     * @param sql  SQL or named statement
     * @param args arguments to bind positionally
     * @return query object
     * @see Handle#select(CharSequence, Object...)
     */
    public Query select(String sql, Object... args) {
        return select((CharSequence) sql, args);
    }

    /**
     * Execute a SQL statement, and return the number of rows affected by the statement.
     *
     * @param sql  the SQL statement to execute, using positional parameters (if any).
     * @param args positional arguments.
     * @return the number of rows affected.
     */
    public int execute(CharSequence sql, Object... args) {
        try (Update stmt = createUpdate(sql)) {
            int position = 0;
            for (Object arg : args) {
                stmt.bind(position++, arg);
            }
            return stmt.execute();
        }
    }

    /**
     * Execute a SQL statement, and return the number of rows affected by the statement. Takes a string argument for backwards compatibility reasons.
     *
     * @param sql  the SQL statement to execute, using positional parameters (if any).
     * @param args positional arguments.
     * @return the number of rows affected.
     * @see Handle#execute(CharSequence, Object...)
     */
    public int execute(String sql, Object... args) {
        return execute((CharSequence) sql, args);
    }

    /**
     * Create a non-prepared (no bound parameters, but different SQL) batch statement.
     *
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
     * @param sql the batch SQL.
     * @return a batch which can have "statements" added.
     */
    public PreparedBatch prepareBatch(CharSequence sql) {
        return new PreparedBatch(this, sql);
    }

    /**
     * Prepare a batch to execute. This is for efficiently executing more than one
     * of the same statements with different parameters bound. Takes a string argument for backwards compatibility reasons.
     *
     * @param sql the batch SQL.
     * @return a batch which can have "statements" added.
     * @see Handle#prepareBatch(CharSequence)
     */
    public PreparedBatch prepareBatch(String sql) {
        return prepareBatch((CharSequence) sql);
    }

    /**
     * Create a call to a stored procedure.
     *
     * @param sql the stored procedure sql.
     * @return the Call.
     */
    public Call createCall(CharSequence sql) {
        return new Call(this, sql);
    }

    /**
     * Create a call to a stored procedure. Takes a string argument for backwards compatibility reasons.
     *
     * @param sql the stored procedure sql.
     * @return the Call.
     * @see Handle#createCall(CharSequence)
     */
    public Call createCall(String sql) {
        return createCall((CharSequence) sql);
    }

    /**
     * Return a Query instance that executes a statement
     * with bound parameters and maps the result set into Java types.
     *
     * @param sql SQL that may return results.
     * @return a Query builder.
     */
    public Query createQuery(CharSequence sql) {
        return new Query(this, sql);
    }

    /**
     * Return a Query instance that executes a statement
     * with bound parameters and maps the result set into Java types. Takes a string argument for backwards compatibility reasons.
     *
     * @param sql SQL that may return results.
     * @return a Query builder.
     * @see Handle#createQuery(CharSequence)
     */
    public Query createQuery(String sql) {
        return createQuery((CharSequence) sql);
    }

    /**
     * Creates a Script from the given SQL script.
     *
     * @param sql the SQL script.
     * @return the created Script.
     */
    public Script createScript(CharSequence sql) {
        return new Script(this, sql);
    }

    /**
     * Create an Insert or Update statement which returns the number of rows modified. Takes a string argument for backwards compatibility reasons.
     *
     * @param sql the statement sql.
     * @return the Update builder.
     * @see Handle#createScript(CharSequence)
     */
    public Script createScript(String sql) {
        return createScript((CharSequence) sql);
    }

    /**
     * Create an Insert or Update statement which returns the number of rows modified.
     *
     * @param sql the statement sql.
     * @return the Update builder.
     */
    public Update createUpdate(CharSequence sql) {
        return new Update(this, sql);
    }

    /**
     * Create an Insert or Update statement which returns the number of rows modified. Takes a string argument for backwards compatibility reasons.
     *
     * @param sql the statement sql.
     * @return the Update builder.
     * @see Handle#createUpdate(CharSequence)
     */
    public Update createUpdate(String sql) {
        return createUpdate((CharSequence) sql);
    }

    /**
     * Access database metadata that returns a {@link java.sql.ResultSet}. All methods of {@link org.jdbi.v3.core.result.ResultBearing} can be used to format
     * and map the returned results.
     *
     * <pre>
     *     List&lt;String&gt; catalogs = h.queryMetadata(DatabaseMetaData::getCatalogs)
     *                                      .mapTo(String.class)
     *                                      .list();
     * </pre>
     * <p>
     * returns the list of catalogs from the current database.
     *
     * @param metadataFunction Maps the provided {@link java.sql.DatabaseMetaData} object onto a {@link java.sql.ResultSet} object.
     * @return The metadata builder.
     */
    public ResultBearing queryMetadata(MetaData.MetaDataResultSetProvider metadataFunction) {
        return new MetaData(this, metadataFunction);
    }

    /**
     * Access all database metadata that returns simple values.
     *
     * <pre>
     *     boolean supportsTransactions = handle.queryMetadata(DatabaseMetaData::supportsTransactions);
     * </pre>
     *
     * @param metadataFunction Maps the provided {@link java.sql.DatabaseMetaData} object to a response object.
     * @return The response object.
     */
    public <T> T queryMetadata(MetaData.MetaDataValueProvider<T> metadataFunction) {
        try (MetaData metadata = new MetaData(this, metadataFunction)) {
            return metadata.execute();
        }
    }

    /**
     * Returns whether the handle is in a transaction. Delegates to the underlying {@link TransactionHandler}.
     *
     * @return True if the handle is in a transaction.
     */
    public boolean isInTransaction() {
        return transactionHandler.isInTransaction(this);
    }

    /**
     * Start a transaction.
     *
     * @return the same handle.
     */
    public Handle begin() {
        transactionHandler.begin(this);
        LOG.trace("Handle [{}] begin transaction", this);
        return this;
    }

    /**
     * Commit a transaction.
     *
     * @return the same handle.
     */
    public Handle commit() {
        final long start = System.nanoTime();
        transactionHandler.commit(this);
        LOG.trace("Handle [{}] commit transaction in {}ms", this, msSince(start));
        drainCallbacks()
                .forEach(TransactionCallback::afterCommit);
        return this;
    }

    /**
     * Rollback a transaction.
     *
     * @return the same handle.
     */
    public Handle rollback() {
        final long start = System.nanoTime();
        transactionHandler.rollback(this);
        LOG.trace("Handle [{}] rollback transaction in {}ms", this, msSince(start));
        drainCallbacks()
                .forEach(TransactionCallback::afterRollback);
        return this;
    }

    /**
     * Execute an action the next time this Handle commits, unless it is rolled back first.
     *
     * @param afterCommit the action to execute after commit.
     * @return this Handle.
     */
    @Beta
    public Handle afterCommit(Runnable afterCommit) {
        return addTransactionCallback(new TransactionCallback() {
            @Override
            public void afterCommit() {
                afterCommit.run();
            }
        });
    }

    /**
     * Execute an action the next time this Handle rolls back, unless it is committed first.
     *
     * @param afterRollback the action to execute after rollback.
     * @return this Handle.
     */
    @Beta
    public Handle afterRollback(Runnable afterRollback) {
        return addTransactionCallback(new TransactionCallback() {
            @Override
            public void afterRollback() {
                afterRollback.run();
            }
        });
    }

    List<TransactionCallback> drainCallbacks() {
        synchronized (transactionCallbacks) {
            List<TransactionCallback> result = new ArrayList<>(transactionCallbacks);
            transactionCallbacks.clear();
            return result;
        }
    }

    Handle addTransactionCallback(TransactionCallback cb) {
        if (!isInTransaction()) {
            throw new IllegalStateException("Handle must be in transaction");
        }
        synchronized (transactionCallbacks) {
            transactionCallbacks.add(cb);
        }
        return this;
    }

    /**
     * Rollback a transaction to a named savepoint.
     *
     * @param savepointName the name of the savepoint, previously declared with {@link Handle#savepoint}.
     * @return the same handle.
     */
    public Handle rollbackToSavepoint(String savepointName) {
        final long start = System.nanoTime();
        transactionHandler.rollbackToSavepoint(this, savepointName);
        LOG.trace("Handle [{}] rollback to savepoint \"{}\" in {}ms", this, savepointName, msSince(start));
        return this;
    }

    private static long msSince(final long start) {
        return MILLISECONDS.convert(System.nanoTime() - start, NANOSECONDS);
    }

    /**
     * Create a transaction savepoint with the name provided.
     *
     * @param name The name of the savepoint.
     * @return The same handle.
     */
    public Handle savepoint(String name) {
        transactionHandler.savepoint(this, name);
        LOG.trace("Handle [{}] savepoint \"{}\"", this, name);
        return this;
    }

    /**
     * Release a previously created savepoint.
     *
     * @param savepointName the name of the savepoint to release.
     * @return the same handle.
     * @deprecated Use {@link Handle#releaseSavepoint(String)}
     */
    @Deprecated
    public Handle release(String savepointName) {
        return releaseSavepoint(savepointName);
    }

    /**
     * Release a previously created savepoint.
     *
     * @param savepointName the name of the savepoint to release.
     * @return the same handle.
     */
    public Handle releaseSavepoint(String savepointName) {
        transactionHandler.releaseSavepoint(this, savepointName);
        LOG.trace("Handle [{}] release savepoint \"{}\"", this, savepointName);
        return this;
    }

    /**
     * Whether the connection is in read-only mode.
     *
     * @return True if the connection is in read-only mode.
     * @see Connection#isReadOnly()
     */
    public boolean isReadOnly() {
        try {
            return connection.isReadOnly();
        } catch (SQLException e) {
            throw new UnableToManipulateTransactionIsolationLevelException("Could not get read-only status for a connection", e);
        }
    }

    /**
     * Set the Handle read-only. This acts as a hint to the database to improve performance or concurrency.
     * <br/>
     * May not be called in an active transaction!
     *
     * @param readOnly whether the Handle is readOnly.
     * @return this Handle.
     * @see Connection#setReadOnly(boolean)
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
     * @param <R>      type returned by callback
     * @param <X>      exception type thrown by the callback, if any
     * @return value returned from the callback
     * @throws X any exception thrown by the callback
     */
    public <R, X extends Exception> R inTransaction(HandleCallback<R, X> callback) throws X {
        return isInTransaction()
                ? callback.withHandle(this)
                : transactionHandler.inTransaction(this, callback);
    }

    /**
     * Executes <code>callback</code> in a transaction.
     *
     * @param consumer a callback which will receive an open handle, in a transaction.
     * @param <X>      exception type thrown by the callback, if any
     * @throws X any exception thrown by the callback
     */
    public <X extends Exception> void useTransaction(final HandleConsumer<X> consumer) throws X {
        inTransaction(consumer.asCallback());
    }

    /**
     * Executes <code>callback</code> in a transaction, and returns the result of the callback.
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     *
     * @param level    the transaction isolation level which will be applied to the connection for the scope of this
     *                 transaction, after which the original isolation level will be restored.
     * @param callback a callback which will receive an open handle, in a transaction.
     * @param <R>      type returned by callback
     * @param <X>      exception type thrown by the callback, if any
     * @return value returned from the callback
     * @throws X any exception thrown by the callback
     */
    public <R, X extends Exception> R inTransaction(TransactionIsolationLevel level, HandleCallback<R, X> callback) throws X {
        if (isInTransaction()) {
            TransactionIsolationLevel currentLevel = getTransactionIsolationLevel();
            if (currentLevel != level && level != TransactionIsolationLevel.UNKNOWN) {
                throw new TransactionException(
                        "Tried to execute nested transaction with isolation level " + level + ", "
                                + "but already running in a transaction with isolation level " + currentLevel + ".");
            }
            return callback.withHandle(this);
        }

        try (SetTransactionIsolation isolation = new SetTransactionIsolation(level)) {
            return transactionHandler.inTransaction(this, level, callback);
        }
    }

    /**
     * Executes <code>callback</code> in a transaction.
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     *
     * @param level    the transaction isolation level which will be applied to the connection for the scope of this
     *                 transaction, after which the original isolation level will be restored.
     * @param consumer a callback which will receive an open handle, in a transaction.
     * @param <X>      exception type thrown by the callback, if any
     * @throws X any exception thrown by the callback
     */
    public <X extends Exception> void useTransaction(TransactionIsolationLevel level, HandleConsumer<X> consumer) throws X {
        inTransaction(level, consumer.asCallback());
    }

    /**
     * Set the transaction isolation level on the underlying connection if it is different from the current isolation level.
     *
     * @param level the {@link TransactionIsolationLevel} to use.
     * @throws UnableToManipulateTransactionIsolationLevelException if isolation level is not supported by the underlying connection or JDBC driver.
     * @deprecated Use {@link Handle#setTransactionIsolationLevel(int)}
     */
    @Deprecated
    public void setTransactionIsolation(TransactionIsolationLevel level) {
        setTransactionIsolationLevel(level);
    }

    /**
     * Set the transaction isolation level on the underlying connection if it is different from the current isolation level.
     *
     * @param level the {@link TransactionIsolationLevel} to use.
     * @throws UnableToManipulateTransactionIsolationLevelException if isolation level is not supported by the underlying connection or JDBC driver.
     */
    public void setTransactionIsolationLevel(TransactionIsolationLevel level) {
        if (level != TransactionIsolationLevel.UNKNOWN) {
            setTransactionIsolationLevel(level.intValue());
        }
    }

    /**
     * Set the transaction isolation level on the underlying connection if it is different from the current isolation level.
     *
     * @param level the isolation level to use.
     * @see Handle#setTransactionIsolationLevel(TransactionIsolationLevel)
     * @see Connection#TRANSACTION_NONE
     * @see Connection#TRANSACTION_READ_UNCOMMITTED
     * @see Connection#TRANSACTION_READ_COMMITTED
     * @see Connection#TRANSACTION_REPEATABLE_READ
     * @see Connection#TRANSACTION_SERIALIZABLE
     * @deprecated Use {@link Handle#setTransactionIsolationLevel(TransactionIsolationLevel)}
     */
    @Deprecated
    public void setTransactionIsolation(int level) {
        setTransactionIsolationLevel(level);
    }

    /**
     * Set the transaction isolation level on the underlying connection if it is different from the current isolation level.
     *
     * @param level the isolation level to use.
     * @see Handle#setTransactionIsolationLevel(TransactionIsolationLevel)
     * @see Connection#TRANSACTION_NONE
     * @see Connection#TRANSACTION_READ_UNCOMMITTED
     * @see Connection#TRANSACTION_READ_COMMITTED
     * @see Connection#TRANSACTION_REPEATABLE_READ
     * @see Connection#TRANSACTION_SERIALIZABLE
     */
    public void setTransactionIsolationLevel(int level) {
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
     * @return the current isolation level on the underlying connection.
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
     * @param <T>           the extension type
     * @return the new extension object bound to this handle
     */
    public <T> T attach(Class<T> extensionType) {
        return getConfig(Extensions.class)
                .findFor(extensionType, ConstantHandleSupplier.of(this))
                .orElseThrow(() -> new NoSuchExtensionException(extensionType));
    }

    /**
     * Returns the extension method currently bound to the handle's context.
     *
     * @return the extension method currently bound to the handle's context
     */
    public ExtensionMethod getExtensionMethod() {
        return currentExtensionContext.getExtensionMethod();
    }

    Handle acceptExtensionContext(ExtensionContext extensionContext) {
        this.currentExtensionContext = extensionContext == null ? defaultExtensionContext : extensionContext;

        return this;
    }

    private void notifyHandleCreated() {
        handleListeners.forEach(listener -> listener.handleCreated(this));
    }

    private void notifyHandleClosed() {
        handleListeners.forEach(listener -> listener.handleClosed(this));
    }

    private void cleanConnection(boolean doForceEndTransactions) {

        final ThrowableSuppressor throwableSuppressor = new ThrowableSuppressor();
        final boolean connectionIsLive = checkConnectionIsLive();

        boolean wasInTransaction = false;

        if (connectionIsLive && doForceEndTransactions) {
            wasInTransaction = throwableSuppressor.suppressAppend(this::isInTransaction, false);
        }

        if (wasInTransaction) {
            throwableSuppressor.suppressAppend(this::rollback);
        }

        if (connectionIsLive) {
            try {
                connectionCleaner.close();
            } catch (SQLException e) {
                CloseException ce = new CloseException("Unable to close Connection", e);
                throwableSuppressor.attachToThrowable(ce);
                throw ce;
            }

            throwableSuppressor.throwIfNecessary(t -> new CloseException("Failed to clear transaction status on close", t));

            if (wasInTransaction) {
                TransactionException te = new TransactionException("Improper transaction handling detected: A Handle with an open "
                        + "transaction was closed. Transactions must be explicitly committed or rolled back "
                        + "before closing the Handle. "
                        + "Jdbi has rolled back this transaction automatically. "
                        + "This check may be disabled by calling getConfig(Handles.class).setForceEndTransactions(false).");

                throwableSuppressor.attachToThrowable(te); // any exception present is not the cause but just collateral.
                throw te;
            }
        } else {
            throwableSuppressor.throwIfNecessary(t -> new CloseException("Failed to clear transaction status on close", t));
        }
    }

    private boolean checkConnectionIsLive() {
        try {
            return !connection.isClosed();
        } catch (SQLException e) {
            // if the connection state can not be determined, assume that the
            // connection is closed and ignore the exception
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Handle handle = (Handle) o;
        return jdbi.equals(handle.jdbi) && connection.equals(handle.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jdbi, connection);
    }

    class SetTransactionIsolation implements AutoCloseable {
        private final TransactionIsolationLevel prevLevel;

        SetTransactionIsolation(TransactionIsolationLevel setLevel) {
            prevLevel = getTransactionIsolationLevel();
            setTransactionIsolationLevel(setLevel);
        }

        @Override
        public void close() {
            setTransactionIsolationLevel(prevLevel);
        }
    }
}
