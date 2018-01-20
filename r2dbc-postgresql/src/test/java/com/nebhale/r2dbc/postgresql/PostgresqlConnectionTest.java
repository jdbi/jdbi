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
import com.nebhale.r2dbc.postgresql.message.backend.ErrorResponse;
import com.nebhale.r2dbc.postgresql.message.frontend.Query;
import com.nebhale.r2dbc.postgresql.message.frontend.Terminate;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.util.Collections;

import static com.nebhale.r2dbc.IsolationLevel.READ_COMMITTED;
import static com.nebhale.r2dbc.Mutability.READ_ONLY;
import static com.nebhale.r2dbc.postgresql.client.TestClient.NO_OP;
import static com.nebhale.r2dbc.postgresql.client.TransactionStatus.IDLE;
import static com.nebhale.r2dbc.postgresql.client.TransactionStatus.OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;

public final class PostgresqlConnectionTest {

    private final StatementCache statementCache = mock(StatementCache.class, RETURNS_SMART_NULLS);

    @Test
    public void beginTransaction() {
        Client client = TestClient.builder()
            .expectRequest(new Query("BEGIN")).thenRespond(new CommandComplete("BEGIN", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .beginTransaction()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void beginTransactionErrorResponse() {
        Client client = TestClient.builder()
            .expectRequest(new Query("BEGIN")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .beginTransaction()
            .as(StepVerifier::create)
            .verifyErrorMatches(PostgresqlServerErrorException.class::isInstance);
    }

    @Test
    public void beginTransactionNonIdle() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .beginTransaction()
            .as(StepVerifier::create)
            .verifyErrorMatches(IllegalStateException.class::isInstance);
    }

    @Test
    public void close() {
        Client client = TestClient.builder()
            .expectRequest(Terminate.INSTANCE).thenRespond()
            .expectClose()
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .close()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void commitTransaction() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("COMMIT")).thenRespond(new CommandComplete("COMMIT", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .commitTransaction()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void commitTransactionErrorResponse() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("COMMIT")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .commitTransaction()
            .as(StepVerifier::create)
            .verifyErrorMatches(PostgresqlServerErrorException.class::isInstance);
    }

    @Test
    public void commitTransactionNonOpen() {
        Client client = TestClient.builder()
            .transactionStatus(IDLE)
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .commitTransaction()
            .as(StepVerifier::create)
            .verifyErrorMatches(IllegalStateException.class::isInstance);
    }

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlConnection(null, () -> "", this.statementCache))
            .withMessage("client must not be null");
    }

    @Test
    public void constructorNoPortalNameSupplier() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlConnection(NO_OP, null, this.statementCache))
            .withMessage("portalNameSupplier must not be null");
    }

