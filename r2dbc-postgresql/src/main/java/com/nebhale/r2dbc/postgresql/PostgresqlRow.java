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

import com.nebhale.r2dbc.postgresql.codec.Codecs;
import com.nebhale.r2dbc.postgresql.message.Format;
import com.nebhale.r2dbc.postgresql.message.backend.DataRow;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription;
import com.nebhale.r2dbc.spi.Row;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static reactor.function.TupleUtils.function;

/**
 * An implementation of {@link Row} for a PostgreSQL database.
 */
public final class PostgresqlRow implements Row {

    private final Codecs codecs;

    private final List<Column> columns;

    private final AtomicBoolean isReleased = new AtomicBoolean(false);

    private final Map<String, Column> nameKeyedColumns;

    PostgresqlRow(Codecs codecs, List<Column> columns) {
        this.codecs = requireNonNull(codecs, "codecs must not be null");
        this.columns = requireNonNull(columns, "columns must not be null");

        this.nameKeyedColumns = this.columns.stream()
            .collect(Collectors.toMap(Column::getName, Function.identity()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostgresqlRow that = (PostgresqlRow) o;
        return Objects.equals(this.columns, that.columns);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code identifier} is not a {@link String} or {@link Integer}
     * @throws NullPointerException     if {@code identifier} or {@code type} is {@code null}
     */
    @Override
    public <T> T get(Object identifier, Class<T> type) {
        requireNonNull(identifier, "identifier must not be null");
        requireNonNull(type, "type must not be null");
        requireNotReleased();

        Column column;
        if (identifier instanceof Integer) {
            column = getColumn((Integer) identifier);
        } else if (identifier instanceof String) {
            column = getColumn((String) identifier);
        } else {
            throw new IllegalArgumentException(String.format("Identifier '%s' is not a valid identifier. Should either be an Integer index or a String column name.", identifier));
        }

        return this.codecs.decode(column.getByteBuf(), column.getDataType(), column.getFormat(), type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.columns);
    }

    @Override
    public String toString() {
        return "PostgresqlRow{" +
            "codecs=" + this.codecs +
            ", columns=" + this.columns +
            ", isReleased=" + this.isReleased +
            ", nameKeyedColumns=" + this.nameKeyedColumns +
            '}';
    }

    static PostgresqlRow toRow(Codecs codecs, DataRow dataRow, RowDescription rowDescription) {
        requireNonNull(codecs, "codecs must not be null");
        requireNonNull(dataRow, "dataRow must not be null");
        requireNonNull(rowDescription, "rowDescription must not be null");

        List<Column> columns = Flux
            .zip(
                Flux.fromIterable(dataRow.getColumns()),
                Flux.fromIterable(rowDescription.getFields()))
            .map(function((column, field) -> new Column(column, field.getDataType(), field.getFormat(), field.getName())))
            .collectList()
            .block();

        dataRow.release();

        return new PostgresqlRow(codecs, columns);
    }

    void release() {
        this.columns.forEach(Column::release);
        this.isReleased.set(true);
    }

    private Column getColumn(String name) {
        if (!this.nameKeyedColumns.containsKey(name)) {
            throw new IllegalArgumentException(String.format("Column name '%s' does not exist in column names %s", name, this.nameKeyedColumns.keySet()));
        }

        return this.nameKeyedColumns.get(name);
    }

    private Column getColumn(Integer index) {
        if (index >= this.columns.size()) {
            throw new IllegalArgumentException(String.format("Column index %d is larger than the number of columns %d", index, this.columns.size()));
        }

        return this.columns.get(index);
    }

    private void requireNotReleased() {
        if (this.isReleased.get()) {
            throw new IllegalStateException("Value cannot be retrieved after row has been released");
        }
    }

    static final class Column {

        private final ByteBuf byteBuf;

        private final Integer dataType;

        private final Format format;

        private final String name;

        Column(ByteBuf byteBuf, Integer dataType, Format format, String name) {
            this.byteBuf = byteBuf == null ? null : byteBuf.retain();
            this.dataType = requireNonNull(dataType, "dataType must not be null");
            this.format = requireNonNull(format, "format must not be null");
            this.name = requireNonNull(name, "name must not be null");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Column that = (Column) o;
            return Objects.equals(this.byteBuf, that.byteBuf) &&
                Objects.equals(this.dataType, that.dataType) &&
                this.format == that.format &&
                Objects.equals(this.name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.byteBuf, this.dataType, this.format, this.name);
        }

        @Override
        public String toString() {
            return "Column{" +
                "byteBuf=" + this.byteBuf +
                ", dataType=" + this.dataType +
                ", format=" + this.format +
                ", name='" + this.name + '\'' +
                '}';
        }

        private ByteBuf getByteBuf() {
            return this.byteBuf;
        }

        private Integer getDataType() {
            return this.dataType;
        }

        private Format getFormat() {
            return this.format;
        }

        private String getName() {
            return this.name;
        }

        private void release() {
            if (this.byteBuf != null) {
                this.byteBuf.release();
            }
        }

    }

}
