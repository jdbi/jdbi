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
import com.nebhale.r2dbc.postgresql.message.Format;
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

import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.BOOL;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.DATE;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.FLOAT4;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.FLOAT8;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.INT2;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.INT4;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.INT8;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.MONEY;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.NUMERIC;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.TIME;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.TIMESTAMP;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.TIMESTAMPTZ;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.UNSPECIFIED;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.VARCHAR;
import static com.nebhale.r2dbc.postgresql.client.TestClient.NO_OP;
import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.util.ByteBufUtils.encode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ExtendedQueryPostgresqlStatementTest {

    private static final Parameter DEFAULT_PARAMETER = new Parameter(TEXT, 400, null);

    private final StatementCache statementCache = mock(StatementCache.class, RETURNS_SMART_NULLS);

    private final ExtendedQueryPostgresqlStatement statement = new ExtendedQueryPostgresqlStatement(NO_OP, () -> "", "test-query-$1", this.statementCache);

    @Test
    public void bindBigDecimal() {
        BigDecimal bigDecimal = new BigDecimal("100");

        assertThat(this.statement.bind("$1", bigDecimal).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, NUMERIC.getObjectId(), encode(Unpooled.buffer(), "100"))));
    }

    @Test
    public void bindBoolean() {
        assertThat(this.statement.bind("$1", true).bind("$1", false).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER)
                .add(0, new Parameter(Format.TEXT, BOOL.getObjectId(), encode(Unpooled.buffer(), "TRUE")))
                .add(0, new Parameter(Format.TEXT, BOOL.getObjectId(), encode(Unpooled.buffer(), "FALSE"))));
    }

    @Test
    public void bindByte() {
        assertThat(this.statement.bind("$1", (byte) 100).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, INT2.getObjectId(), Unpooled.buffer().writeShort(100))));
    }

    @Test
    public void bindCharacter() {
        assertThat(this.statement.bind("$1", 'A').getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, VARCHAR.getObjectId(), encode(Unpooled.buffer(), "A"))));
    }

    @Test
    public void bindDate() {
        Date date = new Date();

        assertThat(this.statement.bind("$1", date).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, TIMESTAMP.getObjectId(), encode(Unpooled.buffer(), date.toInstant().toString()))));
    }

    @Test
    public void bindDouble() {
        assertThat(this.statement.bind("$1", 100d).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, FLOAT8.getObjectId(), Unpooled.buffer().writeDouble(100))));
    }

    @Test
    public void bindEnum() {
        TimeUnit timeUnit = TimeUnit.DAYS;

        assertThat(this.statement.bind("$1", timeUnit).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, VARCHAR.getObjectId(), encode(Unpooled.buffer(), timeUnit.name()))));
    }

    @Test
    public void bindFloat() {
        assertThat(this.statement.bind("$1", 100f).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, FLOAT4.getObjectId(), Unpooled.buffer().writeFloat(100))));
    }

    @Test
    public void bindIndex() {
        assertThat(this.statement.bind(0, 100).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100))));
    }

    @Test
    public void bindIndexNoIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> this.statement.bind(null, null))
            .withMessage("index must not be null");
    }

    @Test
    public void bindIndexNoType() {
        assertThatNullPointerException().isThrownBy(() -> this.statement.bindNull("$1", null))
            .withMessage("type must not be null");
    }

    @Test
    public void bindInetAddress4() throws UnknownHostException {
        InetAddress inetAddress = Inet4Address.getByName("localhost");

        assertThat(this.statement.bind("$1", inetAddress).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, UNSPECIFIED.getObjectId(), encode(Unpooled.buffer(), inetAddress.getHostAddress()))));
    }

    @Test
    public void bindInetAddress6() throws UnknownHostException {
        InetAddress inetAddress = Inet6Address.getByName("::1");

        assertThat(this.statement.bind("$1", inetAddress).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, UNSPECIFIED.getObjectId(), encode(Unpooled.buffer(), inetAddress.getHostAddress()))));
    }

    @Test
    public void bindInstant() {
        Instant instant = Instant.now();

        assertThat(this.statement.bind("$1", instant).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, TIMESTAMP.getObjectId(), encode(Unpooled.buffer(), instant.toString()))));
    }

    @Test
    public void bindInteger() {
        assertThat(this.statement.bind("$1", 100).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100))));
    }

    @Test
    public void bindLocalDate() {
        LocalDate localDate = LocalDate.now();

        assertThat(this.statement.bind("$1", localDate).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, DATE.getObjectId(), encode(Unpooled.buffer(), localDate.toString()))));
    }

    @Test
    public void bindLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();

        assertThat(this.statement.bind("$1", localDateTime).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, TIMESTAMP.getObjectId(), encode(Unpooled.buffer(), localDateTime.toString()))));
    }

    @Test
    public void bindLocalTime() {
        LocalTime localTime = LocalTime.now();

        assertThat(this.statement.bind("$1", localTime).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, TIME.getObjectId(), encode(Unpooled.buffer(), localTime.toString()))));
    }

    @Test
    public void bindLong() {
        assertThat(this.statement.bind("$1", 100L).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, INT8.getObjectId(), Unpooled.buffer().writeLong(100))));
    }

    @Test
    public void bindNoIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> this.statement.bind((String) null, null))
            .withMessage("identifier must not be null");
    }

    @Test
    public void bindNull() {
        assertThat(this.statement.bindNull("$1", MONEY).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, MONEY.getObjectId(), null)));
    }

    @Test
    public void bindNullNoIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> this.statement.bindNull((String) null, MONEY))
            .withMessage("identifier must not be null");
    }

    @Test
    public void bindNullWrongIdentifierFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.statement.bindNull("foo", MONEY))
            .withMessage("Identifier 'foo' is not a valid identifier. Should be of the pattern '.*\\$([\\d]+).*'.");
    }

    @Test
    public void bindNullWrongIdentifierType() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.statement.bindNull(new Object(), MONEY))
            .withMessage("identifier must be a String");
    }

    @Test
    public void bindNullWrongTypeType() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.statement.bindNull("$1", 1))
            .withMessage("type must be a PostgresqlObjectId");
    }

    @Test
    public void bindOffsetDateTime() {
        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        assertThat(this.statement.bind("$1", offsetDateTime).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, TIMESTAMPTZ.getObjectId(), encode(Unpooled.buffer(), offsetDateTime.toString()))));
    }

    @Test
    public void bindOptional() {
        Optional<Integer> optional = Optional.of(100);

        assertThat(this.statement.bind("$1", optional).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100))));
    }

    @Test
    public void bindOptionalEmpty() {
        Optional<Integer> optional = Optional.empty();

        assertThat(this.statement.bind("$1", optional).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, UNSPECIFIED.getObjectId(), null)));
    }

    @Test
    public void bindShort() {
        assertThat(this.statement.bind("$1", (short) 100).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, INT2.getObjectId(), Unpooled.buffer().writeShort(100))));
    }

    @Test
    public void bindString() {
        String string = "test";

        assertThat(this.statement.bind("$1", string).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, VARCHAR.getObjectId(), encode(Unpooled.buffer(), "test"))));
    }

    @Test
    public void bindUnsupported() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.statement.bind("$1", new Object()))
            .withMessage("Unknown parameter of type java.lang.Object");
    }

    @Test
    public void bindUri() {
        URI uri = URI.create("http://localhost");

        assertThat(this.statement.bind("$1", uri).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, VARCHAR.getObjectId(), encode(Unpooled.buffer(), "http://localhost"))));
    }

    @Test
    public void bindUrl() throws MalformedURLException {
        URL url = new URL("http://localhost");

        assertThat(this.statement.bind("$1", url).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, VARCHAR.getObjectId(), encode(Unpooled.buffer(), "http://localhost"))));
    }

    @Test
    public void bindUuid() {
        UUID uuid = UUID.randomUUID();

        assertThat(this.statement.bind("$1", uuid).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, PostgresqlObjectId.UUID.getObjectId(),
                Unpooled.buffer().writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits()))));
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
    public void bindZoneId() {
        ZoneId zoneId = ZoneId.systemDefault();

        assertThat(this.statement.bind("$1", zoneId).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, VARCHAR.getObjectId(), encode(Unpooled.buffer(), zoneId.getId()))));
    }

    @Test
    public void bindZonedDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.now();

        assertThat(this.statement.bind("$1", zonedDateTime).getCurrentBinding())
            .isEqualTo(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(TEXT, TIMESTAMPTZ.getObjectId(), encode(Unpooled.buffer(), zonedDateTime.toString()))));
    }

    @Test
    public void constructorNoClient() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(null, () -> "", "test-query", this.statementCache))
            .withMessage("client must not be null");
    }

    @Test
    public void constructorNoPortalNameSupplier() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, null, "test-query", this.statementCache))
            .withMessage("portalNameSupplier must not be null");
    }

    @Test
    public void constructorNoSql() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, () -> "", null, this.statementCache))
            .withMessage("sql must not be null");
    }

    @Test
    public void constructorNoStatementCache() {
        assertThatNullPointerException().isThrownBy(() -> new ExtendedQueryPostgresqlStatement(NO_OP, () -> "", "test-query", null))
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

        PortalNameSupplier portalNameSupplier = new LinkedList<>(Arrays.asList("B_0", "B_1"))::remove;

        when(this.statementCache.getName(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100))), "test-query-$1")).thenReturn(Mono.just
            ("test-name"));
        when(this.statementCache.getName(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(200))), "test-query-$1")).thenReturn(Mono.just
            ("test-name"));

        new ExtendedQueryPostgresqlStatement(client, portalNameSupplier, "test-query-$1", this.statementCache)
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

        PortalNameSupplier portalNameSupplier = new LinkedList<>(Arrays.asList("B_0", "B_1"))::remove;

        when(this.statementCache.getName(new Binding(DEFAULT_PARAMETER).add(0, new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer().writeInt(100))), "test-query-$1")).thenReturn(Mono.just
            ("test-name"));

        new ExtendedQueryPostgresqlStatement(client, portalNameSupplier, "test-query-$1", this.statementCache)
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
