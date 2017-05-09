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

/**
 * Callback that expects an open transaction and produces a result.
 *
 * @param <R> the result type produced
 * @param <T> the SqlObject type to provide
 * @param <X> exception thrown
 */
public interface TransactionalCallback<R, T extends Transactional<T>, X extends Exception>
{
    /**
     * Execute in a transaction. Will be committed afterwards, or rolled back if an exception is thrown.
     *
     * @param transactional The object communicating with the database.
     * @return the transaction result
     * @throws X any exception thrown will cause the transaction to be rolled back
     */
    R inTransaction(T transactional) throws X;
}
