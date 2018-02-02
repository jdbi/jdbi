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
import com.nebhale.r2dbc.postgresql.client.ExtendedQueryMessageFlow;
import com.nebhale.r2dbc.postgresql.client.Parameter;
import com.nebhale.r2dbc.postgresql.client.PortalNameSupplier;
import com.nebhale.r2dbc.postgresql.message.Format;
import com.nebhale.r2dbc.postgresql.message.backend.CloseComplete;
import com.nebhale.r2dbc.postgresql.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.BOOL;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.DATE;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.FLOAT4;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.FLOAT8;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.INT2;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.INT4;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.INT8;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.NUMERIC;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.TIME;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.TIMESTAMP;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.TIMESTAMPTZ;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.UNSPECIFIED;
import static com.nebhale.r2dbc.postgresql.PostgresqlObjectId.VARCHAR;
import static com.nebhale.r2dbc.postgresql.client.ExtendedQueryMessageFlow.PARAMETER_SYMBOL;
import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.util.ObjectUtils.requireType;
import static java.util.Objects.requireNonNull;

final class ExtendedQueryPostgresqlStatement implements PostgresqlStatement {

    private static final Parameter UNSPECIFIED_PARAMETER = new Parameter(TEXT, UNSPECIFIED.getObjectId(), null);

    private final Bindings bindings = new Bindings();

    private final Client client;

    private final PortalNameSupplier portalNameSupplier;

    private final String sql;

    private final StatementCache statementCache;

    ExtendedQueryPostgresqlStatement(Client client, PortalNameSupplier portalNameSupplier, String sql, StatementCache statementCache) {
        this.client = requireNonNull(client, "client must not be null");
        this.portalNameSupplier = requireNonNull(portalNameSupplier, "portalNameSupplier must not be null");
        this.sql = requireNonNull(sql, "sql must not be null");
        this.statementCache = requireNonNull(statementCache, "statementCache must not be null");
    }

    @Override
    public ExtendedQueryPostgresqlStatement add() {
        this.bindings.finish();
        return this;
    }

    @Override
    public ExtendedQueryPostgresqlStatement bind(Object identifier, Object value) {
        requireNonNull(identifier, "identifier must not be null");
        requireType(identifier, String.class, "identifier must be a String");

        return bind(getIndex((String) identifier), value);
    }

    @Override
    public ExtendedQueryPostgresqlStatement bind(Integer index, Object value) {
        requireNonNull(index, "index must not be null");

        if (value == null) {
            bindNull(index, UNSPECIFIED);
        } else if (value instanceof BigDecimal) {
            bindBigDecimal(index, (BigDecimal) value);
        } else if (value instanceof Boolean) {
            bindBoolean(index, (Boolean) value);
        } else if (value instanceof Byte) {
            bindByte(index, (Byte) value);
        } else if (value instanceof Character) {
            bindCharacter(index, (Character) value);
        } else if (value instanceof Date) {
            bindDate(index, (Date) value);
        } else if (value instanceof Double) {
            bindDouble(index, (Double) value);
        } else if (value instanceof Enum) {
            bindEnum(index, (Enum) value);
        } else if (value instanceof Float) {
            bindFloat(index, (Float) value);
        } else if (value instanceof Inet4Address) {
            bindInet4Address(index, (Inet4Address) value);
        } else if (value instanceof Inet6Address) {
            bindInet6Address(index, (Inet6Address) value);
        } else if (value instanceof Instant) {
            bindInstant(index, (Instant) value);
        } else if (value instanceof Integer) {
            bindInteger(index, (Integer) value);
        } else if (value instanceof LocalDate) {
            bindLocalDate(index, (LocalDate) value);
        } else if (value instanceof LocalDateTime) {
            bindLocalDateTime(index, (LocalDateTime) value);
        } else if (value instanceof LocalTime) {
            bindLocalTime(index, (LocalTime) value);
        } else if (value instanceof Long) {
            bindLong(index, (Long) value);
        } else if (value instanceof Optional) {
            bindOptional(index, (Optional<?>) value);
        } else if (value instanceof OffsetDateTime) {
            bindOffsetDateTime(index, (OffsetDateTime) value);
        } else if (value instanceof Short) {
            bindShort(index, (Short) value);
        } else if (value instanceof String) {
            bindString(index, (String) value);
        } else if (value instanceof URI) {
            bindUri(index, (URI) value);
        } else if (value instanceof URL) {
            bindUrl(index, (URL) value);
        } else if (value instanceof UUID) {
            bindUuid(index, (UUID) value);
        } else if (value instanceof ZonedDateTime) {
            bindZonedDateTime(index, (ZonedDateTime) value);
        } else if (value instanceof ZoneId) {
            bindZoneId(index, (ZoneId) value);
        } else {
            throw new IllegalArgumentException(String.format("Unknown parameter of type %s", value.getClass().getName()));
        }

        return this;
    }

