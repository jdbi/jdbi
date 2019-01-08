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

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.test.MockColumnMetadata;
import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockRow;
import io.r2dbc.spi.test.MockRowMetadata;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class ResultBearingTest {

    @Test
    void mapRowBiFunction() {
        MockRowMetadata rowMetadata = MockRowMetadata.builder()
            .columnMetadata(MockColumnMetadata.builder()
                .name("test-name")
                .type(100)
                .build())
            .build();

        MockRow row1 = MockRow.builder()
            .identified("test-identifier-1", Object.class, new Object())
            .build();

        MockRow row2 = MockRow.builder()
            .identified("test-identifier-2", Object.class, new Object())
            .build();

        MockResultBearing resultBearing = MockResultBearing.builder()
            .result(MockResult.builder()
                .rowMetadata(rowMetadata)
                .row(row1, row2)
                .build())
            .build();

        resultBearing
            .mapRow(Tuples::of)
            .as(StepVerifier::create)
            .expectNext(Tuples.of(row1, rowMetadata))
            .expectNext(Tuples.of(row2, rowMetadata))
            .verifyComplete();
    }

    @Test
    void mapRowBiFunctionNoF() {
        MockResultBearing resultBearing = MockResultBearing.builder()
            .result(MockResult.empty())
            .build();

        assertThatIllegalArgumentException().isThrownBy(() -> resultBearing.mapRow((BiFunction<Row, RowMetadata, ?>) null))
            .withMessage("f must not be null");
    }

    @Test
    void mapRowFunction() {
        MockRow row1 = MockRow.builder()
            .identified("test-identifier-1", Object.class, new Object())
            .build();

        MockRow row2 = MockRow.builder()
            .identified("test-identifier-2", Object.class, new Object())
            .build();

        MockResultBearing resultBearing = MockResultBearing.builder()
            .result(MockResult.builder()
                .rowMetadata(MockRowMetadata.empty())
                .row(row1, row2)
                .build())
            .build();

        resultBearing
            .mapRow(Function.identity())
            .as(StepVerifier::create)
            .expectNext(row1)
            .expectNext(row2)
            .verifyComplete();
    }

    @Test
    void mapRowFunctionNoF() {
        MockResultBearing resultBearing = MockResultBearing.builder()
            .result(MockResult.empty())
            .build();

        assertThatIllegalArgumentException().isThrownBy(() -> resultBearing.mapRow((Function<Row, ?>) null))
            .withMessage("f must not be null");
    }

}
