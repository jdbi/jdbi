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

package com.nebhale.r2dbc.postgresql;

import com.nebhale.r2dbc.IsolationLevel;
import com.nebhale.r2dbc.Mutability;
import com.nebhale.r2dbc.Transaction;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.Util.not;

final class PostgresqlTransaction implements Transaction {

    private final Client client;

    private final PostgresqlOperations delegate;

    PostgresqlTransaction(Client client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.delegate = new PostgresqlOperations(this.client);
    }

    @Override
    public Mono<Void> commit() {
        return SimpleQueryMessageFlow
            .exchange(this.client, "COMMIT")
            .takeWhile(not(CommandComplete.class::isInstance))
            .then();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @Override
    public Mono<Void> createSavepoint(String name) {
        Objects.requireNonNull(name, "name must not be null");

        return SimpleQueryMessageFlow
            .exchange(this.client, String.format("SAVEPOINT %s", name))
            .takeWhile(not(CommandComplete.class::isInstance))
            .then();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code query} is {@code null}
     */
    @Override
    public Flux<Flux<PostgresqlRow>> query(String query) {
        return this.delegate.query(query);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@cod name} is {@code null}
     */
    @Override
    public Mono<Void> releaseSavepoint(String name) {
        Objects.requireNonNull(name, "name must not be null");

        return SimpleQueryMessageFlow
            .exchange(this.client, String.format("RELEASE SAVEPOINT %s", name))
            .takeWhile(not(CommandComplete.class::isInstance))
            .then();
    }

    @Override
    public Mono<Void> rollback() {
        return SimpleQueryMessageFlow
            .exchange(this.client, "ROLLBACK")
            .takeWhile(not(CommandComplete.class::isInstance))
            .then();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @Override
    public Mono<Void> rollbackToSavepoint(String name) {
        Objects.requireNonNull(name, "name must not be null");

        return SimpleQueryMessageFlow
            .exchange(this.client, String.format("ROLLBACK TO SAVEPOINT %s", name))
            .takeWhile(not(CommandComplete.class::isInstance))
            .then();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code isolationLevel} is {@code null}
     */
    @Override
    public Mono<Void> setIsolationLevel(IsolationLevel isolationLevel) {
        Objects.requireNonNull(isolationLevel, "isolationLevel must not be null");

        return SimpleQueryMessageFlow
            .exchange(this.client, String.format("SET TRANSACTION ISOLATION LEVEL %s", isolationLevel.asSql()))
            .takeWhile(not(CommandComplete.class::isInstance))
            .then();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code mutability} is {@code null}
     */
    @Override
    public Mono<Void> setMutability(Mutability mutability) {
        Objects.requireNonNull(mutability, "mutability must not be null");

        return SimpleQueryMessageFlow
            .exchange(this.client, String.format("SET TRANSACTION %s", mutability.asSql()))
            .takeWhile(not(CommandComplete.class::isInstance))
            .then();
    }

    @Override
    public String toString() {
        return "PostgresqlTransaction{" +
            "client=" + client +
            ", delegate=" + delegate +
            '}';
    }

}