    @Override
    public ExtendedQueryPostgresqlStatement bindNull(Object identifier, Object type) {
        requireNonNull(identifier, "identifier must not be null");
        requireType(identifier, String.class, "identifier must be a String");
        requireNonNull(type, "type must not be null");
        requireType(type, PostgresqlObjectId.class, "type must be a PostgresqlObjectId");

        bindNull(getIndex((String) identifier), (PostgresqlObjectId) type);
        return this;
    }

    @Override
    public Flux<PostgresqlResult> execute() {
        return this.statementCache.getName(this.bindings.first(), this.sql)
            .flatMapMany(name -> ExtendedQueryMessageFlow.execute(Flux.fromStream(this.bindings.stream()), this.client, this.portalNameSupplier, name))
            .windowUntil(CloseComplete.class::isInstance)
            .map(PostgresqlResult::toResult);
    }

    @Override
    public String toString() {
        return "ExtendedQueryPostgresqlStatement{" +
            "bindings=" + this.bindings +
            ", client=" + this.client +
            ", portalNameSupplier=" + this.portalNameSupplier +
            ", sql='" + this.sql + '\'' +
            ", statementCache=" + this.statementCache +
            '}';
    }

    static boolean supports(String sql) {
        requireNonNull(sql, "sql must not be null");

        return !sql.trim().isEmpty() && !sql.contains(";") && PARAMETER_SYMBOL.matcher(sql).matches();
    }

    Binding getCurrentBinding() {
        return this.bindings.getCurrent();
    }

    private void addToCurrentBinding(Integer index, Format format, PostgresqlObjectId type, ByteBuf value) {
        this.bindings.getCurrent().add(index, new Parameter(format, type.getObjectId(), value));
    }

