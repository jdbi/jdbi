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
package org.jdbi.v3.sqlobject.transaction;

import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.SqlObject;

/**
 * A mixin interface to expose transaction methods on the sql object.
 * <p>
 * Use caution with {@link org.jdbi.v3.core.Jdbi#onDemand(Class) on-demand} {@code Transactional} instances.
 * {@link org.jdbi.v3.core.Handle} throws {@link TransactionException} if closed while a
 * transaction is open. Since on-demand extensions open and close a handle around each method invocation, calling
 * {@link #begin()} on an on-demand {@code Transactional} will always leave a transaction open, and thus
 * <em>always</em> throw this exception.
 * <p>
 * Users of on-demand {@code Transactional} instances should use the {@code inTransaction} and {@code useTransaction}
 * methods to execute transactions. It is safe to call other {@code Transactional} methods from inside these callbacks.
 *
 * @param <This> must match the interface that is extending this one.
 */
public interface Transactional<This extends Transactional<This>> extends SqlObject {
    /**
     * Begins a transaction.
     *
     * @throws TransactionException if called on an on-demand Transactional instance.
     */
    default void begin() {
        getHandle().begin();
    }

    /**
     * Commits the open transaction.
     */
    default void commit() {
        getHandle().commit();
    }

    /**
     * Rolls back the open transaction.
     */
    default void rollback() {
        getHandle().rollback();
    }

    /**
     * Creates a savepoint with the given name on the transaction.
     *
     * @param savepointName the savepoint name.
     */
    default void savepoint(String savepointName) {
        getHandle().savepoint(savepointName);
    }

    /**
     * Rolls back to the given savepoint.
     *
     * @param savepointName the savepoint name.
     */
    default void rollbackToSavepoint(String savepointName) {
        getHandle().rollbackToSavepoint(savepointName);
    }

    /**
     * Releases the given savepoint.
     *
     * @param savepointName the savepoint name.
     */
    default void releaseSavepoint(String savepointName) {
        getHandle().release(savepointName);
    }

    /**
     * Executes the given callback within a transaction, returning the value returned by the callback.
     *
     * @param callback the callback to execute
     * @param <R>      method return type
     * @param <X>      exception optionally thrown by the callback.
     * @return the value returned by the callback.
     * @throws X any exception thrown by the callback.
     */
    @SuppressWarnings("unchecked")
    default <R, X extends Exception> R inTransaction(TransactionalCallback<R, This, X> callback) throws X {
        return withHandle(h -> h.inTransaction(txn -> callback.inTransaction((This) this)));
    }

    /**
     * Executes the given callback within a transaction, returning the value returned by the callback.
     *
     * @param isolation the transaction isolation level.
     * @param callback  the callback to execute
     * @param <R>       method return type
     * @param <X>       exception optionally thrown by the callback.
     * @return the value returned by the callback.
     * @throws X any exception thrown by the callback.
     */
    @SuppressWarnings("unchecked")
    default <R, X extends Exception> R inTransaction(
            TransactionIsolationLevel isolation, TransactionalCallback<R, This, X> callback) throws X {
        return withHandle(h -> h.inTransaction(isolation, txn -> callback.inTransaction((This) this)));
    }

    /**
     * Executes the given callback within a transaction.
     *
     * @param callback the callback to execute
     * @param <X>      exception optionally thrown by the callback.
     * @throws X any exception thrown by the callback.
     */
    default <X extends Exception> void useTransaction(TransactionalConsumer<This, X> callback) throws X {
        inTransaction(callback.asCallback());
    }

    /**
     * Executes the given callback within a transaction.
     *
     * @param isolation the transaction isolation level.
     * @param callback  the callback to execute
     * @param <X>       exception optionally thrown by the callback.
     * @throws X any exception thrown by the callback.
     */
    default <X extends Exception> void useTransaction(TransactionIsolationLevel isolation,
                                                      TransactionalConsumer<This, X> callback) throws X {
        inTransaction(isolation, callback.asCallback());
    }
}
