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
import com.nebhale.r2dbc.postgresql.client.PortalNameSupplier;
import com.nebhale.r2dbc.postgresql.client.TestClient;
import com.nebhale.r2dbc.postgresql.message.Format;
import com.nebhale.r2dbc.postgresql.message.backend.BindComplete;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.NoData;
import com.nebhale.r2dbc.postgresql.message.frontend.Bind;
import com.nebhale.r2dbc.postgresql.message.frontend.Describe;
import com.nebhale.r2dbc.postgresql.message.frontend.Execute;
import com.nebhale.r2dbc.postgresql.message.frontend.ExecutionType;
import com.nebhale.r2dbc.postgresql.message.frontend.Sync;
import com.nebhale.r2dbc.postgresql.util.ByteBufUtils;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import static com.nebhale.r2dbc.postgresql.client.TestClient.NO_OP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ExtendedQueryPostgresqlStatementTest {

    private final StatementCache statementCache = mock(StatementCache.class, RETURNS_SMART_NULLS);

    @Test
    public void bindNoParameters() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, () -> "", "test-query", this.statementCache).bind(null))
            .withMessage("parameters must not be null");
    }

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(null, () -> "", "test-query", this.statementCache))
            .withMessage("client must not be null");
    }

    @Test
    public void constructorNoPortalNameSupplier() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, null, "test-query", this.statementCache).bind(null))
            .withMessage("portalNameSupplier must not be null");
    }

    @Test
    public void constructorNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, () -> "", null, this.statementCache))
            .withMessage("sql must not be null");
    }

    @Test
    public void execute() {
        Client client = TestClient.builder()
            .expectRequest(
                new Bind("B_0", Collections.singletonList(Format.TEXT), Collections.singletonList(ByteBufUtils.encode(Unpooled.buffer(), "100")), Collections.emptyList(), "test-name"),
                new Describe("B_0", ExecutionType.PORTAL),
                new Execute("B_0", 0),
                new Bind("B_1", Collections.singletonList(Format.TEXT), Collections.singletonList(ByteBufUtils.encode(Unpooled.buffer(), "200")), Collections.emptyList(), "test-name"),
                new Describe("B_1", ExecutionType.PORTAL),
                new Execute("B_1", 0),
                Sync.INSTANCE)
            .thenRespond(
                BindComplete.INSTANCE, NoData.INSTANCE, new CommandComplete("test", null, null),
                BindComplete.INSTANCE, NoData.INSTANCE, new CommandComplete("test", null, null)
            )
            .build();

        PortalNameSupplier portalNameSupplier = new LinkedList<>(Arrays.asList("B_0", "B_1"))::remove;

        when(this.statementCache.getName("test-query-$1")).thenReturn(Mono.just("test-name"));

        new ExtendedQueryPostgresqlStatement(client, portalNameSupplier, "test-query-?", this.statementCache)
            .bind(Collections.singletonList(100))
            .bind(Collections.singletonList(200))
            .execute()
            .as(StepVerifier::create)
            .expectNextCount(3)  // TODO: Decrease by 1 when https://github.com/reactor/reactor-core/issues/1033
            .verifyComplete();
    }

    @Test
    public void supportsNoQuestion() {
        assertThat(ExtendedQueryPostgresqlStatement.supports("test-query")).isFalse();
    }

    @Test
    public void supportsNoSql() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryPostgresqlStatement.supports(null))
            .withMessage("sql must not be null");
    }

    @Test
    public void supportsNone() {
        assertThat(ExtendedQueryPostgresqlStatement.supports("test-query-?")).isTrue();
    }

    @Test
    public void supportsQueryEmpty() {
        assertThat(ExtendedQueryPostgresqlStatement.supports(" ")).isFalse();
    }

    @Test
    public void supportsSemicolon() {
        assertThat(ExtendedQueryPostgresqlStatement.supports("test-query-1; test-query-2")).isFalse();
    }

}
