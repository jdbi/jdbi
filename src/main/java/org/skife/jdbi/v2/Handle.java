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

import org.skife.jdbi.v2.exceptions.TransactionFailedException;
import org.skife.jdbi.v2.tweak.ArgumentFactory;
import org.skife.jdbi.v2.tweak.ContainerFactory;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.io.Closeable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * This represents a connection to the database system. It ususally is a wrapper around
 * a JDBC Connection object.
 */
public interface Handle extends Closeable
{

    /**
     * Get the JDBC Connection this Handle uses
     * @return the JDBC Connection this Handle uses
     */
    Connection getConnection();

    @Override
    /**
     * @throws org.skife.jdbi.v2.exceptions.UnableToCloseResourceException if any
     * resources throw exception while closing
     */
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
     */
    Handle begin();

    /**
     * Commit a transaction
     */
    Handle commit();

    /**
     * Rollback a transaction
     */
    Handle rollback();

    /**
     * Rollback a transaction to a named checkpoint
     * @param checkpointName the name of the checkpoint, previously declared with {@link Handle#checkpoint}
     */
    Handle rollback(String checkpointName);

    /**
     * Is the handle in a transaction? It defers to the underlying {@link org.skife.jdbi.v2.tweak.TransactionHandler}
     */
    boolean isInTransaction();

    /**
     * Return a default Query instance which can be executed later, as long as this handle remains open.
     * @param sql the select sql
     */
    Query<Map<String, Object>> createQuery(String sql);

    /**
     * Create an Insert or Update statement which returns the number of rows modified.
     * @param sql The statement sql
     */
    Update createStatement(String sql);

    /**
     * Create a call to a stored procedure
     *
     * @param callableSql
     * @return the Call
     */
    Call createCall(String callableSql);


    /**
     * Execute a simple insert statement
     * @param sql the insert SQL
     * @return the number of rows inserted
     */
    int insert(String sql, Object... args);

    /**
     * Execute a simple update statement
     * @param sql the update SQL
     * @param args positional arguments
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
     * Executes <code>callback</code> in a transaction. If the transaction succeeds, the
     * result of the callback will be returned. If it fails a {@link TransactionFailedException}
     * will be thrown.
     *
     * @return value returned from the callback
     * @throws TransactionFailedException if the transaction failed in the callback
     */
    <ReturnType> ReturnType inTransaction(TransactionCallback<ReturnType> callback) throws TransactionFailedException;

    /**
     * Executes <code>callback</code> in a transaction. If the transaction succeeds, the
     * result of the callback will be returned. If it fails a {@link TransactionFailedException}
     * will be thrown.
     *
     * @throws TransactionFailedException if the transaction failed in the callback
     */
    void useTransaction(TransactionConsumer callback) throws TransactionFailedException;

    /**
     * Executes <code>callback</code> in a transaction. If the transaction succeeds, the
     * result of the callback will be returned. If it fails a {@link TransactionFailedException}
     * will be thrown.
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     * @return value returned from the callback
     * @throws TransactionFailedException if the transaction failed in the callback
     */
    <ReturnType> ReturnType inTransaction(TransactionIsolationLevel level,
                                          TransactionCallback<ReturnType> callback) throws TransactionFailedException;

    /**
     * Executes <code>callback</code> in a transaction. If the transaction succeeds, the
     * result of the callback will be returned. If it fails a {@link TransactionFailedException}
     * will be thrown.
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     * @return value returned from the callback
     * @throws TransactionFailedException if the transaction failed in the callback
     */
    void useTransaction(TransactionIsolationLevel level, TransactionConsumer callback) throws TransactionFailedException;

    /**
     * Convenience method which executes a select with purely positional arguments
     * @param sql SQL or named statement
     * @param args arguments to bind positionally
     * @return results of the query
     */
    List<Map<String, Object>> select(String sql, Object... args);

    /**
     * Allows for overiding the default statement locator. The default searches the
     * classpath for named statements
     */
    void setStatementLocator(StatementLocator locator);

    /**
     * Allows for overiding the default statement rewriter. The default handles
     * named parameter interpolation.
     */
    void setStatementRewriter(StatementRewriter rewriter);

    /**
     * Creates an SQL script, looking for the source of the script using the
     * current statement locator (which defaults to searching the classpath)
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
     */
    Handle release(String checkpointName);

    /**
     * Specify the statement builder to use for this handle
     * @param builder StatementBuilder to be used
     */
    void setStatementBuilder(StatementBuilder builder);

    /**
     * Specify the class used to log sql statements. The default is inherited from the DBI used
     * to create this Handle.
     */
    void setSQLLog(SQLLog log);

    /**
     * Specify the class used to collect timing information. The default is inherited from the DBI used
     * to create this Handle.
     */
    void setTimingCollector(TimingCollector timingCollector);

    /**
     * Register a result set mapper which will have its parameterized type inspected to determine what it maps to
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     */
    void registerMapper(ResultSetMapper mapper);

    /**
     * Register a result set mapper factory.
     *
     * Will be used with {@link Query#mapTo(Class)} for registerd mappings.
     */
    void registerMapper(ResultSetMapperFactory factory);

    /**
     * Create a a sql object of the specified type bound to this handle. Any state changes to the handle, or the
     * sql object, such as transaction status, closing it, etc, will apply to both the object and the handle.
     *
     * @param sqlObjectType
     * @param <SqlObjectType>
     * @return the new sql object bound to this handle
     */
    <SqlObjectType> SqlObjectType attach(Class<SqlObjectType> sqlObjectType);

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

    void registerContainerFactory(ContainerFactory<?> factory);
}
