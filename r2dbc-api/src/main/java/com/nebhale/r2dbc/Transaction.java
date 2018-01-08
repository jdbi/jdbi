/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nebhale.r2dbc;

import org.reactivestreams.Publisher;

public interface Transaction extends Operations {

    /**
     * Commits the current transaction.
     *
     * @return a {@link Publisher} that indicates that a transaction has been committed
     */
    Publisher<Void> commit();

    /**
     * Creates a savepoint in this transaction.
     *
     * @param name the name of the savepoint to create
     * @return a {@link Publisher} that indicates that a savepoint has been created
     */
    Publisher<Void> createSavepoint(String name);

    /**
     * Releases a savepoint in this transaction.
     *
     * @param name the name of the savepoint to release
     * @return a {@link Publisher} that indicates that a savepoint has been released
     */
    Publisher<Void> releaseSavepoint(String name);

    /**
     * Rolls back the current transaction.
     *
     * @return a {@link Publisher} that indicates that a transaction has been rolled back
     */
    Publisher<Void> rollback();

    /**
     * Rolls back to a savepoint int his transaction.
     *
     * @param name the name of the savepoint to rollback to
     * @return a {@link Publisher} that indicates that a savepoint has been rolled back to
     */
    Publisher<Void> rollbackToSavepoint(String name);

    /**
     * Configures the isolation level for this transaction.
     *
     * @param isolationLevel the isolation level for this transaction
     * @return a {@link Publisher} that indicates that a transaction level has been configured
     */
    Publisher<Void> setIsolationLevel(IsolationLevel isolationLevel);

    /**
     * Configures the mutability for this transaction.
     *
     * @param mutability the mutability for this transaction
     * @return a {@link Publisher} that indicates that mutability has been configured
     */
    Publisher<Void> setMutability(Mutability mutability);

}
