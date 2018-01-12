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

import com.nebhale.r2dbc.postgresql.authentication.AuthenticationHandler;
import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationMessage;
import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationOk;
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.StartupMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Objects;

/**
 * A utility class that encapsulates the <a href="https://www.postgresql.org/docs/10/static/protocol-flow.html#idm46428664018352">Start-up</a> message flow.
 */
public final class StartupMessageFlow {

    private StartupMessageFlow() {
    }

    /**
     * Execute the <a href="https://www.postgresql.org/docs/10/static/protocol-flow.html#idm46428664018352">Start-up</a> message flow.
     *
     * @param applicationName       the name of the application connecting to the server
     * @param authenticationHandler the {@link AuthenticationHandler} to use for authentication
     * @param client                the {@link Client} to exchange messages with
     * @param database              the database to connect to
     * @param username              the username to authenticate with
     * @return the messages received after authentication is complete, in response to this exchange
     * @throws NullPointerException if {@code applicationName}, {@code authenticationHandler}, {@code Client}, or {@code username} is {@code null}
     */
    public static Flux<BackendMessage> exchange(String applicationName, AuthenticationHandler authenticationHandler, Client client, String database, String username) {
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(authenticationHandler, "authenticationHandler must not be null");
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(username, "username must not be null");

        EmitterProcessor<FrontendMessage> requestProcessor = EmitterProcessor.create();
        FluxSink<FrontendMessage> requests = requestProcessor.sink();

        return client.exchange(requestProcessor.startWith(new StartupMessage(applicationName, database, username)))
            .handle((message, sink) -> {
                if (message instanceof AuthenticationOk) {
                    requests.complete();
                } else if (message instanceof AuthenticationMessage) {
                    try {
                        requests.next(authenticationHandler.handle((AuthenticationMessage) message));
                    } catch (Exception e) {
                        requests.error(e);
                        sink.error(e);
                    }
                } else {
                    sink.next(message);
                }
            });
    }

}
