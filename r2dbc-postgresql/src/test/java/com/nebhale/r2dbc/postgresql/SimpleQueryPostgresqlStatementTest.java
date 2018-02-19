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
import com.nebhale.r2dbc.postgresql.codec.MockCodecs;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.DataRow;
import com.nebhale.r2dbc.postgresql.message.backend.EmptyQueryResponse;
import com.nebhale.r2dbc.postgresql.message.backend.ErrorResponse;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription;
import com.nebhale.r2dbc.postgresql.message.frontend.Query;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.util.Collections;

import static com.nebhale.r2dbc.postgresql.client.TestClient.NO_OP;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class SimpleQueryPostgresqlStatementTest {

    @Test
    public void bind() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> new SimpleQueryPostgresqlStatement(NO_OP, MockCodecs.empty(), "test-query").bind(null, null))
            .withMessage("Binding parameters is not supported for the statement 'test-query'");
    }

    @Test
    public void bindNull() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> new SimpleQueryPostgresqlStatement(NO_OP, MockCodecs.empty(), "test-query").bindNull(null, null))
            .withMessage("Binding parameters is not supported for the statement 'test-query'");
    }

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> new SimpleQueryPostgresqlStatement(null, MockCodecs.empty(), "test-query"))
            .withMessage("client must not be null");
    }

    @Test
    public void constructorNoCodecs() {
        assertThatNullPointerException().isThrownBy(() -> new SimpleQueryPostgresqlStatement(NO_OP, null, "test-query"))
            .withMessage("codecs must not be null");
    }

    @Test
    public void constructorNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new SimpleQueryPostgresqlStatement(NO_OP, MockCodecs.empty(), null))
            .withMessage("sql must not be null");
    }

    @Test
    public void executeCommandCompleteMap() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(new CommandComplete("test", null, 1))
            .build();

        new SimpleQueryPostgresqlStatement(client, MockCodecs.empty(), "test-query")
            .execute()
            .flatMap(result -> result.map((row, rowMetadata) -> row))
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void executeCommandCompleteRowsUpdated() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(new CommandComplete("test", null, 1))
            .build();

        new SimpleQueryPostgresqlStatement(client, MockCodecs.empty(), "test-query")
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    public void executeEmptyQueryResponseRows() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(EmptyQueryResponse.INSTANCE)
            .build();

        new SimpleQueryPostgresqlStatement(client, MockCodecs.empty(), "test-query")
            .execute()
            .flatMap(result -> result.map((row, rowMetadata) -> row))
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void executeEmptyQueryResponseRowsUpdated() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(EmptyQueryResponse.INSTANCE)
            .build();

        new SimpleQueryPostgresqlStatement(client, MockCodecs.empty(), "test-query")
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void executeErrorResponseRows() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new SimpleQueryPostgresqlStatement(client, MockCodecs.empty(), "test-query")
            .execute()
            .flatMap(result -> result.map((row, rowMetadata) -> row))
            .as(StepVerifier::create)
            .verifyError(PostgresqlServerErrorException.class);
    }

    @Test
    public void executeErrorResponseRowsUpdated() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        new SimpleQueryPostgresqlStatement(client, MockCodecs.empty(), "test-query")
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .as(StepVerifier::create)
            .verifyError(PostgresqlServerErrorException.class);
    }

    @Test
    public void executeRowDescriptionRows() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query"))
            .thenRespond(
                new RowDescription(Collections.singletonList(new RowDescription.Field((short) 100, 200, 300, (short) 400, TEXT, "test-name", 500))),
                new DataRow(Collections.singletonList(TEST.buffer(4).writeInt(100))),
                new CommandComplete("test", null, null))
            .build();

        new SimpleQueryPostgresqlStatement(client, MockCodecs.empty(), "test-query")
            .execute()
            .flatMap(result -> result.map((row, rowMetadata) -> row))
            .as(StepVerifier::create)
            .expectNext(new PostgresqlRow(MockCodecs.empty(), Collections.singletonList(new PostgresqlRow.Column(TEST.buffer(4).writeInt(100), 200, TEXT, "test-name"))))
            .verifyComplete();
    }

    @Test
    public void executeRowDescriptionRowsUpdated() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query"))
            .thenRespond(
                new RowDescription(Collections.singletonList(new RowDescription.Field((short) 100, 200, 300, (short) 400, TEXT, "test-name", 500))),
                new DataRow(Collections.singletonList(TEST.buffer(4).writeInt(100))),
                new CommandComplete("test", null, null))
            .build();

        new SimpleQueryPostgresqlStatement(client, MockCodecs.empty(), "test-query")
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void supportsNoSql() {
        assertThatNullPointerException().isThrownBy(() -> SimpleQueryPostgresqlStatement.supports(null))
            .withMessage("sql must not be null");
    }

    @Test
    public void supportsParameterSymbol() {
        assertThat(SimpleQueryPostgresqlStatement.supports("test-query-$1")).isFalse();
    }

    @Test
    public void supportsQueryEmpty() {
        assertThat(SimpleQueryPostgresqlStatement.supports(" ")).isTrue();
    }

    @Test
    public void supportsSemicolon() {
        assertThat(SimpleQueryPostgresqlStatement.supports("test-query-1 ; test-query-2")).isTrue();
    }

    @Test
    public void supportsSimple() {
        assertThat(SimpleQueryPostgresqlStatement.supports("test-query")).isTrue();
    }

}
