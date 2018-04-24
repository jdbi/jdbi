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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;

/**
 * This <code>TransactionHandler</code> uses local JDBC transactions
 * demarcated explicitly on the handle and passed through to be handled
 * directly on the JDBC Connection instance.
 */
public class LocalTransactionHandler implements TransactionHandler {
    private final ConcurrentHashMap<Handle, LocalStuff> localStuff = new ConcurrentHashMap<>();
    private final ThreadLocal<Boolean> didTxnRollback = ThreadLocal.withInitial(() -> false);

    @Override
    public void begin(Handle handle) {
        try {
            if (!localStuff.containsKey(handle)) {
                boolean initial = handle.getConnection().getAutoCommit();
                localStuff.putIfAbsent(handle, new LocalStuff(initial));
                handle.getConnection().setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new TransactionException("Failed to start transaction", e);
        }
    }

    @Override
    public void commit(Handle handle) {
        try {
            handle.getConnection().commit();
        } catch (SQLException e) {
            throw new TransactionException("Failed to commit transaction", e);
        } finally {
            restoreAutoCommitState(handle);
        }
    }

    @Override
    public void rollback(Handle handle) {
        didTxnRollback.set(true);
        try {
            handle.getConnection().rollback();
        } catch (SQLException e) {
            throw new TransactionException("Failed to rollback transaction", e);
        } finally {
            restoreAutoCommitState(handle);
        }
    }

    @Override
    public void savepoint(Handle handle, String name) {
        final Connection conn = handle.getConnection();
        try {
            final Savepoint savepoint = conn.setSavepoint(name);
            localStuff.get(handle).getSavepoints().put(name, savepoint);
        } catch (SQLException e) {
            throw new TransactionException(String.format("Unable to create savepoint '%s'", name), e);
        }
    }

    @Override
    public void releaseSavepoint(Handle handle, String name) {
        final Connection conn = handle.getConnection();
        try {
            final Savepoint savepoint = localStuff.get(handle).getSavepoints().remove(name);
            if (savepoint == null) {
                throw new TransactionException(String.format("Attempt to release non-existent savepoint, '%s'",
                                                             name));
            }
            conn.releaseSavepoint(savepoint);
        } catch (SQLException e) {
            throw new TransactionException(String.format("Unable to create savepoint %s", name), e);
        }
    }

    @Override
    public void rollbackToSavepoint(Handle handle, String name) {
        final Connection conn = handle.getConnection();
        try {
            final Savepoint savepoint = localStuff.get(handle).getSavepoints().remove(name);
            if (savepoint == null) {
                throw new TransactionException(String.format("Attempt to rollback to non-existent savepoint, '%s'",
                                                             name));
            }
            conn.rollback(savepoint);
        } catch (SQLException e) {
            throw new TransactionException(String.format("Unable to create savepoint %s", name), e);
        }
    }

    @Override
    public boolean isInTransaction(Handle handle) {
        try {
            return !handle.getConnection().getAutoCommit();
        } catch (SQLException e) {
            throw new TransactionException("Failed to test for transaction status", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    HandleCallback<R, X> callback) throws X {
        if (isInTransaction(handle)) {
            throw new IllegalStateException("Already in transaction");
        }
        didTxnRollback.set(false);
        final R returnValue;
        try {
            handle.begin();
            returnValue = callback.withHandle(handle);
            if (!didTxnRollback.get()) {
                handle.commit();
            }
        } catch (Exception e) {
            try {
                handle.rollback();
            } catch (Exception rollback) {
                e.addSuppressed(rollback);
            }
            throw (X) e;
        }

        didTxnRollback.remove();
        return returnValue;
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    TransactionIsolationLevel level,
                                                    HandleCallback<R, X> callback) throws X {
        final TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();
        try {
            handle.setTransactionIsolation(level);
            return inTransaction(handle, callback);
        } finally {
            handle.setTransactionIsolation(initial);
        }
    }

    private void restoreAutoCommitState(final Handle handle) {
        try {
            final LocalStuff stuff = localStuff.remove(handle);
            if (stuff != null) {
                handle.getConnection().setAutoCommit(stuff.getInitialAutocommit());
                stuff.getSavepoints().clear();
            }
        } catch (SQLException e) {
            throw new UnableToRestoreAutoCommitStateException(e);
        } finally {
            // prevent memory leak if rollback throws an exception
            localStuff.remove(handle);
        }
    }


    private static class LocalStuff {
        private final Map<String, Savepoint> savepoints = new HashMap<>();
        private final boolean initialAutocommit;

        LocalStuff(boolean initial) {
            this.initialAutocommit = initial;
        }

        Map<String, Savepoint> getSavepoints() {
            return savepoints;
        }

        boolean getInitialAutocommit() {
            return initialAutocommit;
        }
    }
}
