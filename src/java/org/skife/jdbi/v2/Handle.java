/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.TransactionFailedException;

import java.sql.Connection;
import java.util.Map;

public interface Handle
{

    /**
     * Get the JDBC Connection this Handle uses
     * @return the JDBC Connection this Handle uses 
     */
    Connection getConnection();

    /**
     * @throws org.skife.jdbi.v2.exceptions.UnableToCloseResourceException if any
     * resources throw exception while closing
     */
    void close();

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
     * Return a default Query instance which can be executed later, as long as this handle remains open.
     * @param sql the query sql
     */
    Query<Map<String, Object>> createQuery(String sql);

    /**
     * Create an Insert or Update statement which returns the number of rows modified.
     * @param sql The statement sql
     */
    UpdateStatement createStatement(String sql);

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
}
