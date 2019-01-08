/*
 * Copyright 2017-2019 the original author or authors.
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
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class UpdateTest {

    @Test
    void add() {
        MockStatement statement = MockStatement.empty();

        new Update(statement)
            .add();

        assertThat(statement.isAddCalled()).isTrue();
    }

    @Test
    void bind() {
        MockStatement statement = MockStatement.empty();

        new Update(statement)
            .bind("test-identifier", "test-value");

        assertThat(statement.getBindings()).contains(Collections.singletonMap("test-identifier", "test-value"));
    }

    @Test
    void bindIndex() {
        MockStatement statement = MockStatement.empty();

        new Update(statement)
            .bind(100, "test-value");

        assertThat(statement.getBindings()).contains(Collections.singletonMap(100, "test-value"));
    }

    @Test
    void bindIndexNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Update(MockStatement.empty()).bind(100, null))
            .withMessage("value must not be null");
    }

    @Test
    void bindNoIdentifier() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Update(MockStatement.empty()).bind(null, new Object()))
            .withMessage("identifier must not be null");
    }

    @Test
    void bindNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Update(MockStatement.empty()).bind("test-identifier", null))
            .withMessage("value must not be null");
    }

    @Test
    void bindNull() {
        MockStatement statement = MockStatement.empty();

        new Update(statement)
            .bindNull("test-identifier", Integer.class);

        assertThat(statement.getBindings()).contains(Collections.singletonMap("test-identifier", Integer.class));
    }

    @Test
    void bindNullNoIdentifier() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Update(MockStatement.empty()).bindNull(null, Object.class))
            .withMessage("identifier must not be null");
    }

    @Test
    void bindNullNoType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Update(MockStatement.empty()).bindNull("test-identifier", null))
            .withMessage("type must not be null");
    }

    @Test
    void constructorNoStatement() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Update(null))
            .withMessage("statement must not be null");
    }

    @Test
    void execute() {
        MockResult result = MockResult.builder()
            .rowsUpdated(100)
            .build();

        MockStatement statement = MockStatement.builder()
            .result(result)
            .build();

        new Update(statement)
            .execute()
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();
    }

}
