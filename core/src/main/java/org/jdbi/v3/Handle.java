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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jdbi.v3.extension.ExtensionConfig;
import org.jdbi.v3.extension.ExtensionFactory;
import org.jdbi.v3.extension.NoSuchExtensionException;
import org.jdbi.v3.rewriter.StatementRewriter;
import org.jdbi.v3.tweak.ArgumentFactory;
import org.jdbi.v3.tweak.CollectorFactory;
import org.jdbi.v3.tweak.ColumnMapper;
import org.jdbi.v3.tweak.RowMapper;
import org.jdbi.v3.tweak.StatementBuilder;
import org.jdbi.v3.tweak.StatementLocator;

/**
 * This represents a connection to the database system. It usually is a wrapper around
 * a JDBC Connection object.
 */
public interface Handle extends Closeable
{

    /**
     * @return the JDBC Connection this Handle uses
     */
    Connection getConnection();

    /**
     * Closes the handle, its connection, and any other database resources it is holding.
     *
     * @throws org.jdbi.v3.exceptions.UnableToCloseResourceException if any resources throw exception while closing
     */
    @Override
    void close();

    /**
     * Define a statement attribute which will be applied to all {@link StatementContext}
     * instances for statements created from this handle
     *
     * @param key Attribute name
     * @param value Attribute value
     */
    void define(String key, Object value);

    /**
     * Start a transaction
     *
     * @return the same handle
     */
    Handle begin();

    /**
     * Commit a transaction
     *
     * @return the same handle
     */
    Handle commit();

    /**
     * Rollback a transaction
     *
     * @return the same handle
     */
    Handle rollback();

    /**
     * Rollback a transaction to a named checkpoint
     *
     * @param checkpointName the name of the checkpoint, previously declared with {@link Handle#checkpoint}
     *
     * @return the same handle
     */
    Handle rollback(String checkpointName);

    /**
     * @return whether the handle is in a transaction. Delegates to the underlying
     *         {@link org.jdbi.v3.tweak.TransactionHandler}.
     */
    boolean isInTransaction();

    /**
     * Return a default Query instance which can be executed later, as long as this handle remains open.
     * @param sql the select sql
     *
     * @return the Query
     */
    Query<Map<String, Object>> createQuery(String sql);

    /**
     * Create an Insert or Update statement which returns the number of rows modified.
     *
     * @param sql The statement sql
     *
     * @return the Update
     */
    Update createStatement(String sql);

    /**
     * Create a call to a stored procedure
     *
     * @param sql the stored procedure sql
     *
     * @return the Call
     */
    Call createCall(String sql);


    /**
     * Execute a simple insert statement
     *
     * @param sql the insert SQL
     * @param args positional arguments
     *
     * @return the number of rows inserted
     */
    int insert(String sql, Object... args);

    /**
     * Execute a simple update statement
     *
     * @param sql the update SQL
     * @param args positional arguments
     *
     * @return the number of updated inserted
     */
    int update(String sql, Object... args);

    /**
     * Prepare a batch to execute. This is for efficiently executing more than one
     * of the same statements with different parameters bound
     * @param sql the batch SQL
     * @return a batch which can have "statements" added
     */
    PreparedBatch prepareBatch(String sql);

    /**
     * Create a non-prepared (no bound parameters, but different SQL, batch statement
     * @return empty batch
     * @see Handle#prepareBatch(String)
     */
    Batch createBatch();

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
    <R, X extends Exception> R inTransaction(TransactionCallback<R, X> callback) throws X;

    /**
     * Executes <code>callback</code> in a transaction.
     *
     * @param callback a callback which will receive an open handle, in a transaction.
     * @param <X> exception type thrown by the callback, if any
     *
     * @throws X any exception thrown by the callback
     */
    <X extends Exception> void useTransaction(TransactionConsumer<X> callback) throws X;

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
    <R, X extends Exception> R inTransaction(TransactionIsolationLevel level,
                                             TransactionCallback<R, X> callback) throws X;

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
    <X extends Exception> void useTransaction(TransactionIsolationLevel level,
                                              TransactionConsumer<X> callback) throws X;

