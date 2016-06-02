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
package org.jdbi.v3.transaction;

import org.jdbi.v3.Handle;
import org.jdbi.v3.TransactionCallback;
import org.jdbi.v3.TransactionIsolationLevel;

/**
 * Interface which defines callbacks to be used when transaction methods are called on a handle.
 * Used by specifying on a <code>DBI</code> instance. All <code>Handle</code> instances
 * opened from that <code>DBI</code> will use the handler specified.
 *
 * <p>
 * The default implementation, <code>ConnectionTransactionHandler</code>, explicitly manages
 * the transactions on the underlying JDBC <code>Connection</code>.
 * </p>
 */
public interface TransactionHandler
{
    /**
     * Called when a transaction is started
     *
     * @param handle the handle the transaction is being started on
     */
    void begin(Handle handle); // TODO consider having this return a TransactionStatus

    /**
     * Called when a transaction is committed
     *
     * @param handle the handle the commit is being performed on
     */
    void commit(Handle handle);

    /**
     * Called when a transaction is rolled back
     *
     * @param handle the handle the rollback is being performed on
     */
    void rollback(Handle handle);

    /**
     * Roll back to a named checkpoint
     *
     * @param handle the handle the rollback is being performed on
     * @param name the name of the checkpoint to rollback to
     */
    void rollback(Handle handle, String name);

    /**
     * @param handle the handle to test
     * @return whether the given handle is in a transaction
     */
    boolean isInTransaction(Handle handle);

    /**
     * Create a new checkpoint (savepoint in JDBC terminology)
     *
     * @param handle the handle on which the transaction is being checkpointed
     * @param name The name of the chckpoint, used to rollback to or release late
     */
    void checkpoint(Handle handle, String name);

    /**
     * Release a previously created checkpoint
     *
     * @param handle the handle on which the checkpoint is being released
     * @param checkpointName the checkpoint to release
     */
    void release(Handle handle, String checkpointName);

    /**
     * Run a transaction.
     *
     * @param handle the handle to the database
     * @param callback a callback which will receive the open handle, in a transaction.
     * @param <R> the callback return type
     * @param <X> the exception type thrown by the callback, if any
     *
     * @return the value returned by the callback.
     *
     * @throws X any exception thrown by the callback.
     * @see Handle#inTransaction(TransactionCallback)
     */
    <R, X extends Exception> R inTransaction(Handle handle,
                                             TransactionCallback<R, X> callback) throws X;

    /**
     * Run a transaction.
     *
     * @param handle the handle to the database
     * @param level the isolation level for the transaction
     * @param callback a callback which will receive the open handle, in a transaction.
     * @param <R> the callback return type
     * @param <X> the exception type thrown by the callback, if any
     *
     * @return the value returned by the callback.
     *
     * @throws X any exception thrown by the callback.
     * @see Handle#inTransaction(TransactionIsolationLevel, TransactionCallback)
     */
    <R, X extends Exception> R inTransaction(Handle handle,
                                             TransactionIsolationLevel level,
                                             TransactionCallback<R, X> callback) throws X;

}
