/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.client;

import io.r2dbc.client.util.Assert;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.IsolationLevel;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.stream.IntStream;

import static io.r2dbc.client.util.ReactiveUtils.appendError;
import static io.r2dbc.client.util.ReactiveUtils.typeSafe;

/**
 * A wrapper for a {@link Connection} providing additional convenience APIs.
 */
public final class Handle {

    private final Connection connection;

    Handle(Connection connection) {
        this.connection = Assert.requireNonNull(connection, "connection must not be null");
    }

    /**
     * Begins a new transaction.
     *
     * @return a {@link Publisher} that indicates that the transaction is open
     */
    public Publisher<Void> beginTransaction() {
        return this.connection.beginTransaction();
    }

    /**
     * Release any resources held by the {@link Handle}.
     *
     * @return a {@link Publisher} that termination is complete
     */
    public Publisher<Void> close() {
        return this.connection.close();
    }

    /**
     * Commits the current transaction.
     *
     * @return a {@link Publisher} that indicates that a transaction has been committed
     */
    public Publisher<Void> commitTransaction() {
        return this.connection.commitTransaction();
    }

    /**
     * Creates a new {@link Batch} instance for building a batched request.
     *
     * @return a new {@link Batch} instance
     */
    public Batch createBatch() {
        return new Batch(this.connection.createBatch());
    }

    /**
     * Creates a new {@link Query} instance for building a request.
     *
     * @param sql the SQL of the query
     * @return a new {@link Query} instance
     * @throws IllegalArgumentException if {@code sql} is {@code null}
     */
    public Query createQuery(String sql) {
        Assert.requireNonNull(sql, "sql must not be null");

        return new Query(this.connection.createStatement(sql));
    }

    /**
     * Creates a savepoint in the current transaction.
     *
     * @param name the name of the savepoint to create
     * @return a {@link Publisher} that indicates that a savepoint has been created
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public Publisher<Void> createSavepoint(String name) {
        Assert.requireNonNull(name, "name must not be null");

        return this.connection.createSavepoint(name);
    }

    /**
     * Create a new {@link Update} instance for building an updating request.
     *
     * @param sql the SQL of the update
     * @return a new {@link Update} instance
     * @throws IllegalArgumentException if {@code sql} is {@code null}
     */
    public Update createUpdate(String sql) {
        Assert.requireNonNull(sql, "sql must not be null");

        return new Update(this.connection.createStatement(sql));
    }

    /**
     * A convenience method for building and executing an {@link Update}, binding an ordered set of parameters.
     *
     * @param sql        the SQL of the update
     * @param parameters the parameters to bind
     * @return the number of rows that were updated
     * @throws IllegalArgumentException if {@code sql} or {@code parameters} is {@code null}
     */
    public Flux<Integer> execute(String sql, Object... parameters) {
        Assert.requireNonNull(sql, "sql must not be null");
        Assert.requireNonNull(parameters, "parameters must not be null");

        Update update = createUpdate(sql);

        IntStream.range(0, parameters.length)
            .forEach(i -> update.bind(i, parameters[i]));

        return update.add().execute();
    }

    /**
     * Execute behavior within a transaction returning results.  The transaction is committed if the behavior completes successfully, and rolled back it produces an error.
     *
     * @param resourceFunction a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of results
     * @param <T>              the type of results
     * @return a {@link Flux} of results
     * @throws IllegalArgumentException if {@code resourceFunction} is {@code null}
     * @see Connection#commitTransaction()
     * @see Connection#rollbackTransaction()
     */
    @SuppressWarnings("unchecked")
    public <T> Flux<T> inTransaction(Function<Handle, ? extends Publisher<? extends T>> resourceFunction) {
        Assert.requireNonNull(resourceFunction, "resourceFunction must not be null");

        return Mono.from(
            beginTransaction())
            .thenMany((Publisher<T>) resourceFunction.apply(this))
            .concatWith(typeSafe(this::commitTransaction))
            .onErrorResume(appendError(this::rollbackTransaction));
    }

