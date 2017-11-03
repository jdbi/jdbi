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
package org.skife.jdbi.v2.tweak.transactions;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.TransactionException;
import org.skife.jdbi.v2.exceptions.TransactionFailedException;
import org.skife.jdbi.v2.exceptions.UnableToRestoreAutoCommitStateException;
import org.skife.jdbi.v2.tweak.TransactionHandler;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This <code>TransactionHandler</code> uses local JDBC transactions
 * demarcated explicitly on the handle and passed through to be handled
 * directly on the JDBC Connection instance.
 */
public class LocalTransactionHandler implements TransactionHandler
{
    private final ConcurrentHashMap<Handle, LocalStuff> localStuff = new ConcurrentHashMap<Handle, LocalStuff>();

    /**
     * Called when a transaction is started
     */
    @Override
    public void begin(Handle handle)
    {
        try {
            if (!localStuff.containsKey(handle)) {
                boolean initial = handle.getConnection().getAutoCommit();
                localStuff.putIfAbsent(handle, new LocalStuff(initial));
                handle.getConnection().setAutoCommit(false);
            }
        }
        catch (SQLException e) {
            throw new TransactionException("Failed to start transaction", e);
        }
    }

    /**
     * Called when a transaction is committed
     */
    @Override
    public void commit(Handle handle)
    {
        try {
            handle.getConnection().commit();
        }
        catch (SQLException e) {
            throw new TransactionException("Failed to commit transaction", e);
        }
        finally {
            restoreAutoCommitState(handle);
        }
    }

    /**
     * Called when a transaction is rolled back
     */
    @Override
    public void rollback(Handle handle)
    {
        try {
            handle.getConnection().rollback();
        }
        catch (SQLException e) {
            throw new TransactionException("Failed to rollback transaction", e);
        }
        finally {
            restoreAutoCommitState(handle);
        }
    }

    /**
     * Create a new checkpoint (savepoint in JDBC terminology)
     *
     * @param handle the handle on which the transaction is being checkpointed
     * @param name   The name of the chckpoint, used to rollback to or release late
     */
    @Override
    public void checkpoint(Handle handle, String name)
    {
        final Connection conn = handle.getConnection();
        try {
            final Savepoint savepoint = conn.setSavepoint(name);
            localStuff.get(handle).getCheckpoints().put(name, savepoint);
        }
        catch (SQLException e) {
            throw new TransactionException(String.format("Unable to create checkpoint %s", name), e);
        }
    }

    @Override
    public void release(Handle handle, String name)
    {
        final Connection conn = handle.getConnection();
        try {
            final Savepoint savepoint = localStuff.get(handle).getCheckpoints().remove(name);
            if (savepoint == null) {
                throw new TransactionException(String.format("Attempt to rollback to non-existant savepoint, '%s'",
                                                             name));
            }
            conn.releaseSavepoint(savepoint);
        }
        catch (SQLException e) {
            throw new TransactionException(String.format("Unable to create checkpoint %s", name), e);
        }
    }

    /**
     * Roll back to a named checkpoint
     *
     * @param handle the handle the rollback is being performed on
     * @param name   the name of the checkpoint to rollback to
     */
    @Override
    public void rollback(Handle handle, String name)
    {
        final Connection conn = handle.getConnection();
        try {
            final Savepoint savepoint = localStuff.get(handle).getCheckpoints().remove(name);
            if (savepoint == null) {
                throw new TransactionException(String.format("Attempt to rollback to non-existant savepoint, '%s'",
                                                             name));
            }
            conn.rollback(savepoint);
        }
        catch (SQLException e) {
            throw new TransactionException(String.format("Unable to create checkpoint %s", name), e);
        }
    }

    /**
     * Called to test if a handle is in a transaction
     */
    @Override
    public boolean isInTransaction(Handle handle)
    {
        return !localStuff.containsKey(handle);
    }

    @Override
    public <ReturnType> ReturnType inTransaction(Handle handle, TransactionCallback<ReturnType> callback)
    {
        final AtomicBoolean failed = new AtomicBoolean(false);
        TransactionStatus status = new TransactionStatus()
        {
            @Override
            public void setRollbackOnly()
            {
                failed.set(true);
            }
        };
        final ReturnType returnValue;
        try {
            handle.begin();
            returnValue = callback.inTransaction(handle, status);
            if (!failed.get()) {
                handle.commit();
            }
        }
        catch (RuntimeException e) {
            try {
                handle.rollback();
            } catch (Exception rollback) {
                suppressOrLog(e, rollback);
            }
            throw e;
        }
        catch (Exception e) {
            try {
                handle.rollback();
            } catch (Exception rollback) {
                suppressOrLog(e, rollback);
            }
            throw new TransactionFailedException("Transaction failed do to exception being thrown " +
                                                 "from within the callback. See cause " +
                                                 "for the original exception.", e);
        }
        if (failed.get()) {
            handle.rollback();
            throw new TransactionFailedException("Transaction failed due to transaction status being set " +
                                                 "to rollback only.");
        }
        else {
            return returnValue;
        }
    }

    @Override
    public <ReturnType> ReturnType inTransaction(Handle handle, TransactionIsolationLevel level,
            TransactionCallback<ReturnType> callback)
    {
        final TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();
        try {
            handle.setTransactionIsolation(level);
            return inTransaction(handle, callback);
        }
        finally {
            handle.setTransactionIsolation(initial);
        }
    }

    private void restoreAutoCommitState(final Handle handle) {
        try {
            final LocalStuff stuff = localStuff.remove(handle);
            if (stuff != null) {
                handle.getConnection().setAutoCommit(stuff.getInitialAutocommit());
                stuff.getCheckpoints().clear();
            }
        } catch (SQLException e) {
            throw new UnableToRestoreAutoCommitStateException(e);
        } finally {
            // prevent memory leak if rollback throws an exception
            localStuff.remove(handle);
        }
    }


    private static class LocalStuff
    {
        private final Map<String, Savepoint> checkpoints = new HashMap<String, Savepoint>();
        private final boolean initialAutocommit;

        LocalStuff(boolean initial)
        {
            this.initialAutocommit = initial;
        }

        public Map<String, Savepoint> getCheckpoints()
        {
            return checkpoints;
        }

        public boolean getInitialAutocommit()
        {
            return initialAutocommit;
        }
    }

    /** Java 7 or up: add inner exception as suppressed.  Java 6 just log
     * and move on with life. */ // TODO: jdbi3 fixup
    private void suppressOrLog(Throwable outer, Throwable suppressed) {
        try {
            Throwable.class.getMethod("addSuppressed", Throwable.class).invoke(outer, suppressed);
            return;
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        System.err.println("Exception caught while attempting connection rollback in LocalTransactionHandler;"
                + "in Java 7 or later we would 'suppress' it but since we can't we'll log it here:");
        suppressed.printStackTrace();
    }
}
