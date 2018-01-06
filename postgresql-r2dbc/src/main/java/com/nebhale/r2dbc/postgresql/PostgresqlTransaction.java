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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class PostgresqlTransaction implements Transaction {

    private final Client client;

    private final PostgresqlOperations delegate;

    PostgresqlTransaction(Client client) {
        this.client = client;
        this.delegate = new PostgresqlOperations(this.client);
    }

    @Override
    public Mono<Void> commit() {
        return SimpleQueryMessageFlow
            .exchange(this.client, "COMMIT")
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

    @Override
    public Mono<Void> rollback() {
        return SimpleQueryMessageFlow
            .exchange(this.client, "ROLLBACK")
            .then();
    }

    @Override
    public Mono<Void> setIsolationLevel(IsolationLevel isolationLevel) {
        return SimpleQueryMessageFlow
            .exchange(this.client, String.format("SET TRANSACTION ISOLATION LEVEL %s", isolationLevel.asSql()))
            .then();
    }

    @Override
    public Mono<Void> setMutability(Mutability mutability) {
        return SimpleQueryMessageFlow
            .exchange(this.client, String.format("SET TRANSACTION %s", mutability.asSql()))
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