    @Test
    public void constructorNoStatementCache() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlConnection(NO_OP, () -> "", null))
            .withMessage("statementCache must not be null");
    }

    @Test
    public void createBatch() {
        assertThat(new PostgresqlConnection(NO_OP, () -> "", this.statementCache).createBatch()).isInstanceOf(PostgresqlBatch.class);
    }

    @Test
    public void createSavePointNonOpen() {
        Client client = TestClient.builder()
            .transactionStatus(IDLE)
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .createSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyErrorMatches(IllegalStateException.class::isInstance);
    }

    @Test
    public void createSavepoint() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("SAVEPOINT test-name")).thenRespond(new CommandComplete("SAVEPOINT", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .createSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void createSavepointErrorResponse() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("SAVEPOINT test-name")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .createSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyErrorMatches(PostgresqlServerErrorException.class::isInstance);
    }

    @Test
    public void createSavepointNoName() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlConnection(NO_OP, () -> "", this.statementCache).createSavepoint(null))
            .withMessage("name must not be null");
    }

    @Test
    public void createStatementExtended() {
        assertThat(new PostgresqlConnection(NO_OP, () -> "", this.statementCache).createStatement("test-query-?")).isInstanceOf(ExtendedQueryPostgresqlStatement.class);
    }

    @Test
    public void createStatementIllegal() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PostgresqlConnection(NO_OP, () -> "", this.statementCache).createStatement("test-query-?-1 ; test-query-?-2"))
            .withMessage("Statement 'test-query-?-1 ; test-query-?-2' cannot be created. This is often due to the presence of both multiple statements and parameters at the same time.");
    }

    @Test
    public void createStatementSimple() {
        assertThat(new PostgresqlConnection(NO_OP, () -> "", this.statementCache).createStatement("test-query-1; test-query-2")).isInstanceOf(SimpleQueryPostgresqlStatement.class);
    }

    @Test
    public void getParameterStatus() {
        Client client = TestClient.builder()
            .parameterStatus("test-key", "test-value")
            .build();

        assertThat(new PostgresqlConnection(client, () -> "", this.statementCache).getParameterStatus()).containsEntry("test-key", "test-value");
    }

    @Test
    public void releaseSavepoint() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("RELEASE SAVEPOINT test-name")).thenRespond(new CommandComplete("RELEASE", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .releaseSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void releaseSavepointErrorResponse() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("RELEASE SAVEPOINT test-name")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .releaseSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyErrorMatches(PostgresqlServerErrorException.class::isInstance);
    }

    @Test
    public void releaseSavepointNoName() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlConnection(NO_OP, () -> "", this.statementCache).releaseSavepoint(null))
            .withMessage("name must not be null");
    }

    @Test
    public void releaseSavepointNonOpen() {
        Client client = TestClient.builder()
            .transactionStatus(IDLE)
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .releaseSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyErrorMatches(IllegalStateException.class::isInstance);
    }

    @Test
    public void rollbackTransaction() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("ROLLBACK")).thenRespond(new CommandComplete("ROLLBACK", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .rollbackTransaction()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void rollbackTransactionErrorResponse() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("ROLLBACK")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .rollbackTransaction()
            .as(StepVerifier::create)
            .verifyErrorMatches(PostgresqlServerErrorException.class::isInstance);
    }

    @Test
    public void rollbackTransactionNonOpen() {
        Client client = TestClient.builder()
            .transactionStatus(IDLE)
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .rollbackTransaction()
            .as(StepVerifier::create)
            .verifyErrorMatches(IllegalStateException.class::isInstance);
    }

    @Test
    public void rollbackTransactionToSavepoint() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("ROLLBACK TO SAVEPOINT test-name")).thenRespond(new CommandComplete("ROLLBACK", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .rollbackTransactionToSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void rollbackTransactionToSavepointErrorResponse() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("ROLLBACK TO SAVEPOINT test-name")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .rollbackTransactionToSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyErrorMatches(PostgresqlServerErrorException.class::isInstance);
    }

    @Test
    public void rollbackTransactionToSavepointNoName() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlConnection(NO_OP, () -> "", this.statementCache).rollbackTransactionToSavepoint(null))
            .withMessage("name must not be null");
    }

    @Test
    public void rollbackTransactionToSavepointNonOpen() {
        Client client = TestClient.builder()
            .transactionStatus(IDLE)
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .rollbackTransactionToSavepoint("test-name")
            .as(StepVerifier::create)
            .verifyErrorMatches(IllegalStateException.class::isInstance);
    }

    @Test
    public void setTransactionIsolationLevelErrorResponse() {
        Client client = TestClient.builder()
            .transactionStatus(IDLE)
            .expectRequest(new Query("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL READ COMMITTED")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .setTransactionIsolationLevel(READ_COMMITTED)
            .as(StepVerifier::create)
            .verifyErrorMatches(PostgresqlServerErrorException.class::isInstance);
    }

    @Test
    public void setTransactionIsolationLevelIdle() {
        Client client = TestClient.builder()
            .transactionStatus(IDLE)
            .expectRequest(new Query("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL READ COMMITTED")).thenRespond(new CommandComplete("SET", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .setTransactionIsolationLevel(READ_COMMITTED)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void setTransactionIsolationLevelNoIsolationLevel() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlConnection(NO_OP, () -> "", this.statementCache).setTransactionIsolationLevel(null))
            .withMessage("isolationLevel must not be null");
    }

    @Test
    public void setTransactionIsolationLevelOpen() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("SET TRANSACTION ISOLATION LEVEL READ COMMITTED")).thenRespond(new CommandComplete("SET", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .setTransactionIsolationLevel(READ_COMMITTED)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void setTransactionMutabilityErrorResponse() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("SET TRANSACTION ISOLATION LEVEL READ COMMITTED")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .setTransactionIsolationLevel(READ_COMMITTED)
            .as(StepVerifier::create)
            .verifyErrorMatches(PostgresqlServerErrorException.class::isInstance);
    }

    @Test
    public void setTransactionMutabilityIdle() {
        Client client = TestClient.builder()
            .transactionStatus(IDLE)
            .expectRequest(new Query("SET SESSION CHARACTERISTICS AS TRANSACTION READ ONLY")).thenRespond(new CommandComplete("SET", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .setTransactionMutability(READ_ONLY)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void setTransactionMutabilityNoMutability() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlConnection(NO_OP, () -> "", this.statementCache).setTransactionMutability(null))
            .withMessage("mutability must not be null");
    }

    @Test
    public void setTransactionMutabilityOpen() {
        Client client = TestClient.builder()
            .transactionStatus(OPEN)
            .expectRequest(new Query("SET TRANSACTION READ ONLY")).thenRespond(new CommandComplete("SET", null, null))
            .build();

        new PostgresqlConnection(client, () -> "", this.statementCache)
            .setTransactionMutability(READ_ONLY)
            .as(StepVerifier::create)
            .verifyComplete();
    }

}