    /**
     * Execute behavior within a transaction returning results.  The transaction is committed if the behavior completes successfully, and rolled back it produces an error.
     *
     * @param isolationLevel   the isolation level of the transaction
     * @param resourceFunction a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of results
     * @param <T>              the type of results
     * @return a {@link Flux} of results
     * @throws IllegalArgumentException if {@code resourceFunction} is {@code null}
     * @see Connection#setTransactionIsolationLevel(IsolationLevel)
     * @see Connection#commitTransaction()
     * @see Connection#rollbackTransaction()
     */
    @SuppressWarnings("unchecked")
    public <T> Flux<T> inTransaction(IsolationLevel isolationLevel, Function<Handle, ? extends Publisher<? extends T>> resourceFunction) {
        Assert.requireNonNull(isolationLevel, "isolationLevel must not be null");
        Assert.requireNonNull(resourceFunction, "resourceFunction must not be null");

        return inTransaction(handle -> Flux.from(handle
            .setTransactionIsolationLevel(isolationLevel))
            .thenMany((Publisher<T>) resourceFunction.apply(this)));
    }

    /**
     * Releases a savepoint in the current transaction.
     *
     * @param name the name of the savepoint to release
     * @return a {@link Publisher} that indicates that a savepoint has been released
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public Publisher<Void> releaseSavepoint(String name) {
        Assert.requireNonNull(name, "name must not be null");

        return this.connection.releaseSavepoint(name);
    }

    /**
     * Rolls back the current transaction.
     *
     * @return a {@link Publisher} that indicates that a transaction has been rolled back
     */
    public Publisher<Void> rollbackTransaction() {
        return this.connection.rollbackTransaction();
    }

    /**
     * Rolls back to a savepoint in the current transaction.
     *
     * @param name the name of the savepoint to rollback to
     * @return a {@link Publisher} that indicates that a savepoint has been rolled back to
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public Publisher<Void> rollbackTransactionToSavepoint(String name) {
        Assert.requireNonNull(name, "name must not be null");

        return this.connection.rollbackTransactionToSavepoint(name);
    }

    /**
     * A convenience method for building a {@link Query}, binding an ordered set of parameters.
     *
     * @param sql        the SQL of the query
     * @param parameters the parameters to bind
     * @return a new {@link Query} instance
     * @throws IllegalArgumentException if {@code sql} or {@code parameters} is {@code null}
     */
    public Query select(String sql, Object... parameters) {
        Assert.requireNonNull(sql, "sql must not be null");
        Assert.requireNonNull(parameters, "parameters must not be null");

        Query query = createQuery(sql);

        IntStream.range(0, parameters.length)
            .forEach(i -> query.bind(i, parameters[i]));

        return query.add();
    }

    /**
     * Configures the isolation level for the current transaction.
     *
     * @param isolationLevel the isolation level for this transaction
     * @return a {@link Publisher} that indicates that a transaction level has been configured
     * @throws IllegalArgumentException if {@code isolationLevel} is {@code null}
     */
    public Publisher<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel) {
        Assert.requireNonNull(isolationLevel, "isolationLevel must not be null");

        return this.connection.setTransactionIsolationLevel(isolationLevel);
    }

    @Override
    public String toString() {
        return "Handle{" +
            "connection=" + this.connection +
            '}';
    }

    /**
     * Execute behavior within a transaction not returning results.  The transaction is committed if the behavior completes successfully, and rolled back it produces an error.
     *
     * @param resourceFunction a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of results.  These results are discarded.
     * @return a {@link Mono} that execution is complete
     * @throws IllegalArgumentException if {@code resourceFunction} is {@code null}
     * @see Connection#commitTransaction()
     * @see Connection#rollbackTransaction()
     */
    public Mono<Void> useTransaction(Function<Handle, ? extends Publisher<?>> resourceFunction) {
        Assert.requireNonNull(resourceFunction, "resourceFunction must not be null");

        return inTransaction(resourceFunction)
            .then();
    }

    /**
     * Execute behavior within a transaction not returning results.  The transaction is committed if the behavior completes successfully, and rolled back it produces an error.
     *
     * @param isolationLevel   the isolation level of the transaction
     * @param resourceFunction a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of results.  These results are discarded.
     * @return a {@link Mono} that execution is complete
     * @throws IllegalArgumentException if {@code isolationLevel} or {@code resourceFunction} is {@code null}
     * @see Connection#setTransactionIsolationLevel(IsolationLevel)
     * @see Connection#commitTransaction()
     * @see Connection#rollbackTransaction()
     */
    public Mono<Void> useTransaction(IsolationLevel isolationLevel, Function<Handle, ? extends Publisher<?>> resourceFunction) {
        Assert.requireNonNull(isolationLevel, "isolationLevel must not be null");
        Assert.requireNonNull(resourceFunction, "resourceFunction must not be null");

        return inTransaction(isolationLevel, resourceFunction)
            .then();
    }

}
