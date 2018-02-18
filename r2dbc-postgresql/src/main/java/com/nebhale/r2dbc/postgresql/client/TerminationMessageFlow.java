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

import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.Terminate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.Objects.requireNonNull;

/**
 * A utility class that encapsulates the <a href="https://www.postgresql.org/docs/10/static/protocol-flow.html#idm46428663883648">Termination</a> message flow.
 */
public final class TerminationMessageFlow {

    private TerminationMessageFlow() {
    }

    /**
     * Execute the <a href="https://www.postgresql.org/docs/10/static/protocol-flow.html#idm46428663883648">Termination</a> message flow.
     *
     * @param client the {@link Client} to exchange messages with
     * @return the messages received in response to this exchange
     * @throws NullPointerException if {@code client} is {@code null}
     */
    public static Flux<BackendMessage> exchange(Client client) {
        requireNonNull(client, "client must not be null");

        return client.exchange(Mono.just(Terminate.INSTANCE));
    }

}
