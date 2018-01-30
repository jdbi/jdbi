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

package com.nebhale.r2dbc.postgresql.client;

import com.nebhale.r2dbc.postgresql.message.Format;
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.backend.ParseComplete;
import com.nebhale.r2dbc.postgresql.message.frontend.Bind;
import com.nebhale.r2dbc.postgresql.message.frontend.Describe;
import com.nebhale.r2dbc.postgresql.message.frontend.Execute;
import com.nebhale.r2dbc.postgresql.message.frontend.ExecutionType;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.Parse;
import com.nebhale.r2dbc.postgresql.message.frontend.Sync;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A utility class that encapsulates the <a href="https://www.postgresql.org/docs/current/static/protocol-flow.html#PROTOCOL-FLOW-EXT-QUERY">Extended query</a> message flow.
 */
public final class ExtendedQueryMessageFlow {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private ExtendedQueryMessageFlow() {
    }

    /**
     * Execute the execute portion of the <a href="https://www.postgresql.org/docs/current/static/protocol-flow.html#PROTOCOL-FLOW-EXT-QUERY">Extended query</a> message flow.
     *
     * @param client             the {@link Client} to exchange messages with
     * @param portalNameSupplier supplier unique portal names for each binding
     * @param statement          the name of the statement to execute
     * @param values             the values to bind and execute
     * @return the messages received in response to the exchange
     * @throws NullPointerException if {@code client}, {@code statement}, or {@code values} is {@code null}
     */
    public static Flux<BackendMessage> execute(Client client, PortalNameSupplier portalNameSupplier, String statement, Publisher<List<ByteBuf>> values) {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(portalNameSupplier, "portalNameSupplier must not be null");
        Objects.requireNonNull(statement, "statement must not be null");
        Objects.requireNonNull(values, "values must not be null");

        return client.exchange(Flux.from(values)
            .flatMap(value -> toBindFlow(portalNameSupplier, statement, value))
            .concatWith(Mono.just(Sync.INSTANCE)));
    }

    /**
     * Execute the parse portion of the <a href="https://www.postgresql.org/docs/current/static/protocol-flow.html#PROTOCOL-FLOW-EXT-QUERY">Extended query</a> message flow.
     *
     * @param client the {@link Client} to exchange messages with
     * @param name   the name of the statement to prepare
     * @param query  the query to execute
     * @return the messages received in response to this exchange
     * @throws NullPointerException if {@code client}, {@code name}, or {@code query} is {@code null}
     */
    public static Flux<BackendMessage> parse(Client client, String name, String query) {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(query, "query must not be null");

        return client.exchange(Flux.just(new Parse(name, Collections.emptyList(), query), Sync.INSTANCE))
            .takeUntil(ParseComplete.class::isInstance);
    }

    private static Flux<FrontendMessage> toBindFlow(PortalNameSupplier portalNameSupplier, String statement, List<ByteBuf> value) {
        String portal = portalNameSupplier.get();

        Bind bind = new Bind(portal, Collections.singletonList(Format.TEXT), value, Collections.emptyList(), statement);

        return Flux.just(bind, new Describe(portal, ExecutionType.PORTAL), new Execute(portal, 0));
    }

}
