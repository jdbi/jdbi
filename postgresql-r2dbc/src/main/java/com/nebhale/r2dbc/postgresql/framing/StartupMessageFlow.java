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

package com.nebhale.r2dbc.postgresql.framing;

import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationMD5Password;
import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationOk;
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.PasswordMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.StartupMessage;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Objects;

/**
 * A helper class that encapsulates the <a href="https://www.postgresql.org/docs/current/static/protocol-flow.html#idm46428664018352">Start-up message flow</a>.
 */
public final class StartupMessageFlow {

    private StartupMessageFlow() {
    }

    /**
     * Initiates the message flow.
     *
     * @param applicationName the name of the application
     * @param client          the client to use to send and receive messages
     * @param database        the database to connect to
     * @param password        the password to authenticate with
     * @param username        the username to authenticate with
     * @return the messages received after authentication has completed successfully
     * @throws NullPointerException if {@code applicationName}, {@code client}, {@code password}, or {@code username} is {@code null}
     */
    public static Flux<BackendMessage> exchange(String applicationName, Client client, String database, String password, String username) {
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(username, "username must not be null");

        DirectProcessor<FrontendMessage> requestProcessor = DirectProcessor.create();
        FluxSink<FrontendMessage> requests = requestProcessor.sink();

        return Flux.from(client.exchange(requestProcessor))
            .doOnSubscribe(s -> requests.next(new StartupMessage(applicationName, database, username)))
            .handle((message, sink) -> {
                if (message instanceof AuthenticationOk) {
                    requests.complete();
                } else if (message instanceof AuthenticationMD5Password) {
                    handleAuthenticationMD5Password((AuthenticationMD5Password) message, password, requests, username);
                } else {
                    sink.next(message);
                }
            });
    }

    private static void handleAuthenticationMD5Password(AuthenticationMD5Password message, String password, FluxSink<FrontendMessage> requests, String username) {
        String shadow = new FluentMessageDigest("md5")
            .update("%s%s", password, username)
            .digest();

        String transfer = new FluentMessageDigest("md5")
            .update(shadow)
            .update(message.getSalt())
            .digest();

        requests.next(new PasswordMessage(String.format("md5%s", transfer)));
    }

}
