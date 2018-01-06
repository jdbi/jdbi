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

import com.nebhale.r2dbc.Connection;
import com.nebhale.r2dbc.IsolationLevel;
import com.nebhale.r2dbc.Mutability;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

final class PostgresqlConnection implements Connection<PostgresqlTransaction> {

    private final Client client;

    private final PostgresqlOperations delegate;

    private final Map<String, String> parameters;

    private final int processId;

    private final int secretKey;

    private PostgresqlConnection(Client client, Map<String, String> parameters, int processId, int secretKey) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.parameters = Objects.requireNonNull(parameters);
        this.processId = processId;
        this.secretKey = secretKey;
        this.delegate = new PostgresqlOperations(this.client);
    }

    @Override
    public Mono<PostgresqlTransaction> begin() {
        return SimpleQueryMessageFlow
            .exchange(this.client, "BEGIN")
            .then(Mono.just(new PostgresqlTransaction(this.client)));
    }

    @Override
    public void close() {
        this.client.close();
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
    public Mono<Void> setIsolationLevel(IsolationLevel isolationLevel) {
        return SimpleQueryMessageFlow
            .exchange(this.client, String.format("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL %s", isolationLevel.asSql()))
            .then();
    }

    @Override
    public Mono<Void> setMutability(Mutability mutability) {
        return SimpleQueryMessageFlow
            .exchange(this.client, String.format("SET SESSION CHARACTERISTICS AS TRANSACTION %s", mutability.asSql()))
            .then();
    }

    @Override
    public String toString() {
        return "PostgresqlConnection{" +
            "client=" + this.client +
            ", delegate=" + this.delegate +
            ", parameters=" + this.parameters +
            ", processId=" + this.processId +
            ", secretKey=" + this.secretKey +
            '}';
    }

    @Override
    public Mono<Void> withTransaction(Function<PostgresqlTransaction, Publisher<Void>> transaction) {
        return begin()
            .flatMap(tx -> Flux
                .from(transaction.apply(tx))
                .thenEmpty(tx.commit())
                .onErrorResume(t -> tx.rollback()));
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {

        private Client client;

        private Map<String, String> parameters = new HashMap<>();

        private int processId;

        private int secretKey;

        private Builder() {
        }

        @Override
        public String toString() {
            return "Builder{" +
                "client=" + this.client +
                ", parameters=" + this.parameters +
                ", processId=" + this.processId +
                ", secretKey=" + this.secretKey +
                '}';
        }

        PostgresqlConnection build() {
            return new PostgresqlConnection(this.client, this.parameters, this.processId, this.secretKey);
        }

        Builder client(Client client) {
            this.client = Objects.requireNonNull(client, "client must not be null");
            return this;
        }

        Builder parameter(String key, String value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");

            this.parameters.put(key, value);
            return this;
        }

        Builder processId(int processId) {
            this.processId = processId;
            return this;
        }

        Builder secretKey(int secretKey) {
            this.secretKey = secretKey;
            return this;
        }

    }

}
