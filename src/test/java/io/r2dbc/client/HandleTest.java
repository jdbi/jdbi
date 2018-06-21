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

package io.r2dbc.client;

import io.r2dbc.spi.test.MockBatch;
import io.r2dbc.spi.test.MockConnection;
import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockStatement;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static io.r2dbc.spi.IsolationLevel.SERIALIZABLE;
import static io.r2dbc.spi.Mutability.READ_ONLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class HandleTest {

    @Test
    public void beginTransaction() {
        MockConnection connection = MockConnection.empty();

        Publisher<Void> publisher = new Handle(connection)
            .beginTransaction();

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.isBeginTransactionCalled()).isTrue();
    }

    @Test
    public void close() {
        MockConnection connection = MockConnection.empty();

        Publisher<Void> publisher = new Handle(connection)
            .close();

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    public void commitTransaction() {
        MockConnection connection = MockConnection.empty();

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
            .batch(MockBatch.empty())
            .build();

        Batch batch = new Handle(connection)
            .createBatch();

        assertThat(batch).isNotNull();
    }

    @Test
    public void createQuery() {
        MockConnection connection = MockConnection.builder()
            .statement(MockStatement.empty())
            .build();

        Query query = new Handle(connection)
            .createQuery("test-query");

        assertThat(query).isNotNull();
        assertThat(connection.getCreateStatementSql()).isEqualTo("test-query");
    }

    @Test
    public void createQueryNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).createQuery(null))
            .withMessage("sql must not be null");
    }

    @Test
    public void createSavepoint() {
        MockConnection connection = MockConnection.empty();

        Publisher<Void> publisher = new Handle(connection)
            .createSavepoint("test-savepoint");

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getCreateSavepointName()).isEqualTo("test-savepoint");
    }

    @Test
    public void createSavepointNoName() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).createSavepoint(null))
            .withMessage("name must not be null");
    }

    @Test
    public void createUpdate() {
        MockConnection connection = MockConnection.builder()
            .statement(MockStatement.empty())
            .build();

        Update update = new Handle(connection)
            .createUpdate("test-update");

        assertThat(update).isNotNull();
        assertThat(connection.getCreateStatementSql()).isEqualTo("test-update");
    }

    @Test
    public void createUpdateNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).createUpdate(null))
            .withMessage("sql must not be null");
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
    public void executeNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).execute(null, new Object()))
            .withMessage("sql must not be null");
    }

    @Test
    public void inTransaction() {
        MockConnection connection = MockConnection.empty();

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
        MockConnection connection = MockConnection.empty();
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
    public void inTransactionIsolationLevel() {
        MockConnection connection = MockConnection.empty();

        new Handle(connection)
            .inTransaction(SERIALIZABLE, handle ->
                Mono.just(100))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.getSetTransactionIsolationLevelIsolationLevel()).isEqualTo(SERIALIZABLE);
        assertThat(connection.isCommitTransactionCalled()).isTrue();
    }

    @Test
    public void inTransactionIsolationLevelNoF() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).inTransaction(SERIALIZABLE, null))
            .withMessage("f must not be null");
    }

    @Test
    public void inTransactionIsolationLevelNoIsolationLevel() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).inTransaction(null, handle -> Mono.empty()))
            .withMessage("isolationLevel must not be null");
    }

    @Test
    public void inTransactionNoF() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).inTransaction(null))
            .withMessage("f must not be null");
    }

    @Test
    public void releaseSavepoint() {
        MockConnection connection = MockConnection.empty();

        Publisher<Void> publisher = new Handle(connection)
            .releaseSavepoint("test-savepoint");

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getReleaseSavepointName()).isEqualTo("test-savepoint");
    }

    @Test
    public void releaseSavepointNoName() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).releaseSavepoint(null))
            .withMessage("name must not be null");
    }

    @Test
    public void rollbackTransaction() {
        MockConnection connection = MockConnection.empty();

        Publisher<Void> publisher = new Handle(connection)
            .rollbackTransaction();

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.isRollbackTransactionCalled()).isTrue();
    }

    @Test
    public void rollbackTransactionToSavepoint() {
        MockConnection connection = MockConnection.empty();

        Publisher<Void> publisher = new Handle(connection)
            .rollbackTransactionToSavepoint("test-savepoint");

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getRollbackTransactionToSavepointName()).isEqualTo("test-savepoint");
    }

    @Test
    public void rollbackTransactionToSavepointNoName() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).rollbackTransactionToSavepoint(null))
            .withMessage("name must not be null");
    }

    @Test
    public void select() {
        MockStatement statement = MockStatement.empty();

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
    public void selectNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).select(null, new Object()))
            .withMessage("sql must not be null");
    }

    @Test
    public void setTransactionIsolationLevel() {
        MockConnection connection = MockConnection.empty();

        Publisher<Void> publisher = new Handle(connection)
            .setTransactionIsolationLevel(SERIALIZABLE);

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getSetTransactionIsolationLevelIsolationLevel()).isEqualTo(SERIALIZABLE);
    }

    @Test
    public void setTransactionIsolationLevelNoIsolationLevel() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).setTransactionIsolationLevel(null))
            .withMessage("isolationLevel must not be null");
    }

    @Test
    public void setTransactionMutability() {
        MockConnection connection = MockConnection.empty();

        Publisher<Void> publisher = new Handle(connection)
            .setTransactionMutability(READ_ONLY);

        StepVerifier.create(publisher).verifyComplete();
        assertThat(connection.getSetTransactionMutabilityMutability()).isEqualTo(READ_ONLY);
    }

    @Test
    public void setTransactionMutabilityNoMutability() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).setTransactionMutability(null))
            .withMessage("mutability must not be null");
    }

    @Test
    public void useTransaction() {
        MockConnection connection = MockConnection.empty();

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
        MockConnection connection = MockConnection.empty();
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
    public void useTransactionIsolationLevel() {
        MockConnection connection = MockConnection.empty();

        new Handle(connection)
            .useTransaction(SERIALIZABLE, handle ->
                Mono.just(100))
            .as(StepVerifier::create)
            .verifyComplete();

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.getSetTransactionIsolationLevelIsolationLevel()).isEqualTo(SERIALIZABLE);
        assertThat(connection.isCommitTransactionCalled()).isTrue();
    }

    @Test
    public void useTransactionIsolationLevelNoF() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).useTransaction(SERIALIZABLE, null))
            .withMessage("f must not be null");
    }

    @Test
    public void useTransactionIsolationLevelNoIsolationLevel() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).useTransaction(null, handle -> Mono.empty()))
            .withMessage("isolationLevel must not be null");
    }

    @Test
    public void useTransactionNoF() {
        assertThatNullPointerException().isThrownBy(() -> new Handle(MockConnection.empty()).useTransaction(null))
            .withMessage("f must not be null");
    }

}
