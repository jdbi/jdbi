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

package com.nebhale.r2dbc.core;

import com.nebhale.r2dbc.spi.MockBatch;
import com.nebhale.r2dbc.spi.MockConnection;
import com.nebhale.r2dbc.spi.MockResult;
import com.nebhale.r2dbc.spi.MockStatement;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static com.nebhale.r2dbc.spi.IsolationLevel.SERIALIZABLE;
import static com.nebhale.r2dbc.spi.Mutability.READ_ONLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class HandleTest {

    @Test
    public void beginTransaction() {
        MockConnection connection = MockConnection.EMPTY;

        Publisher<Void> publisher = new Handle(connection)
            .beginTransaction();

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.isBeginTransactionCalled()).isTrue();
    }

    @Test
    public void close() {
        MockConnection connection = MockConnection.EMPTY;

        Publisher<Void> publisher = new Handle(connection)
            .close();

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    public void commitTransaction() {
        MockConnection connection = MockConnection.EMPTY;

        Publisher<Void> publisher = new Handle(connection)
            .commitTransaction();

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.isCommitTransactionCalled()).isTrue();
    }

    @Test
    public void constructorNoConnection() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(null))
            .withMessage("connection must not be null");
    }

    @Test
    public void createBatch() {
        MockConnection connection = MockConnection.builder()
            .batch(MockBatch.EMPTY)
            .build();

        Batch batch = new Handle(connection)
            .createBatch();

        assertThat(batch).isNotNull();
    }

    @Test
    public void createQuery() {
        MockConnection connection = MockConnection.builder()
            .statement(MockStatement.EMPTY)
            .build();

        Query query = new Handle(connection)
            .createQuery("test-query");

        assertThat(query).isNotNull();
        assertThat(connection.getCreateStatementSql()).isEqualTo("test-query");
    }

    @Test
    public void createSavepoint() {
        MockConnection connection = MockConnection.EMPTY;

        Publisher<Void> publisher = new Handle(connection)
            .createSavepoint("test-savepoint");

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getCreateSavepointName()).isEqualTo("test-savepoint");
    }

    @Test
    public void createUpdate() {
        MockConnection connection = MockConnection.builder()
            .statement(MockStatement.EMPTY)
            .build();

        Update update = new Handle(connection)
            .createUpdate("test-update");

        assertThat(update).isNotNull();
        assertThat(connection.getCreateStatementSql()).isEqualTo("test-update");
    }

    @Test
    public void execute() {
        MockStatement statement = MockStatement.builder()
            .result(MockResult.builder()
                .rowsUpdated(200)
                .build())
            .build();

        MockConnection connection = MockConnection.builder()
            .statement(statement)
            .build();

        new Handle(connection)
            .execute("test-update", 100)
            .as(StepVerifier::create)
            .expectNext(200)
            .verifyComplete();

        assertThat(connection.getCreateStatementSql()).isEqualTo("test-update");
        assertThat(statement.getBindings()).contains(Collections.singletonMap(0, 100));
    }

    @Test
    public void inTransaction() {
        MockConnection connection = MockConnection.EMPTY;

        new Handle(connection)
            .inTransaction(handle ->
                Mono.just(100))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.isCommitTransactionCalled()).isTrue();
    }

    @Test
    public void inTransactionError() {
        MockConnection connection = MockConnection.EMPTY;
        Exception exception = new Exception();

        new Handle(connection)
            .inTransaction(handle ->
                Mono.error(exception))
            .as(StepVerifier::create)
            .verifyErrorMatches(exception::equals);

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.isRollbackTransactionCalled()).isTrue();
    }

    @Test
    public void inTransactionNoF() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.EMPTY).inTransaction(null))
            .withMessage("f must not be null");
    }

    @Test
    public void releaseSavepoint() {
        MockConnection connection = MockConnection.EMPTY;

        Publisher<Void> publisher = new Handle(connection)
            .releaseSavepoint("test-savepoint");

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getReleaseSavepointName()).isEqualTo("test-savepoint");
    }

    @Test
    public void rollbackTransaction() {
        MockConnection connection = MockConnection.EMPTY;

        Publisher<Void> publisher = new Handle(connection)
            .rollbackTransaction();

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.isRollbackTransactionCalled()).isTrue();
    }

    @Test
    public void rollbackTransactionToSavepoint() {
        MockConnection connection = MockConnection.EMPTY;

        Publisher<Void> publisher = new Handle(connection)
            .rollbackTransactionToSavepoint("test-savepoint");

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getRollbackTransactionToSavepointName()).isEqualTo("test-savepoint");
    }

    @Test
    public void select() {
        MockStatement statement = MockStatement.EMPTY;

        MockConnection connection = MockConnection.builder()
            .statement(statement)
            .build();

        Query query = new Handle(connection)
            .select("test-query", 100);

        assertThat(query).isNotNull();
        assertThat(connection.getCreateStatementSql()).isEqualTo("test-query");
        assertThat(statement.getBindings()).contains(Collections.singletonMap(0, 100));
    }

    @Test
    public void setTransactionIsolationLevel() {
        MockConnection connection = MockConnection.EMPTY;

        Publisher<Void> publisher = new Handle(connection)
            .setTransactionIsolationLevel(SERIALIZABLE);

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getSetTransactionIsolationLevelIsolationLevel()).isEqualTo(SERIALIZABLE);
    }

    @Test
    public void setTransactionMutability() {
        MockConnection connection = MockConnection.EMPTY;

        Publisher<Void> publisher = new Handle(connection)
            .setTransactionMutability(READ_ONLY);

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getSetTransactionMutabilityMutability()).isEqualTo(READ_ONLY);
    }

    @Test
    public void useTransaction() {
        MockConnection connection = MockConnection.EMPTY;

        new Handle(connection)
            .useTransaction(handle ->
                Mono.just(100))
            .as(StepVerifier::create)
            .verifyComplete();

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.isCommitTransactionCalled()).isTrue();
    }

    @Test
    public void useTransactionError() {
        MockConnection connection = MockConnection.EMPTY;
        Exception exception = new Exception();

        new Handle(connection)
            .useTransaction(handle ->
                Mono.error(exception))
            .as(StepVerifier::create)
            .verifyErrorMatches(exception::equals);

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.isRollbackTransactionCalled()).isTrue();
    }

    @Test
    public void useTransactionNoF() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.EMPTY).useTransaction(null))
            .withMessage("f must not be null");
    }

}
