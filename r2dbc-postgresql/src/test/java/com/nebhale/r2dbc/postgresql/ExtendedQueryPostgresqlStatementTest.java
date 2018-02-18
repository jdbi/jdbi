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
import com.nebhale.r2dbc.postgresql.client.PortalNameSupplier;
import com.nebhale.r2dbc.postgresql.client.TestClient;
import com.nebhale.r2dbc.postgresql.codec.MockCodecs;
import com.nebhale.r2dbc.postgresql.message.backend.BindComplete;
import com.nebhale.r2dbc.postgresql.message.backend.CloseComplete;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.backend.NoData;
import com.nebhale.r2dbc.postgresql.message.frontend.Bind;
import com.nebhale.r2dbc.postgresql.message.frontend.Close;
import com.nebhale.r2dbc.postgresql.message.frontend.Describe;
import com.nebhale.r2dbc.postgresql.message.frontend.Execute;
import com.nebhale.r2dbc.postgresql.message.frontend.ExecutionType;
import com.nebhale.r2dbc.postgresql.message.frontend.Sync;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;

import static com.nebhale.r2dbc.postgresql.client.TestClient.NO_OP;
import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.INT4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ExtendedQueryPostgresqlStatementTest {

    private final Parameter parameter = new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100));

    private final MockCodecs codecs = MockCodecs.builder().encoding(100, this.parameter).build();

    private final StatementCache statementCache = mock(StatementCache.class, RETURNS_SMART_NULLS);

    private final ExtendedQueryPostgresqlStatement statement = new ExtendedQueryPostgresqlStatement(NO_OP, this.codecs, () -> "", "test-query-$1", this.statementCache);

    @Test
    public void bind() {
        assertThat(this.statement.bind("$1", 100).getCurrentBinding()).isEqualTo(new Binding().add(0, this.parameter));
    }

    @Test
    public void bindIndex() {
        assertThat(this.statement.bind(0, 100).getCurrentBinding()).isEqualTo(new Binding().add(0, this.parameter));
    }

    @Test
    public void bindIndexNoIndex() {
        assertThatNullPointerException().isThrownBy(() -> this.statement.bind(null, null))
            .withMessage("index must not be null");
    }

    @Test
    public void bindIndexOptional() {
        Optional<Integer> optional = Optional.of(100);

        assertThat(this.statement.bind(0, optional).getCurrentBinding()).isEqualTo(new Binding().add(0, this.parameter));
    }

    @Test
    public void bindIndexOptionalEmpty() {
        MockCodecs codecs = MockCodecs.builder()
            .encoding(null, this.parameter)
            .build();

        Optional<Integer> optional = Optional.empty();

        assertThat(new ExtendedQueryPostgresqlStatement(NO_OP, codecs, () -> "", "test-query-$1", this.statementCache).bind(0, optional).getCurrentBinding())
            .isEqualTo(new Binding().add(0, this.parameter));
    }

    @Test
    public void bindNoIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> this.statement.bind((String) null, null))
            .withMessage("identifier must not be null");
    }

    @Test
    public void bindNull() {
        assertThat(this.statement.bindNull("$1", 100).getCurrentBinding())
            .isEqualTo(new Binding().add(0, new Parameter(BINARY, 100, null)));
    }

    @Test
    public void bindNullNoIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> this.statement.bindNull(null, 100))
            .withMessage("identifier must not be null");
    }

    @Test
    public void bindNullNoType() {
        assertThatNullPointerException().isThrownBy(() -> this.statement.bindNull("$1", null))
            .withMessage("type must not be null");
    }

    @Test
    public void bindNullWrongIdentifierFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.statement.bindNull("foo", 100))
            .withMessage("Identifier 'foo' is not a valid identifier. Should be of the pattern '.*\\$([\\d]+).*'.");
    }

    @Test
    public void bindNullWrongIdentifierType() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.statement.bindNull(new Object(), 100))
            .withMessage("identifier must be a String");
    }

    @Test
    public void bindNullWrongTypeType() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.statement.bindNull("$1", new Object()))
            .withMessage("type must be a Integer");
    }

    @Test
    public void bindWrongIdentifierFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.statement.bind("foo", ""))
            .withMessage("Identifier 'foo' is not a valid identifier. Should be of the pattern '.*\\$([\\d]+).*'.");
    }

    @Test
    public void bindWrongIdentifierType() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.statement.bind(new Object(), ""))
            .withMessage("identifier must be a String");
    }

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(null, MockCodecs.EMPTY, () -> "", "test-query", this.statementCache))
            .withMessage("client must not be null");
    }

    @Test
    public void constructorNoCodecs() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, null, () -> "", "test-query", this.statementCache))
            .withMessage("codecs must not be null");
    }

    @Test
    public void constructorNoPortalNameSupplier() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, MockCodecs.EMPTY, null, "test-query", this.statementCache))
            .withMessage("portalNameSupplier must not be null");
    }

    @Test
    public void constructorNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, MockCodecs.EMPTY, () -> "", null, this.statementCache))
            .withMessage("sql must not be null");
    }

    @Test
    public void constructorNoStatementCache() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, MockCodecs.EMPTY, () -> "", "test-query", null))
            .withMessage("statementCache must not be null");
    }

    @Test
    public void execute() {
        Client client = TestClient.builder()
            .expectRequest(
                new Bind("B_0", Collections.singletonList(BINARY), Collections.singletonList(Unpooled.buffer(4).writeInt(100)), Collections.emptyList(), "test-name"),
                new Describe("B_0", ExecutionType.PORTAL),
                new Execute("B_0", 0),
                new Close("B_0", ExecutionType.PORTAL),
                new Bind("B_1", Collections.singletonList(BINARY), Collections.singletonList(Unpooled.buffer(4).writeInt(200)), Collections.emptyList(), "test-name"),
                new Describe("B_1", ExecutionType.PORTAL),
                new Execute("B_1", 0),
                new Close("B_1", ExecutionType.PORTAL),
                Sync.INSTANCE)
            .thenRespond(
                BindComplete.INSTANCE, NoData.INSTANCE, new CommandComplete("test", null, null), CloseComplete.INSTANCE,
                BindComplete.INSTANCE, NoData.INSTANCE, new CommandComplete("test", null, null), CloseComplete.INSTANCE
            )
            .build();

        MockCodecs codecs = MockCodecs.builder()
            .encoding(100, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100)))
            .encoding(200, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(200)))
            .build();

        PortalNameSupplier portalNameSupplier = new LinkedList<>(Arrays.asList("B_0", "B_1"))::remove;

        when(this.statementCache.getName(new Binding().add(0, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100))), "test-query-$1")).thenReturn(Mono.just("test-name"));
        when(this.statementCache.getName(new Binding().add(0, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(200))), "test-query-$1")).thenReturn(Mono.just("test-name"));

        new ExtendedQueryPostgresqlStatement(client, codecs, portalNameSupplier, "test-query-$1", this.statementCache)
            .bind("$1", 100)
            .add()
            .bind("$1", 200)
            .add()
            .execute()
            .as(StepVerifier::create)
            .expectNextCount(3)  // TODO: Decrease by 1 when https://github.com/reactor/reactor-core/issues/1033
            .verifyComplete();
    }

    @Test
    public void executeEmpty() {
        assertThatIllegalStateException().isThrownBy(this.statement::execute)
            .withMessage("No parameters have been bound");
    }

    @Test
    public void executeWithoutAdd() {
        Client client = TestClient.builder()
            .expectRequest(
                new Bind("B_0", Collections.singletonList(BINARY), Collections.singletonList(Unpooled.buffer(4).writeInt(100)), Collections.emptyList(), "test-name"),
                new Describe("B_0", ExecutionType.PORTAL),
                new Execute("B_0", 0),
                new Close("B_0", ExecutionType.PORTAL),
                Sync.INSTANCE)
            .thenRespond(
                BindComplete.INSTANCE, NoData.INSTANCE, new CommandComplete("test", null, null), CloseComplete.INSTANCE)
            .build();

        MockCodecs codecs = MockCodecs.builder()
            .encoding(100, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100)))
            .build();

        PortalNameSupplier portalNameSupplier = new LinkedList<>(Arrays.asList("B_0", "B_1"))::remove;

        when(this.statementCache.getName(new Binding().add(0, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100))), "test-query-$1")).thenReturn(Mono.just("test-name"));

        new ExtendedQueryPostgresqlStatement(client, codecs, portalNameSupplier, "test-query-$1", this.statementCache)
            .bind("$1", 100)
            .execute()
            .as(StepVerifier::create)
            .expectNextCount(2)  // TODO: Decrease by 1 when https://github.com/reactor/reactor-core/issues/1033
            .verifyComplete();
    }

    @Test
    public void supportsNoSql() {
        assertThatNullPointerException().isThrownBy(() -> ExtendedQueryPostgresqlStatement.supports(null))
            .withMessage("sql must not be null");
    }

    @Test
    public void supportsParameterSymbol() {
        assertThat(ExtendedQueryPostgresqlStatement.supports("test-query-$1")).isTrue();
    }

    @Test
    public void supportsQueryEmpty() {
        assertThat(ExtendedQueryPostgresqlStatement.supports(" ")).isFalse();
    }

    @Test
    public void supportsSemicolon() {
        assertThat(ExtendedQueryPostgresqlStatement.supports("test-query-1; test-query-2")).isFalse();
    }

    @Test
    public void supportsSimple() {
        assertThat(ExtendedQueryPostgresqlStatement.supports("test-query")).isFalse();
    }

}
