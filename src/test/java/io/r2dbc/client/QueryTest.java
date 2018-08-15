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

import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockStatement;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

final class QueryTest {

    @Test
    void add() {
        MockStatement statement = MockStatement.empty();

        new Query(statement)
            .add();

        assertThat(statement.isAddCalled()).isTrue();
    }

    @Test
    void bind() {
        MockStatement statement = MockStatement.empty();

        new Query(statement)
            .bind("test-identifier", "test-value");

        assertThat(statement.getBindings()).contains(Collections.singletonMap("test-identifier", "test-value"));
    }

    @Test
    void bindIndex() {
        MockStatement statement = MockStatement.empty();

        new Query(statement)
            .bind(100, "test-value");

        assertThat(statement.getBindings()).contains(Collections.singletonMap(100, "test-value"));
    }

    @Test
    void bindIndexNoValue() {
        assertThatNullPointerException().isThrownBy(() -> new Query(MockStatement.empty()).bind(100, null))
            .withMessage("value must not be null");
    }

    @Test
    void bindNoIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> new Query(MockStatement.empty()).bind(null, new Object()))
            .withMessage("identifier must not be null");
    }

    @Test
    void bindNoValue() {
        assertThatNullPointerException().isThrownBy(() -> new Query(MockStatement.empty()).bind("test-identifier", null))
            .withMessage("value must not be null");
    }

    @Test
    void bindNull() {
        MockStatement statement = MockStatement.empty();

        new Query(statement)
            .bindNull("test-identifier", Integer.class);

        assertThat(statement.getBindings()).contains(Collections.singletonMap("test-identifier", Integer.class));
    }

    @Test
    void bindNullNoIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> new Query(MockStatement.empty()).bindNull(null, Object.class))
            .withMessage("identifier must not be null");
    }

    @Test
    void bindNullNoType() {
        assertThatNullPointerException().isThrownBy(() -> new Query(MockStatement.empty()).bindNull("test-identifier", null))
            .withMessage("type must not be null");
    }

    @Test
    void constructorNoStatement() {
        assertThatNullPointerException().isThrownBy(() -> new Query(null))
            .withMessage("statement must not be null");
    }

    @Test
    void mapResult() {
        MockResult result = MockResult.empty();

        MockStatement statement = MockStatement.builder()
            .result(result)
            .build();

        new Query(statement)
            .mapResult(actual -> {
                assertThat(actual).isSameAs(result);
                return Mono.just(1);
            })
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    void mapResultNoF() {
        assertThatNullPointerException().isThrownBy(() -> new Query(MockStatement.empty()).mapResult(null))
            .withMessage("f must not be null");
    }

}
