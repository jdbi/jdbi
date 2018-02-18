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
import com.nebhale.r2dbc.spi.Batch;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * An implementation of {@link Batch} for executing a collection of statements in a batch against a PostgreSQL database.
 */
public final class PostgresqlBatch implements Batch {

    private final Client client;

    private final List<String> statements = new ArrayList<>();

    PostgresqlBatch(Client client) {
        this.client = requireNonNull(client, "client must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code sql} is {@code null}
     */
    @Override
    public PostgresqlBatch add(String sql) {
        requireNonNull(sql, "sql must not be null");

        if (!SimpleQueryPostgresqlStatement.supports(sql)) {
            throw new IllegalArgumentException(String.format("Statement '%s' is not supported.  This is often due to the presence of parameters.", sql));
        }

        this.statements.add(sql);
        return this;
    }

    @Override
    public Flux<PostgresqlResult> execute() {
        return new SimpleQueryPostgresqlStatement(this.client, this.statements.stream().collect(Collectors.joining("; ")))
            .execute();
    }

    @Override
    public String toString() {
        return "PostgresqlBatch{" +
            "client=" + this.client +
            ", statements=" + this.statements +
            '}';
    }

}