    /**
     * Convenience method which executes a select with purely positional arguments
     * @param sql SQL or named statement
     * @param args arguments to bind positionally
     * @return results of the query
     */
    List<Map<String, Object>> select(String sql, Object... args);

    /**
     * Allows for overriding the default statement locator. The default searches the
     * classpath for named statements
     *
     * @param locator the statement locator
     */
    void setStatementLocator(StatementLocator locator);

    /**
     * Allows for overiding the default statement rewriter. The default handles
     * named parameter interpolation.
     *
     * @param rewriter the statement rewriter.
     */
    void setStatementRewriter(StatementRewriter rewriter);

    /**
     * Creates an SQL script, looking for the source of the script using the
     * current statement locator (which defaults to searching the classpath)
     *
     * @param name the script name (passed to the statement locator)
     *
     * @return the created Script.
     */
    Script createScript(String name);

    /**
     * Execute some SQL with no return value
     * @param sql the sql to execute
     * @param args arguments to bind to the sql
     */
    void execute(String sql, Object... args);

    /**
     * Create a transaction checkpoint (savepoint in JDBC terminology) with the name provided.
     * @param name The name of the checkpoint
     * @return The same handle
     */
    Handle checkpoint(String name);

    /**
     * Release a previously created checkpoint
     *
     * @param checkpointName the name of the checkpoint to release
     *
     * @return the same handle
     */
    Handle release(String checkpointName);

    /**
     * Specify the statement builder to use for this handle
     * @param builder StatementBuilder to be used
     */
    void setStatementBuilder(StatementBuilder builder);

    /**
     * Specify the class used to collect timing information. The default is inherited from the DBI used
     * to create this Handle.
     *
     * @param timingCollector the timing collector
     */
    void setTimingCollector(TimingCollector timingCollector);

    /**
     * Register a row mapper which will have its parameterized type inspected to determine what it maps to
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     *
     * @param mapper the row mapper
     */
    void registerRowMapper(RowMapper<?> mapper);

    /**
     * Register a row mapper factory.
     *
     * Will be used with {@link Query#mapTo(Class)} for registerd mappings.
     *
     * @param factory the row mapper factory
     */
    void registerRowMapper(RowMapperFactory factory);

    /**
     * Register a column mapper which will have its parameterized type inspected to determine what it maps to
     *
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param mapper the column mapper
     */
    void registerColumnMapper(ColumnMapper<?> mapper);

    /**
     * Register a column mapper factory.
     *
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the column mapper factory
     */
    void registerColumnMapper(ColumnMapperFactory factory);

    /**
     * Create a JDBI extension object of the specified type bound to this handle. The returned extension's lifecycle is
     * coupled to the lifecycle of this handle. Closing the handle will render the extension unusable.
     *
     * @param extensionType the extension class
     * @param <T> the extension type
     * @return the new extension object bound to this handle
     */
    <T> T attach(Class<T> extensionType) throws NoSuchExtensionException;

    /**
     * Set the transaction isolation level on the underlying connection
     *
     * @param level the isolation level to use
     */
    void setTransactionIsolation(TransactionIsolationLevel level);

    /**
     * Set the transaction isolation level on the underlying connection
     *
     * @param level the isolation level to use
     */
    void setTransactionIsolation(int level);

    /**
     * Obtain the current transaction isolation level
     *
     * @return the current isolation level on the underlying connection
     */
    TransactionIsolationLevel getTransactionIsolationLevel();

    void registerArgumentFactory(ArgumentFactory argumentFactory);

    void registerCollectorFactory(CollectorFactory factory);

    void registerExtension(ExtensionFactory factory);

    <C extends ExtensionConfig<C>> void configureExtension(Class<C> configClass, Consumer<C> consumer);
}
