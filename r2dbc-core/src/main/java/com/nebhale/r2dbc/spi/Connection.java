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

package com.nebhale.r2dbc.spi;

import org.reactivestreams.Publisher;

/**
 * A single connection to a database.
 */
public interface Connection {

    /**
     * Begins a new transaction.
     *
     * @return a {@link Publisher} that indicates that the transaction is open
     */
    Publisher<Void> beginTransaction();

    /**
     * Release any resources held by the {@link Connection}.
     *
     * @return a {@link Publisher} that termination is complete
     */
    Publisher<Void> close();

    /**
     * Commits the current transaction.
     *
     * @return a {@link Publisher} that indicates that a transaction has been committed
     */
    Publisher<Void> commitTransaction();

    /**
     * Creates a new {@link Batch} instance for building a batched request.
     *
     * @return a new {@link Batch} instance
     */
    Batch createBatch();

    /**
     * Creates a savepoint in the current transaction.
     *
     * @param name the name of the savepoint to create
     * @return a {@link Publisher} that indicates that a savepoint has been created
     */
    Publisher<Void> createSavepoint(String name);

    /**
     * Creates a new statement for building a statement-based request.
     *
     * @param sql the SQL of the statement
     * @return a new {@link Statement} instance
     */
    Statement createStatement(String sql);

    /**
     * Releases a savepoint in the current transaction.
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
    Publisher<Void> rollbackTransaction();

    /**
     * Rolls back to a savepoint in the current transaction.
     *
     * @param name the name of the savepoint to rollback to
     * @return a {@link Publisher} that indicates that a savepoint has been rolled back to
     */
    Publisher<Void> rollbackTransactionToSavepoint(String name);

    /**
     * Configures the isolation level for the current transaction.
     *
     * @param isolationLevel the isolation level for this transaction
     * @return a {@link Publisher} that indicates that a transaction level has been configured
     */
    Publisher<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel);

    /**
     * Configures the mutability for the current transaction.
     *
     * @param mutability the mutability for this transaction
     * @return a {@link Publisher} that indicates that mutability has been configured
     */
    Publisher<Void> setTransactionMutability(Mutability mutability);

}
