/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2.tweak.transactions;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.ExceptionPolicy;
import org.skife.jdbi.v2.exceptions.TransactionException;
import org.skife.jdbi.v2.tweak.TransactionHandler;

import java.sql.SQLException;

/**
 * Handler designed to behave properly in a J2EE CMT environment. It will never
 * explicitely begin or commit a transaction, and will throw a runtime exception
 * when rollback is called to force rollback.
 */
public class CMTTransactionHandler implements TransactionHandler
{
    /**
     * Called when a transaction is started
     */
    public void begin(Handle handle)
    {
        // noop
    }

    /**
     * Called when a transaction is committed
     */
    public void commit(Handle handle)
    {
        // noop
    }

    /**
     * Called when a transaction is rolled back
     * Will throw a RuntimeException to force transactional rollback
     */
    public void rollback(Handle handle)
    {
        throw handle.getExceptionPolicy().transactionException("Rollback called, this runtime exception thrown to halt the transaction");
    }

    /**
     * Roll back to a named checkpoint
     *
     * @param handle the handle the rollback is being performed on
     * @param name   the name of the checkpoint to rollback to
     */
    public void rollback(Handle handle, String name)
    {
        throw new UnsupportedOperationException("Checkpoints not implemented");
    }

    /**
     * Called to test if a handle is in a transaction
     */
    public boolean isInTransaction(Handle handle)
    {
        try
        {
            return ! handle.getConnection().getAutoCommit();
        }
        catch (SQLException e)
        {
            throw handle.getExceptionPolicy().transactionException("Failed to check status of transaction", e);
        }
    }

    /**
     * Create a new checkpoint (savepoint in JDBC terminology)
     *
     * @param handle the handle on which the transaction is being checkpointed
     * @param name   The name of the chckpoint, used to rollback to or release late
     */
    public void checkpoint(Handle handle, String name)
    {
        throw new UnsupportedOperationException("Checkpoints not implemented");
    }

    /**
     * Release a previously created checkpoint
     *
     * @param handle         the handle on which the checkpoint is being released
     * @param checkpointName the checkpoint to release
     */
    public void release(Handle handle, String checkpointName)
    {
        throw handle.getExceptionPolicy().transactionException("Rollback called, this runtime exception thrown to halt the transaction");
    }

    private class ExplodingTransactionStatus implements TransactionStatus
    {
        private final Handle handle;

        ExplodingTransactionStatus(Handle handle)
        {
            this.handle = handle;
        }

        @Override
        public void setRollbackOnly()
        {
            rollback(handle);
        }
    }

    @Override
    public <ReturnType> ReturnType inTransaction(final Handle handle, TransactionCallback<ReturnType> callback)
    {
        try
        {
            return callback.inTransaction(handle, new ExplodingTransactionStatus(handle));
        } catch (Exception e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            throw handle.getExceptionPolicy().transactionException(e);
        }
    }

    @Override
    public <ReturnType> ReturnType inTransaction(Handle handle, TransactionIsolationLevel level,
            TransactionCallback<ReturnType> callback)
    {
        return inTransaction(handle, callback);
    }
}
