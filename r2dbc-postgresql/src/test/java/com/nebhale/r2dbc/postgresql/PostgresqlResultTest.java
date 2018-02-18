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

import com.nebhale.r2dbc.postgresql.codec.MockCodecs;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.DataRow;
import com.nebhale.r2dbc.postgresql.message.backend.EmptyQueryResponse;
import com.nebhale.r2dbc.postgresql.message.backend.ErrorResponse;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class PostgresqlResultTest {

    @Test
    public void constructorNoCodec() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlResult(null, Mono.empty(), Flux.empty(), Mono.empty()))
            .withMessage("codecs must not be null");
    }

    @Test
    public void constructorNoRowMetadata() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlResult(MockCodecs.EMPTY, null, Flux.empty(), Mono.empty()))
            .withMessage("rowMetadata must not be null");
    }

    @Test
    public void constructorNoRows() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlResult(MockCodecs.EMPTY, Mono.empty(), null, Mono.empty()))
            .withMessage("rows must not be null");
    }

    @Test
    public void constructorNoRowsUpdated() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlResult(MockCodecs.EMPTY, Mono.empty(), Flux.empty(), null))
            .withMessage("rowsUpdated must not be null");
    }

    @Test
    public void toResultCommandComplete() {
        PostgresqlResult result = PostgresqlResult.toResult(MockCodecs.EMPTY, Flux.just(new CommandComplete("test", null, 1)));

        result.getRowMetadata()
            .as(StepVerifier::create)
            .verifyComplete();

        result.getRows()
            .as(StepVerifier::create)
            .verifyComplete();

        result.getRowsUpdated()
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    public void toResultEmptyQueryResponse() {
        PostgresqlResult result = PostgresqlResult.toResult(MockCodecs.EMPTY, Flux.just(EmptyQueryResponse.INSTANCE));

        result.getRowMetadata()
            .as(StepVerifier::create)
            .verifyComplete();

        result.getRows()
            .as(StepVerifier::create)
            .verifyComplete();

        result.getRowsUpdated()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void toResultErrorResponse() {
        PostgresqlResult result = PostgresqlResult.toResult(MockCodecs.EMPTY, Flux.just(new ErrorResponse(Collections.emptyList())));

        result.getRowMetadata()
            .as(StepVerifier::create)
            .verifyError(PostgresqlServerErrorException.class);

        result.getRows()
            .as(StepVerifier::create)
            .verifyError(PostgresqlServerErrorException.class);

        result.getRowsUpdated()
            .as(StepVerifier::create)
            .verifyError(PostgresqlServerErrorException.class);
    }


    @Test
    public void toResultNoCodecs() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlResult.toResult(null, Flux.empty()))
            .withMessage("codecs must not be null");
    }

    @Test
    public void toResultNoMessages() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlResult.toResult(MockCodecs.EMPTY, null))
            .withMessage("messages must not be null");
    }

    @Test
    public void toResultRowDescription() {
        PostgresqlResult result = PostgresqlResult.toResult(MockCodecs.EMPTY, Flux.just(new RowDescription(Collections.emptyList()), new DataRow(Collections.emptyList()), new CommandComplete
            ("test", null, null)));

        result.getRowMetadata()
            .as(StepVerifier::create)
            .expectNext(new PostgresqlRowMetadata(Collections.emptyList()))
            .verifyComplete();

        result.getRows()
            .as(StepVerifier::create)
            .expectNext(new PostgresqlRow(MockCodecs.EMPTY, Collections.emptyList()))
            .verifyComplete();

        result.getRowsUpdated()
            .as(StepVerifier::create)
            .verifyComplete();
    }

}
