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

import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationCleartextPassword;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.DataRow;
import com.nebhale.r2dbc.postgresql.message.backend.ReadyForQuery;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.PasswordMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.Query;
import com.nebhale.r2dbc.postgresql.message.frontend.StartupMessage;
import com.nebhale.r2dbc.postgresql.util.PostgresqlServerResource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class ReactorNettyClientTest {

    @ClassRule
    public static final PostgresqlServerResource SERVER = new PostgresqlServerResource();

    private final ReactorNettyClient client = new ReactorNettyClient(SERVER.getHost(), SERVER.getPort());

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
        this.client.close()
            .thenMany(this.client.exchange(Mono.empty()))
            .as(StepVerifier::create)
            .verifyErrorSatisfies(t -> assertThat(t).isInstanceOf(IllegalStateException.class).hasMessage("Cannot exchange messages because the connection is closed"));
    }

    @After
    public void closeClient() {
        this.client.close()
            .block();
    }

    @Test
    public void constructorNoHost() {
        assertThatNullPointerException().isThrownBy(() -> new ReactorNettyClient(null, SERVER.getPort()))
            .withMessage("host must not be null");
    }

    @Before
    public void createClient() {
        EmitterProcessor<FrontendMessage> requestProcessor = EmitterProcessor.create(false);
        FluxSink<FrontendMessage> requests = requestProcessor.sink();

        this.client
            .exchange(requestProcessor)
            .doOnSubscribe(s -> requests.next(new StartupMessage("test-application-name", SERVER.getDatabase(), SERVER.getUsername())))
            .handle((message, sink) -> {
                if (message instanceof AuthenticationCleartextPassword) {
                    requests.next(new PasswordMessage(SERVER.getPassword()));
                } else {
                    sink.next(message);
                }
            })
            .takeUntil(ReadyForQuery.class::isInstance)
            .blockLast();
    }

    @Test
    public void exchange() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        this.client
            .exchange(Mono.just(new Query("SELECT value FROM test")))
            .as(StepVerifier::create)
            .assertNext(message -> assertThat(message).isInstanceOf(RowDescription.class))
            .assertNext(message -> assertThat(message).isInstanceOf(DataRow.class))
            .expectNext(new CommandComplete("SELECT", null, 1))
            .verifyComplete();
    }

    @Test
    public void exchangeNoPublisher() {
        assertThatNullPointerException().isThrownBy(() -> this.client.exchange(null))
            .withMessage("requests must not be null");
    }

    @Test
    public void handleBackendData() {
        assertThat(this.client.getProcessId()).isNotEmpty();
        assertThat(this.client.getSecretKey()).isNotEmpty();
    }

    @Test
    public void handleParameterStatus() {
        assertThat(this.client.getParameterStatus()).containsEntry("application_name", "test-application-name");
    }

    @Test
    public void handleTransactionStatus() {
        assertThat(this.client.getTransactionStatus()).isEqualTo(TransactionStatus.IDLE);

        this.client
            .exchange(Mono.just(new Query("BEGIN")))
            .blockLast();

        assertThat(this.client.getTransactionStatus()).isEqualTo(TransactionStatus.OPEN);
    }

    @Test
    public void largePayload() {
        IntStream.range(0, 1_000)
            .forEach(i -> SERVER.getJdbcOperations().update("INSERT INTO test VALUES(?)", i));

        this.client
            .exchange(Mono.just(new Query("SELECT value FROM test")))
            .as(StepVerifier::create)
            .expectNextCount(1 + 1_000 + 1)
            .verifyComplete();
    }

}
