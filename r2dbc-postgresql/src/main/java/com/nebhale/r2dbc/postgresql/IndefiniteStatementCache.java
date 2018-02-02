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

import com.nebhale.r2dbc.postgresql.client.Binding;
import com.nebhale.r2dbc.postgresql.client.Client;
import com.nebhale.r2dbc.postgresql.client.ExtendedQueryMessageFlow;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;
import static reactor.function.TupleUtils.function;

final class IndefiniteStatementCache implements StatementCache {

    private final Map<Tuple2<String, List<Integer>>, Mono<String>> cache = new HashMap<>();

    private final Client client;

    private final AtomicInteger counter = new AtomicInteger();

    IndefiniteStatementCache(Client client) {
        this.client = requireNonNull(client, "client must not be null");
    }

    @Override
    public Mono<String> getName(Binding binding, String sql) {
        requireNonNull(binding, "binding must not be null");
        requireNonNull(sql, "sql must not be null");

        return this.cache.computeIfAbsent(Tuples.of(sql, binding.getParameterTypes()), function(this::parse));
    }

    @Override
    public String toString() {
        return "IndefiniteStatementCache{" +
            "cache=" + this.cache +
            ", client=" + this.client +
            ", counter=" + this.counter +
            '}';
    }

    private Mono<String> parse(String sql, List<Integer> types) {
        String name = String.format("S_%d", this.counter.getAndIncrement());

        return ExtendedQueryMessageFlow
            .parse(this.client, name, sql, types)
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then(Mono.just(name))
            .cache();
    }

}
