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

import io.r2dbc.spi.test.MockConnection;
import io.r2dbc.spi.test.MockConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

final class R2dbcTest {

    @Test
    void constructorNoConnectionFactory() {
        assertThatNullPointerException().isThrownBy(() -> new R2dbc(null))
            .withMessage("connectionFactory must not be null");
    }

    @Test
    void inTransaction() {
        MockConnection connection = MockConnection.empty();

        MockConnectionFactory connectionFactory = MockConnectionFactory.builder()
            .connection(connection)
            .build();

        new R2dbc(connectionFactory)
            .inTransaction(handle ->
                Mono.just(100))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.isCommitTransactionCalled()).isTrue();
        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    void inTransactionError() {
        MockConnection connection = MockConnection.empty();

        MockConnectionFactory connectionFactory = MockConnectionFactory.builder()
            .connection(connection)
            .build();

        Exception exception = new Exception();

        new R2dbc(connectionFactory)
            .inTransaction(handle ->
                Mono.error(exception))
            .as(StepVerifier::create)
            .verifyErrorMatches(exception::equals);

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.isRollbackTransactionCalled()).isTrue();
        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    void inTransactionNoF() {
        assertThatNullPointerException().isThrownBy(() -> new R2dbc(MockConnectionFactory.empty()).inTransaction(null))
            .withMessage("f must not be null");
    }

    @Test
    void open() {
        MockConnection connection = MockConnection.empty();

        MockConnectionFactory connectionFactory = MockConnectionFactory.builder()
            .connection(connection)
            .build();

        new R2dbc(connectionFactory)
            .open()
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void useHandle() {
        MockConnection connection = MockConnection.empty();

        MockConnectionFactory connectionFactory = MockConnectionFactory.builder()
            .connection(connection)
            .build();

        new R2dbc(connectionFactory)
            .useHandle(handle ->
                Mono.just(100))
            .as(StepVerifier::create)
            .verifyComplete();

        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    void useHandleError() {
        MockConnection connection = MockConnection.empty();

        MockConnectionFactory connectionFactory = MockConnectionFactory.builder()
            .connection(connection)
            .build();

        Exception exception = new Exception();

        new R2dbc(connectionFactory)
            .useHandle(handle ->
                Mono.error(exception))
            .as(StepVerifier::create)
            .verifyErrorMatches(exception::equals);

        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    void useHandleNoF() {
        assertThatNullPointerException().isThrownBy(() -> new R2dbc(MockConnectionFactory.empty()).useHandle(null))
            .withMessage("f must not be null");
    }

    @Test
    void useTransaction() {
        MockConnection connection = MockConnection.empty();

        MockConnectionFactory connectionFactory = MockConnectionFactory.builder()
            .connection(connection)
            .build();

        new R2dbc(connectionFactory)
            .useTransaction(handle ->
                Mono.just(100))
            .as(StepVerifier::create)
            .verifyComplete();

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.isCommitTransactionCalled()).isTrue();
        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    void useTransactionError() {
        MockConnection connection = MockConnection.empty();

        MockConnectionFactory connectionFactory = MockConnectionFactory.builder()
            .connection(connection)
            .build();

        Exception exception = new Exception();

        new R2dbc(connectionFactory)
            .useTransaction(handle ->
                Mono.error(exception))
            .as(StepVerifier::create)
            .verifyErrorMatches(exception::equals);

        assertThat(connection.isBeginTransactionCalled()).isTrue();
        assertThat(connection.isRollbackTransactionCalled()).isTrue();
        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    void useTransactionNoF() {
        assertThatNullPointerException().isThrownBy(() -> new R2dbc(MockConnectionFactory.empty()).useTransaction(null))
            .withMessage("f must not be null");
    }

    @Test
    void withHandle() {
        MockConnection connection = MockConnection.empty();

        MockConnectionFactory connectionFactory = MockConnectionFactory.builder()
            .connection(connection)
            .build();

        new R2dbc(connectionFactory)
            .withHandle(handle ->
                Mono.just(100))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    void withHandleError() {
        MockConnection connection = MockConnection.empty();

        MockConnectionFactory connectionFactory = MockConnectionFactory.builder()
            .connection(connection)
            .build();

        Exception exception = new Exception();

        new R2dbc(connectionFactory)
            .withHandle(handle ->
                Mono.error(exception))
            .as(StepVerifier::create)
            .verifyErrorMatches(exception::equals);

        assertThat(connection.isCloseCalled()).isTrue();
    }

    @Test
    void withHandleNoF() {
        assertThatNullPointerException().isThrownBy(() -> new R2dbc(MockConnectionFactory.empty()).withHandle(null))
            .withMessage("f must not be null");
    }

}
