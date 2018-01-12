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
import com.nebhale.r2dbc.postgresql.client.WindowCollector;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.DataRow;
import com.nebhale.r2dbc.postgresql.message.backend.EmptyQueryResponse;
import com.nebhale.r2dbc.postgresql.message.frontend.Query;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class PostgresqlOperationsTest {

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlOperations(null))
            .withMessage("client must not be null");
    }

    @Test
    public void queryEmptyQueryResponse() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(EmptyQueryResponse.INSTANCE)
            .build();

        new PostgresqlOperations(client)
            .query("test-query")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void queryMultipleStatements() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(
                new DataRow(Collections.singletonList(Unpooled.buffer().writeInt(100))), new CommandComplete("test", null, null),
                new DataRow(Collections.singletonList(Unpooled.buffer().writeInt(200))), new CommandComplete("test", null, null))
            .build();
        WindowCollector<PostgresqlRow> windows = new WindowCollector<>();

        new PostgresqlOperations(client)
            .query("test-query")
            .as(StepVerifier::create)
            .recordWith(windows)
            .expectNextCount(2)
            .verifyComplete();

        windows.next()
            .as(StepVerifier::create)
            .expectNext(new PostgresqlRow(Collections.singletonList(Unpooled.buffer().writeInt(100))))
            .verifyComplete();

        windows.next()
            .as(StepVerifier::create)
            .expectNext(new PostgresqlRow(Collections.singletonList(Unpooled.buffer().writeInt(200))))
            .verifyComplete();
    }

    @Test
    public void queryNoQuery() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlOperations(TestClient.NO_OP).query(null))
            .withMessage("query must not be null");
    }

    @Test
    public void querySingleStatement() {
        Client client = TestClient.builder()
            .expectRequest(new Query("test-query")).thenRespond(
                new DataRow(Collections.singletonList(Unpooled.buffer().writeInt(100))), new CommandComplete("test", null, null))
            .build();
        WindowCollector<PostgresqlRow> windows = new WindowCollector<>();

        new PostgresqlOperations(client)
            .query("test-query")
            .as(StepVerifier::create)
            .recordWith(windows)
            .expectNextCount(1)
            .verifyComplete();

        windows.next()
            .as(StepVerifier::create)
            .expectNext(new PostgresqlRow(Collections.singletonList(Unpooled.buffer().writeInt(100))))
            .verifyComplete();
    }

}
