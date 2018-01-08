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

import com.nebhale.r2dbc.Operations;
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.DataRow;
import com.nebhale.r2dbc.postgresql.message.backend.EmptyQueryResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.Objects;

final class PostgresqlOperations implements Operations {

    private final Client client;

    PostgresqlOperations(Client client) {
        this.client = client;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code query} is {@code null}
     */
    @Override
    public Flux<Flux<PostgresqlRow>> query(String query) {
        Objects.requireNonNull(query, "query must not be null");

        return SimpleQueryMessageFlow
            .exchange(this.client, query)
            .handle(PostgresqlOperations::handleEmptyQueryResponse)
            .windowWhile(message -> !(message instanceof CommandComplete))
            .map(flux -> flux
                .filter(DataRow.class::isInstance)
                .ofType(DataRow.class)
                .map(message -> new PostgresqlRow(message.getColumns())));
    }

    @Override
    public String toString() {
        return "PostgresqlOperations{" +
            "client=" + this.client +
            '}';
    }

    private static void handleEmptyQueryResponse(BackendMessage message, SynchronousSink<Object> sink) {
        if (message instanceof EmptyQueryResponse) {
            sink.complete();
        } else {
            sink.next(message);
        }
    }

}
