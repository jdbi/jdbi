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

import java.sql.SQLException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;

/**
 * Handler designed to behave properly in a J2EE CMT environment. It will never
 * explicitly begin or commit a transaction, and will throw a runtime exception
 * when rollback is called to force rollback.
 */
public class CMTTransactionHandler implements TransactionHandler {
    /**
     * Called when a transaction is started
     */
    @Override
    public void begin(Handle handle) {
        // noop
    }

    /**
     * Called when a transaction is committed
     */
    @Override
    public void commit(Handle handle) {
        // noop
    }

    /**
     * Called when a transaction is rolled back
     * Will throw a RuntimeException to force transactional rollback
     */
    @Override
    public void rollback(Handle handle) {
        throw new TransactionException("Rollback called, this runtime exception thrown to halt the transaction");
    }

    /**
     * Savepoints are not supported.
     */
    @Override
    public void rollbackToSavepoint(Handle handle, String name) {
        throw new UnsupportedOperationException("Savepoints not supported");
    }

    /**
     * Called to test if a handle is in a transaction
     */
    @Override
    public boolean isInTransaction(Handle handle) {
        try {
            return !handle.getConnection().getAutoCommit();
        } catch (SQLException e) {
            throw new TransactionException("Failed to check status of transaction", e);
        }
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
