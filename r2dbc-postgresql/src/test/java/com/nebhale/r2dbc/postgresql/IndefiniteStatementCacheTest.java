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
import com.nebhale.r2dbc.postgresql.message.backend.ErrorResponse;
import com.nebhale.r2dbc.postgresql.message.backend.ParseComplete;
import com.nebhale.r2dbc.postgresql.message.frontend.Describe;
import com.nebhale.r2dbc.postgresql.message.frontend.ExecutionType;
import com.nebhale.r2dbc.postgresql.message.frontend.Parse;
import com.nebhale.r2dbc.postgresql.message.frontend.Sync;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.util.Collections;

import static com.nebhale.r2dbc.postgresql.client.TestClient.NO_OP;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class IndefiniteStatementCacheTest {

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> new IndefiniteStatementCache(null))
            .withMessage("client must not be null");
    }

    @Test
    public void getName() {
        Client client = TestClient.builder()
            .expectRequest(new Parse("S_0", Collections.emptyList(), "test-query"), new Describe("S_0", ExecutionType.STATEMENT), Sync.INSTANCE).thenRespond(ParseComplete.INSTANCE)
            .build();

        IndefiniteStatementCache statementCache = new IndefiniteStatementCache(client);

        statementCache.getName("test-query")
            .as(StepVerifier::create)
            .expectNext("S_0")
            .verifyComplete();

        statementCache.getName("test-query")
            .as(StepVerifier::create)
            .expectNext("S_0")
            .verifyComplete();
    }

    @Test
    public void getNameErrorResponse() {
        Client client = TestClient.builder()
            .expectRequest(new Parse("S_0", Collections.emptyList(), "test-query"), new Describe("S_0", ExecutionType.STATEMENT), Sync.INSTANCE).thenRespond(new ErrorResponse(Collections.emptyList()))
            .build();

        IndefiniteStatementCache statementCache = new IndefiniteStatementCache(client);

        statementCache.getName("test-query")
            .as(StepVerifier::create)
            .verifyError(PostgresqlServerErrorException.class);
    }

    @Test
    public void getNameNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new IndefiniteStatementCache(NO_OP).getName(null))
            .withMessage("sql must not be null");
    }

}
