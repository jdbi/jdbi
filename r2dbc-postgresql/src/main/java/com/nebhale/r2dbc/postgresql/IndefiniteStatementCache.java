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

import com.nebhale.r2dbc.postgresql.client.Client;
import com.nebhale.r2dbc.postgresql.client.ExtendedQueryMessageFlow;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class IndefiniteStatementCache implements StatementCache {

    private final Map<String, Mono<String>> cache = new ConcurrentHashMap<>();

    private final Client client;

    private final AtomicInteger counter = new AtomicInteger();

    IndefiniteStatementCache(Client client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public Mono<String> getName(String sql) {
        Objects.requireNonNull(sql, "sql must not be null");

        return this.cache.computeIfAbsent(sql, this::parse);
    }

    @Override
    public String toString() {
        return "IndefiniteStatementCache{" +
            "cache=" + this.cache +
            ", client=" + this.client +
            ", counter=" + this.counter +
            '}';
    }

    private Mono<String> parse(String sql) {
        String name = String.format("S_%d", this.counter.getAndIncrement());

        return ExtendedQueryMessageFlow.parse(this.client, name, sql)
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then(Mono.just(name))
            .cache();
    }

}
