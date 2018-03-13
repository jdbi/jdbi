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

import com.nebhale.r2dbc.spi.MockResult;
import com.nebhale.r2dbc.spi.MockStatement;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class QueryTest {

    @Test
    public void add() {
        MockStatement statement = MockStatement.empty();

        new Query(statement)
            .add();

        assertThat(statement.isAddCalled()).isTrue();
    }

    @Test
    public void bind() {
        MockStatement statement = MockStatement.empty();

        new Query(statement)
            .bind("test-identifier", "test-value");

        assertThat(statement.getBindings()).contains(Collections.singletonMap("test-identifier", "test-value"));
    }

    @Test
    public void bindIndex() {
        MockStatement statement = MockStatement.empty();

        new Query(statement)
            .bind(100, "test-value");

        assertThat(statement.getBindings()).contains(Collections.singletonMap(100, "test-value"));
    }

    @Test
    public void bindNull() {
        MockStatement statement = MockStatement.empty();

        new Query(statement)
            .bindNull("test-identifier", "test-type");

        assertThat(statement.getBindings()).contains(Collections.singletonMap("test-identifier", "test-type"));
    }

    @Test
    public void constructorNoStatement() {
        assertThatNullPointerException().isThrownBy(() -> new Query(null))
            .withMessage("statement must not be null");
    }

    @Test
    public void executeNoF() {
        assertThatNullPointerException().isThrownBy(() -> new Query(MockStatement.empty()).mapResult(null))
            .withMessage("f must not be null");
    }

    @Test
    public void mapResult() {
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

}
