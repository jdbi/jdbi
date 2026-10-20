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
package org.jdbi.v3.core.transaction;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;

/**
 * A {@link TransactionHandler} for connections whose transactions are managed
 * completely outside of Jdbi, for example by a proxy data source or an XA
 * transaction manager. This handler never reads or changes the transaction
 * state of the connection:
 * <ul>
 * <li>{@link #begin(Handle)} and {@link #commit(Handle)} do nothing. The
 * external manager decides when a transaction starts and when it commits.</li>
 * <li>{@link #rollback(Handle)} throws a {@link TransactionException}. Jdbi
 * cannot roll the external transaction back, so the exception propagates to
 * the external manager, which then does the rollback.</li>
 * <li>{@link #isInTransaction(Handle)} returns false and does not examine the
 * connection. On a wrapped connection, the autocommit flag does not show the
 * real transaction state, so this handler never reads it.</li>
 * <li>{@link #inTransaction(Handle, HandleCallback)} runs the callback on the
 * handle directly.</li>
 * <li>Savepoints are not supported.</li>
 * </ul>
 * Because the handle never reports an open transaction, the transaction
 * callbacks ({@code Handle.afterCommit}, {@code Handle.afterRollback}) cannot
 * be registered under this handler.
 *
 * @see CMTTransactionHandler
 */
public class NoOpTransactionHandler implements TransactionHandler {
    @Override
    public void begin(Handle handle) {
        // the external manager starts the transaction
    }

    @Override
    public void commit(Handle handle) {
        // the external manager commits the transaction
    }

    /**
     * Called when a transaction is rolled back.
     * Will throw a RuntimeException to force transactional rollback.
     */
    @Override
    public void rollback(Handle handle) {
        throw new TransactionException("Rollback called, this runtime exception thrown to halt the transaction");
    }

    @Override
    public boolean isInTransaction(Handle handle) {
        return false;
    }

    /**
     * Savepoints are not supported.
     */
    @Override
    public void savepoint(Handle handle, String name) {
        throw new UnsupportedOperationException("Savepoints not supported");
    }

    /**
     * Savepoints are not supported.
     */
    @Override
    public void rollbackToSavepoint(Handle handle, String name) {
        throw new UnsupportedOperationException("Savepoints not supported");
    }

    /**
     * Savepoints are not supported.
     */
    @Override
    public void releaseSavepoint(Handle handle, String savepointName) {
        throw new UnsupportedOperationException("Savepoints not supported");
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    HandleCallback<R, X> callback) throws X {
        return callback.withHandle(handle);
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    TransactionIsolationLevel level,
                                                    HandleCallback<R, X> callback) throws X {
        return inTransaction(handle, callback);
    }
}
