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
package org.jdbi.v3.sqlobject.mixins;

import org.jdbi.v3.sqlobject.TransactionalCallback;
import org.jdbi.v3.sqlobject.TransactionalConsumer;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

/**
 * A mixin interface to expose transaction methods on the sql object.
 *
 * @param <This> must match the interface that is extending this one.
 */
public interface Transactional<This extends Transactional<This>>
{
    void begin();

    void commit();

    void rollback();

    void checkpoint(String name);

    void release(String name);

    void rollback(String name);

    <R, X extends Exception> R inTransaction(TransactionalCallback<R, This, X> callback) throws X;

    <R, X extends Exception> R inTransaction(TransactionIsolationLevel isolation,
                                             TransactionalCallback<R, This, X> callback) throws X;

    default <X extends Exception> void useTransaction(TransactionalConsumer<This, X> callback) throws X {
        inTransaction((transactional, status) -> {
            callback.useTransaction(transactional, status);
            return null;
        });
    }

    default <X extends Exception> void useTransaction(TransactionIsolationLevel isolation,
                                                      TransactionalConsumer<This, X> callback) throws X {
        inTransaction(isolation, (transactional, status) -> {
            callback.useTransaction(transactional, status);
            return null;
        });
    }
}
