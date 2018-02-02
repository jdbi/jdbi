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

import com.nebhale.r2dbc.postgresql.client.Binding;
import com.nebhale.r2dbc.postgresql.client.Client;
import com.nebhale.r2dbc.postgresql.client.Parameter;
import com.nebhale.r2dbc.postgresql.client.TestClient;
import com.nebhale.r2dbc.postgresql.message.backend.ErrorResponse;
import com.nebhale.r2dbc.postgresql.message.backend.ParseComplete;
import com.nebhale.r2dbc.postgresql.message.frontend.Describe;
import com.nebhale.r2dbc.postgresql.message.frontend.ExecutionType;
import com.nebhale.r2dbc.postgresql.message.frontend.Parse;
import com.nebhale.r2dbc.postgresql.message.frontend.Sync;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.util.Collections;

import static com.nebhale.r2dbc.postgresql.client.TestClient.NO_OP;
import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class IndefiniteStatementCacheTest {

    private static final Parameter DEFAULT_PARAMETER = new Parameter(TEXT, 400, null);

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> new IndefiniteStatementCache(null))
            .withMessage("client must not be null");
    }

    @Test
    public void getName() {
        // @formatter:off
        Client client = TestClient.builder()
            .expectRequest(new Parse("S_0", Collections.singletonList(100), "test-query"), new Describe("S_0", ExecutionType.STATEMENT), Sync.INSTANCE)
                .thenRespond(ParseComplete.INSTANCE)
            .expectRequest(new Parse("S_1", Collections.singletonList(200), "test-query"), new Describe("S_1", ExecutionType.STATEMENT), Sync.INSTANCE)
                .thenRespond(ParseComplete.INSTANCE)
            .expectRequest(new Parse("S_2", Collections.singletonList(200), "test-query-2"), new Describe("S_2", ExecutionType.STATEMENT), Sync.INSTANCE)
                .thenRespond(ParseComplete.INSTANCE)
            .build();
        // @formatter:on

        IndefiniteStatementCache statementCache = new IndefiniteStatementCache(client);

        statementCache.getName(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, 100, Unpooled.buffer().writeInt(100))), "test-query")
            .as(StepVerifier::create)
            .expectNext("S_0")
            .verifyComplete();

        statementCache.getName(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, 100, Unpooled.buffer().writeInt(200))), "test-query")
            .as(StepVerifier::create)
            .expectNext("S_0")
            .verifyComplete();

        statementCache.getName(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, 200, Unpooled.buffer().writeShort(300))), "test-query")
            .as(StepVerifier::create)
            .expectNext("S_1")
            .verifyComplete();

        statementCache.getName(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, 200, Unpooled.buffer().writeShort(300))), "test-query-2")
            .as(StepVerifier::create)
            .expectNext("S_2")
            .verifyComplete();
    }

    @Test
    public void getNameErrorResponse() {
        // @formatter:off
        Client client = TestClient.builder()
            .expectRequest(new Parse("S_0", Collections.singletonList(100), "test-query"), new Describe("S_0", ExecutionType.STATEMENT), Sync.INSTANCE)
                .thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();
        // @formatter:on

        IndefiniteStatementCache statementCache = new IndefiniteStatementCache(client);

        statementCache.getName(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, 100, Unpooled.buffer().writeInt(200))), "test-query")
            .as(StepVerifier::create)
            .verifyError(PostgresqlServerErrorException.class);
    }

    @Test
    public void getNameNoBinding() {
        assertThatNullPointerException().isThrownBy(() -> new IndefiniteStatementCache(NO_OP).getName(null, "test-query"))
            .withMessage("binding must not be null");
    }

    @Test
    public void getNameNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new IndefiniteStatementCache(NO_OP).getName(new Binding(DEFAULT_PARAMETER), null))
            .withMessage("sql must not be null");
    }

}
