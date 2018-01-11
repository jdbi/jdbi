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

import java.util.function.Function;

/**
 * A single connection to a database.
 *
 * @param <U> the concrete {@link Transaction} type
 */
public interface Connection<U extends Transaction> extends Operations {

    /**
     * Begins a new transaction.
     *
     * @return a {@link Publisher} that indicates that the transaction is open
     */
    Publisher<? extends Transaction> begin();

    /**
     * Release any resources held by the {@link Connection}.
     *
     * @return a {@link Publisher} that termination is complete
     */
    <T> Publisher<T> close();

    /**
     * Configures the default isolation level for transactions created with this connection.
     *
     * @param isolationLevel the default isolation level for transactions created with this connection
     * @return a {@link Publisher} that indicates that a transaction level has been configured
     */
    <T> Publisher<T> setIsolationLevel(IsolationLevel isolationLevel);

    /**
     * Configures the default mutability for transactions created with this connection.
     *
     * @param mutability the mutability for transactions created with this connection
     * @return a {@link Publisher} that indicates that mutability has been configured
     */
    <T> Publisher<T> setMutability(Mutability mutability);

    /**
     * Execute a flow within a transaction.  A successful completion of the flow results in commit and an error results in rollback.
     *
     * @param transaction the flow to execute within the transaction
     * @return a {@link Publisher} that indicates that a transaction is complete
     * @see #begin()
     * @see Transaction#commit()
     * @see Transaction#rollback()
     */
    <T> Publisher<T> withTransaction(Function<U, Publisher<T>> transaction);

}
