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
import com.nebhale.r2dbc.postgresql.client.TestClient;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.frontend.Query;
import org.junit.Test;
import reactor.test.StepVerifier;

import static com.nebhale.r2dbc.IsolationLevel.READ_COMMITTED;
import static com.nebhale.r2dbc.Mutability.READ_ONLY;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class PostgresqlTransactionTest {

    @Test
    public void commit() {
        Client client = TestClient.builder()
            .expectRequest(new Query("COMMIT")).thenRespond(new CommandComplete("COMMIT", null, null))
            .build();

        new PostgresqlTransaction(client)
            .commit()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlTransaction(null))
            .withMessage("client must not be null");
    }

    @Test
    public void createSavepoint() {
        Client client = TestClient.builder()
            .expectRequest(new Query("SAVEPOINT test-name")).thenRespond(new CommandComplete("SAVEPOINT", null, null))
            .build();

        new PostgresqlTransaction(client)
            .createSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void createSavepointNoName() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlTransaction(TestClient.NO_OP).createSavepoint(null))
            .withMessage("name must not be null");
    }

    @Test
    public void query() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(new CommandComplete("test", null, null))
            .build();

        new PostgresqlTransaction(client)
            .query("test-query")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void queryNoQuery() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlTransaction(TestClient.NO_OP).query(null))
            .withMessage("query must not be null");
    }

    @Test
    public void releaseSavepoint() {
        Client client = TestClient.builder()
            .expectRequest(new Query("RELEASE SAVEPOINT test-name")).thenRespond(new CommandComplete("RELEASE", null, null))
            .build();

        new PostgresqlTransaction(client)
            .releaseSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void releaseSavepointNoName() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlTransaction(TestClient.NO_OP).releaseSavepoint(null))
            .withMessage("name must not be null");
    }

    @Test
    public void rollback() {
        Client client = TestClient.builder()
            .expectRequest(new Query("ROLLBACK")).thenRespond(new CommandComplete("ROLLBACK", null, null))
            .build();

        new PostgresqlTransaction(client)
            .rollback()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void rollbackToSavepoint() {
        Client client = TestClient.builder()
            .expectRequest(new Query("ROLLBACK TO SAVEPOINT test-name")).thenRespond(new CommandComplete("ROLLBACK", null, null))
            .build();

        new PostgresqlTransaction(client)
            .rollbackToSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void rollbackToSavepointNoName() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlTransaction(TestClient.NO_OP).rollbackToSavepoint(null))
            .withMessage("name must not be null");
    }

    @Test
    public void setIsolationLevel() {
        Client client = TestClient.builder()
            .expectRequest(new Query("SET TRANSACTION ISOLATION LEVEL READ COMMITTED")).thenRespond(new CommandComplete("SET", null, null))
            .build();

        new PostgresqlTransaction(client)
            .setIsolationLevel(READ_COMMITTED)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void setIsolationLevelNoIsolationLevel() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlTransaction(TestClient.NO_OP).setIsolationLevel(null))
            .withMessage("isolationLevel must not be null");
    }

    @Test
    public void setMutability() {
        Client client = TestClient.builder()
            .expectRequest(new Query("SET TRANSACTION READ ONLY")).thenRespond(new CommandComplete("SET", null, null))
            .build();

        new PostgresqlTransaction(client)
            .setMutability(READ_ONLY)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void setMutabilityNoMutability() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlTransaction(TestClient.NO_OP).setMutability(null))
            .withMessage("mutability must not be null");
    }

}
