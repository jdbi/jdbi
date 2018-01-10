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

import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationMD5Password;
import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationOk;
import com.nebhale.r2dbc.postgresql.message.backend.BackendKeyData;
import com.nebhale.r2dbc.postgresql.message.backend.ParameterStatus;
import com.nebhale.r2dbc.postgresql.message.frontend.PasswordMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.StartupMessage;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class PostgresqlConnectionFactoryTest {

    @Test
    public void constructorNoConfiguration() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlConnectionFactory(null))
            .withMessage("configuration must not be null");
    }

    @Test
    public void createAuthenticationMD5Password() {
        Client client = TestClient.builder()
            .expectRequest(new StartupMessage("test-application-name", "test-database", "test-username")).thenRespond(new AuthenticationMD5Password(Unpooled.buffer().writeInt(100)))
            .expectRequest(new PasswordMessage("md55e9836cdb369d50e3bc7d127e88b4804")).thenRespond(AuthenticationOk.INSTANCE, new ParameterStatus("test-key", "test-value"),
                new BackendKeyData(100, 200))
            .build();

        ClientFactory clientFactory = mock(ClientFactory.class);
        when(clientFactory.create()).thenReturn(client);

        PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
            .applicationName("test-application-name")
            .database("test-database")
            .host("test-host")
            .username("test-username")
            .password("test-password")
            .build();

        new PostgresqlConnectionFactory(configuration, clientFactory)
            .create()
            .as(StepVerifier::create)
            .assertNext(connection -> {
                assertThat(connection.getParameters()).containsEntry("test-key", "test-value");
                assertThat(connection.getProcessId()).isEqualTo(100);
                assertThat(connection.getSecretKey()).isEqualTo(200);
            })
            .verifyComplete();
    }

}
