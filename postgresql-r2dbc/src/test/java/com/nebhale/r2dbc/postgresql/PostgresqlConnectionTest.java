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

import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.EmptyQueryResponse;
import com.nebhale.r2dbc.postgresql.message.frontend.Query;
import com.nebhale.r2dbc.postgresql.message.frontend.Terminate;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;

import static com.nebhale.r2dbc.IsolationLevel.READ_COMMITTED;
import static com.nebhale.r2dbc.Mutability.READ_ONLY;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class PostgresqlConnectionTest {

    @Test
    public void begin() {
        Client client = TestClient.builder()
            .expectRequest(new Query("BEGIN")).thenRespond(new CommandComplete("BEGIN", null, null))
            .build();

        PostgresqlConnection.builder().client(client).processId(100).secretKey(200).build()
            .begin()
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    public void builderNoClient() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder().client(null))
            .withMessage("client must not be null");
    }

    @Test
    public void builderParameterNoKey() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder().parameter(null, "test-value"))
            .withMessage("key must not be null");
    }

    @Test
    public void builderParameterNoValue() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder().parameter("test-key", null))
            .withMessage("value must not be null");
    }

    @Test
    public void close() {
        Client client = TestClient.builder()
            .expectRequest(Terminate.INSTANCE).thenRespond()
            .expectClose()
            .build();

        PostgresqlConnection.builder().client(client).processId(100).secretKey(200).build()
            .close()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder()
            .processId(100)
            .secretKey(200)
            .build())
            .withMessage("client must not be null");
    }

    @Test
    public void constructorProcessId() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder()
            .client(TestClient.NO_OP)
            .secretKey(200)
            .build())
            .withMessage("processId must not be null");
    }

    @Test
    public void constructorSecretKey() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder()
            .client(TestClient.NO_OP)
            .processId(100)
            .build())
            .withMessage("secretKey must not be null");
    }

    @Test
    public void query() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(new CommandComplete("test", null, null))
            .build();

        PostgresqlConnection.builder().client(client).processId(100).secretKey(200).build()
            .query("test-query")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void queryNoQuery() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder().client(TestClient.NO_OP).processId(100).secretKey(200).build().query(null))
            .withMessage("query must not be null");
    }

    @Test
    public void setIsolationLevel() {
        Client client = TestClient.builder()
            .expectRequest(new Query("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL READ COMMITTED")).thenRespond(new CommandComplete("SET", null, null))
            .build();

        PostgresqlConnection.builder().client(client).processId(100).secretKey(200).build()
            .setIsolationLevel(READ_COMMITTED)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void setIsolationLevelNoIsolationLevel() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder().client(TestClient.NO_OP).processId(100).secretKey(200).build().setIsolationLevel(null))
            .withMessage("isolationLevel must not be null");
    }

    @Test
    public void setMutability() {
        Client client = TestClient.builder()
            .expectRequest(new Query("SET SESSION CHARACTERISTICS AS TRANSACTION READ ONLY")).thenRespond(new CommandComplete("SET", null, null))
            .build();

        PostgresqlConnection.builder().client(client).processId(100).secretKey(200).build()
            .setMutability(READ_ONLY)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void setMutabilityNoMutability() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder().client(TestClient.NO_OP).processId(100).secretKey(200).build().setMutability(null))
            .withMessage("mutability must not be null");
    }

    @Test
    public void withTransaction() {
        Client client = TestClient.builder()
            .expectRequest(new Query("BEGIN")).thenRespond(new CommandComplete("BEGIN", null, null))
            .expectRequest(new Query("test-query")).thenRespond(EmptyQueryResponse.INSTANCE)
            .expectRequest(new Query("COMMIT")).thenRespond(new CommandComplete("COMMIT", null, null))
            .build();

        PostgresqlConnection.builder().client(client).processId(100).secretKey(200).build()
            .withTransaction(transaction ->
                transaction.query("test-query")
                    .then())
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void withTransactionNoTransaction() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnection.builder().client(TestClient.NO_OP).processId(100).secretKey(200).build().withTransaction(null))
            .withMessage("transaction must not be null");
    }

    @Test
    public void withTransactionRollback() {
        Client client = TestClient.builder()
            .expectRequest(new Query("BEGIN")).thenRespond(new CommandComplete("BEGIN", null, null))
            .expectRequest(new Query("test-query")).thenRespond(Flux.error(new ServerErrorException(Collections.emptyList())))
            .expectRequest(new Query("ROLLBACK")).thenRespond(new CommandComplete("ROLLBACK", null, null))
            .build();

        PostgresqlConnection.builder().client(client).processId(100).secretKey(200).build()
            .withTransaction(transaction ->
                transaction.query("test-query")
                    .then())
            .as(StepVerifier::create)
            .verifyError(ServerErrorException.class);
    }

}
