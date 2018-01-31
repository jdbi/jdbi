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
import com.nebhale.r2dbc.postgresql.client.PortalNameSupplier;
import com.nebhale.r2dbc.postgresql.message.backend.CloseComplete;
import com.nebhale.r2dbc.postgresql.util.ByteBufUtils;
import com.nebhale.r2dbc.postgresql.util.SqlUtils;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

final class ExtendedQueryPostgresqlStatement implements PostgresqlStatement {

    private final List<List<ByteBuf>> bindings = new CopyOnWriteArrayList<>();

    private final Client client;

    private final PortalNameSupplier portalNameSupplier;

    private final String sql;

    private final StatementCache statementCache;

    ExtendedQueryPostgresqlStatement(Client client, PortalNameSupplier portalNameSupplier, String sql, StatementCache statementCache) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.portalNameSupplier = Objects.requireNonNull(portalNameSupplier, "portalNameSupplier must not be null");
        this.sql = Objects.requireNonNull(sql, "sql must not be null");
        this.statementCache = Objects.requireNonNull(statementCache, "statementCache must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code parameters} is {@code null}
     */
    @Override
    public ExtendedQueryPostgresqlStatement bind(Iterable<Object> parameters) {
        Objects.requireNonNull(parameters, "parameters must not be null");

        this.bindings.add(StreamSupport.stream(parameters.spliterator(), false)
            .map(value -> ByteBufUtils.encode(this.client.getByteBufAllocator(), value.toString()))
            .collect(Collectors.toList()));

        return this;
    }

    @Override
    public Flux<PostgresqlResult> execute() {
        return this.statementCache.getName(SqlUtils.replacePlaceholders(this.sql))
            .flatMapMany(name -> ExtendedQueryMessageFlow.execute(this.client, this.portalNameSupplier, name, Flux.fromIterable(this.bindings)))
            .windowUntil(CloseComplete.class::isInstance)
            .map(PostgresqlResult::toResult);
    }

    @Override
    public String toString() {
        return "ExtendedQueryPostgresqlStatement{" +
            "bindings=" + this.bindings +
            ", client=" + this.client +
            ", sql='" + this.sql + '\'' +
            '}';
    }

    static boolean supports(String sql) {
        Objects.requireNonNull(sql, "sql must not be null");

        return !sql.trim().isEmpty() && !sql.contains(";") && sql.contains("?");
    }

}
