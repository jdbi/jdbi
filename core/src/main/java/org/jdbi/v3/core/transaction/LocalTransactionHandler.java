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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.internal.exceptions.Unchecked;

/**
 * This <code>TransactionHandler</code> uses local JDBC transactions
 * demarcated explicitly on the handle and passed through to be handled
 * directly on the JDBC Connection instance.
 */
public class LocalTransactionHandler implements TransactionHandler {
    private final Map<Handle, BoundLocalTransactionHandler> bound = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void begin(Handle handle) {
        nonspecial(handle).begin(handle);
    }

    @Override
    public void commit(Handle handle) {
        nonspecial(handle).commit(handle);
    }

    @Override
    public void rollback(Handle handle) {
        nonspecial(handle).rollback(handle);
    }

    @Override
    public boolean isInTransaction(Handle handle) {
        return nonspecial(handle).isInTransaction(handle);
    }

    @Override
    public void savepoint(Handle handle, String savepointName) {
        nonspecial(handle).savepoint(handle, savepointName);
    }

    @Override
    public void rollbackToSavepoint(Handle handle, String savepointName) {
        nonspecial(handle).rollbackToSavepoint(handle, savepointName);
    }

    @Override
    public void releaseSavepoint(Handle handle, String savepointName) {
        nonspecial(handle).releaseSavepoint(handle, savepointName);
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle, HandleCallback<R, X> callback) throws X {
        return nonspecial(handle).inTransaction(handle, callback);
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle, TransactionIsolationLevel level, HandleCallback<R, X> callback) throws X {
        return nonspecial(handle).inTransaction(handle, level, callback);
    }

    TransactionHandler nonspecial(Handle handle) {
        return bound.computeIfAbsent(handle, Unchecked.function(BoundLocalTransactionHandler::new));
    }

    public static LocalTransactionHandler binding() {
        return new BindingLocalTransactionHandler();
    }

    static class BindingLocalTransactionHandler extends LocalTransactionHandler {
        @Override
        public TransactionHandler specialize(Handle handle) throws SQLException {
            return new BoundLocalTransactionHandler(handle);
        }
    }

    static class BoundLocalTransactionHandler implements TransactionHandler {
        private final Map<String, Savepoint> savepoints = new HashMap<>();
        private boolean initialAutocommit;
        private boolean didBegin;
        private boolean didTxnRollback;

        BoundLocalTransactionHandler(Handle handle) throws SQLException {
            this.initialAutocommit = handle.getConnection().getAutoCommit();
        }

        @Override
        public void begin(Handle handle) {
            try {
                if (!didBegin) {
                    Connection conn = handle.getConnection(); // NOPMD
                    initialAutocommit = conn.getAutoCommit();
                    didTxnRollback = false;
                    savepoints.clear();
                    conn.setAutoCommit(false);
                    didBegin = true;
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
            didTxnRollback = true;
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
            @SuppressWarnings("PMD.CloseResource")
            final Connection conn = handle.getConnection();
            try {
                final Savepoint savepoint = conn.setSavepoint(name);
                savepoints.put(name, savepoint);
            } catch (SQLException e) {
                throw new TransactionException(String.format("Unable to create savepoint '%s'", name), e);
            }
        }

        @Override
        public void releaseSavepoint(Handle handle, String name) {
            @SuppressWarnings("PMD.CloseResource")
            final Connection conn = handle.getConnection();
            try {
                final Savepoint savepoint = savepoints.remove(name);
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
            @SuppressWarnings("PMD.CloseResource")
            final Connection conn = handle.getConnection();
            try {
                final Savepoint savepoint = savepoints.remove(name);
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

        @Override
        public <R, X extends Exception> R inTransaction(Handle handle,
                                                        HandleCallback<R, X> callback) throws X {
            if (isInTransaction(handle)) {
                throw new IllegalStateException("Already in transaction");
            }
            didTxnRollback = false;
            final R returnValue;
            try {
                handle.begin();
                returnValue = callback.withHandle(handle);
                if (!didTxnRollback) {
                    handle.commit();
                }
            } catch (Throwable e) {
                try {
                    handle.rollback();
                } catch (Exception rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            } finally {
                didTxnRollback = false;
            }

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

        private void restoreAutoCommitState(Handle handle) {
            try {
                if (initialAutocommit) {
                    handle.getConnection().setAutoCommit(initialAutocommit);
                    savepoints.clear();
                    didBegin = false;
                }
            } catch (SQLException e) {
                throw new UnableToRestoreAutoCommitStateException(e);
            }
        }
    }
}
