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

import com.nebhale.r2dbc.postgresql.authentication.PasswordAuthenticationHandler;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.DataRow;
import com.nebhale.r2dbc.postgresql.message.backend.DefaultBackendMessageDecoder;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription;
import com.nebhale.r2dbc.postgresql.message.frontend.Query;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class TcpClientClientTest {

    @ClassRule
    public static final PostgresqlServerResource SERVER = new PostgresqlServerResource();

    private final TcpClientClient client = new TcpClientClient(SERVER.getHost(), SERVER.getPort(), DefaultBackendMessageDecoder.INSTANCE);

    @BeforeClass
    public static void createSchema() {
        SERVER.getJdbcOperations().execute("CREATE TABLE test ( value INTEGER )");
    }

    @Before
    public void cleanTable() {
        SERVER.getJdbcOperations().execute("DELETE FROM test");
    }

    @Test
    public void close() {
        this.client.close();
    }

    @Test
    public void constructorNoDecoder() {
        assertThatNullPointerException().isThrownBy(() -> new TcpClientClient(SERVER.getHost(), SERVER.getPort(), null))
            .withMessage("decoder must not be null");
    }

    @Test
    public void constructorNoHost() {
        assertThatNullPointerException().isThrownBy(() -> new TcpClientClient(null, SERVER.getPort(), DefaultBackendMessageDecoder.INSTANCE))
            .withMessage("host must not be null");
    }

    @Before
    public void createClient() {
        PasswordAuthenticationHandler authenticationHandler = new PasswordAuthenticationHandler(SERVER.getPassword(), SERVER.getUsername());

        StartupMessageFlow
            .exchange("test-application-name", authenticationHandler, client, SERVER.getDatabase(), SERVER.getUsername())
            .blockLast();
    }

    @Test
    public void exchange() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        this.client.exchange(Mono.just(new Query("SELECT value FROM test")))
            .as(StepVerifier::create)
            .assertNext(message -> assertThat(message).isInstanceOf(RowDescription.class))
            .assertNext(message -> assertThat(message).isInstanceOf(DataRow.class))
            .expectNext(new CommandComplete("SELECT", null, 1))
            .verifyComplete();
    }

    @Test
    public void exchangeError() {
        this.client.exchange(Mono.just(new Query("SELECT value FROM alternate-test")))
            .as(StepVerifier::create)
            .verifyError(ServerErrorException.class);
    }

    @Test
    public void exchangeNoPublisher() {
        assertThatNullPointerException().isThrownBy(() -> this.client.exchange(null))
            .withMessage("requests must not be null");
    }

}