    private void bindBigDecimal(Integer index, BigDecimal value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value.toString());
        addToCurrentBinding(index, TEXT, NUMERIC, encoded);
    }

    private void bindBoolean(Integer index, Boolean value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value ? "TRUE" : "FALSE");
        addToCurrentBinding(index, TEXT, BOOL, encoded);
    }

    private void bindByte(Integer index, Byte value) {
        bindShort(index, (short) value);
    }

    private void bindCharacter(Integer index, Character value) {
        bindString(index, value.toString());
    }

    private void bindDate(Integer index, Date value) {
        bindInstant(index, value.toInstant());
    }

    private void bindDouble(Integer index, Double value) {
        ByteBuf encoded = this.client.getByteBufAllocator().buffer(8).writeDouble(value);
        addToCurrentBinding(index, BINARY, FLOAT8, encoded);
    }

    private void bindEnum(Integer index, Enum<?> value) {
        bindString(index, value.name());
    }

    private void bindFloat(Integer index, Float value) {
        ByteBuf encoded = this.client.getByteBufAllocator().buffer(4).writeFloat(value);
        addToCurrentBinding(index, BINARY, FLOAT4, encoded);
    }

    private void bindInet4Address(Integer index, Inet4Address value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value.getHostAddress());
        addToCurrentBinding(index, TEXT, UNSPECIFIED, encoded);
    }

    private void bindInet6Address(Integer index, Inet6Address value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value.getHostAddress());
        addToCurrentBinding(index, TEXT, UNSPECIFIED, encoded);
    }

    private void bindInstant(Integer index, Instant value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value.toString());
        addToCurrentBinding(index, TEXT, TIMESTAMP, encoded);
    }

    private void bindInteger(Integer index, Integer value) {
        ByteBuf encoded = this.client.getByteBufAllocator().buffer(4).writeInt(value);
        addToCurrentBinding(index, BINARY, INT4, encoded);
    }

    private void bindLocalDate(Integer index, LocalDate value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value.toString());
        addToCurrentBinding(index, TEXT, DATE, encoded);
    }

    private void bindLocalDateTime(Integer index, LocalDateTime value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value.toString());
        addToCurrentBinding(index, TEXT, TIMESTAMP, encoded);
    }

    private void bindLocalTime(Integer index, LocalTime value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value.toString());
        addToCurrentBinding(index, TEXT, TIME, encoded);
    }

    private void bindLong(Integer index, Long value) {
        ByteBuf encoded = this.client.getByteBufAllocator().buffer(8).writeLong(value);
        addToCurrentBinding(index, BINARY, INT8, encoded);
    }

    private void bindNull(Integer index, PostgresqlObjectId type) {
        addToCurrentBinding(index, BINARY, type, null);
    }

    private void bindOffsetDateTime(Integer index, OffsetDateTime value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value.toString());
        addToCurrentBinding(index, TEXT, TIMESTAMPTZ, encoded);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void bindOptional(Integer index, Optional<?> value) {
        bind(index, value.orElse(null));
    }

    private void bindShort(Integer index, Short value) {
        ByteBuf encoded = this.client.getByteBufAllocator().buffer(2).writeShort(value);
        addToCurrentBinding(index, BINARY, INT2, encoded);
    }

    private void bindString(Integer index, String value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value);
        addToCurrentBinding(index, TEXT, VARCHAR, encoded);
    }

    private void bindUri(Integer index, URI uri) {
        bindString(index, uri.toString());
    }

    private void bindUrl(Integer index, URL url) {
        bindString(index, url.toString());
    }

    private void bindUuid(Integer index, UUID value) {
        ByteBuf encoded = this.client.getByteBufAllocator().buffer(16).writeLong(value.getMostSignificantBits()).writeLong(value.getLeastSignificantBits());
        addToCurrentBinding(index, BINARY, PostgresqlObjectId.UUID, encoded);
    }

    private void bindZoneId(Integer index, ZoneId zoneId) {
        bindString(index, zoneId.getId());
    }

    private void bindZonedDateTime(Integer index, ZonedDateTime value) {
        ByteBuf encoded = ByteBufUtils.encode(this.client.getByteBufAllocator(), value.toString());
        addToCurrentBinding(index, TEXT, TIMESTAMPTZ, encoded);
    }

    private int getIndex(String identifier) {
        Matcher matcher = PARAMETER_SYMBOL.matcher(identifier);

        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format("Identifier '%s' is not a valid identifier. Should be of the pattern '%s'.", identifier, PARAMETER_SYMBOL.pattern()));
        }

        return Integer.parseInt(matcher.group(1)) - 1;
    }

    private static final class Bindings {

        private final List<Binding> bindings = new ArrayList<>();

        private Binding current;

        @Override
        public String toString() {
            return "Bindings{" +
                "bindings=" + this.bindings +
                ", current=" + this.current +
                '}';
        }

        private void finish() {
            this.current = null;
        }

        private Binding first() {
            return this.bindings.stream().findFirst().orElseThrow(() -> new IllegalStateException("No parameters have been bound"));
        }

        private Binding getCurrent() {
            if (this.current == null) {
                this.current = new Binding(UNSPECIFIED_PARAMETER);
                this.bindings.add(this.current);
            }

            return this.current;
        }

        private Stream<Binding> stream() {
            return this.bindings.stream();
        }

    }

}
