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
import com.nebhale.r2dbc.ConnectionFactory;
import com.nebhale.r2dbc.postgresql.framing.Client;
import com.nebhale.r2dbc.postgresql.framing.StartupMessageFlow;
import com.nebhale.r2dbc.postgresql.framing.TcpClientClient;
import com.nebhale.r2dbc.postgresql.message.backend.BackendKeyData;
import com.nebhale.r2dbc.postgresql.message.backend.DefaultBackendMessageDecoder;
import com.nebhale.r2dbc.postgresql.message.backend.ParameterStatus;
import org.reactivestreams.Publisher;

import java.util.Objects;

/**
 * An implementation of {@link ConnectionFactory} for creating connections to a PostgreSQL database.
 */
public final class PostgresqlConnectionFactory implements ConnectionFactory {

    private final PostgresqlConnectionConfiguration configuration;

    /**
     * Creates a new connection factory.
     *
     * @param configuration the configuration to use connections
     * @throws NullPointerException if {@code configuration} is {@code null}
     */
    public PostgresqlConnectionFactory(PostgresqlConnectionConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
    }

    @Override
    public Publisher<? extends Connection> create() {
        Client client = new TcpClientClient(this.configuration.getHost(), this.configuration.getPort(), DefaultBackendMessageDecoder.INSTANCE);

        return StartupMessageFlow.exchange(this.configuration.getApplicationName(), client, this.configuration.getDatabase(), this.configuration.getPassword(), this.configuration.getUsername())
            .reduceWith(PostgresqlConnection::builder, (builder, message) -> {
                if (message instanceof ParameterStatus) {
                    ParameterStatus m = (ParameterStatus) message;
                    return builder.parameter(m.getName(), m.getValue());
                }

                if (message instanceof BackendKeyData) {
                    BackendKeyData m = (BackendKeyData) message;
                    return builder.processId(m.getProcessId()).secretKey(m.getSecretKey());
                }

                return builder;
            })
            .map(builder -> builder.client(client).build())
            .cache();
    }

}
