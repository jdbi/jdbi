/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.client;

import io.r2dbc.spi.test.MockBatch;
import io.r2dbc.spi.test.MockResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class BatchTest {

    @Test
    void add() {
        MockBatch batch = MockBatch.empty();

        new Batch(batch)
            .add("test-query");

        assertThat(batch.getSqls()).contains("test-query");
    }

    @Test
    void addNoSql() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Batch(MockBatch.empty()).add(null))
            .withMessage("sql must not be null");
    }

    @Test
    void constructorNoBatch() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Batch(null))
            .withMessage("batch must not be null");
    }

    @Test
    void mapResult() {
        MockResult result = MockResult.empty();

        MockBatch batch = MockBatch.builder()
            .result(result)
            .build();

        new Batch(batch)
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
        assertThatIllegalArgumentException().isThrownBy(() -> new Batch(MockBatch.empty()).mapResult(null))
            .withMessage("mappingFunction must not be null");
    }

}
