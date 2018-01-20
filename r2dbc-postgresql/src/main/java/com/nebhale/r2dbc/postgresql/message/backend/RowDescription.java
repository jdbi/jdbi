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

package com.nebhale.r2dbc.postgresql.message.backend;

import com.nebhale.r2dbc.postgresql.message.Format;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageUtils.readCStringUTF8;

/**
 * The RowDescription message.
 */
public final class RowDescription implements BackendMessage {

    private final List<Field> fields;

    /**
     * Creates a new message.
     *
     * @param fields the fields
     * @throws NullPointerException if {@code fields} is {@code null}
     */
    public RowDescription(List<Field> fields) {
        this.fields = Objects.requireNonNull(fields, "fields must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RowDescription that = (RowDescription) o;
        return Objects.equals(this.fields, that.fields);
    }

    /**
     * Returns the fields.
     *
     * @return the fields
     */
    public List<Field> getFields() {
        return this.fields;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fields);
    }

    @Override
    public String toString() {
        return "RowDescription{" +
            "fields=" + this.fields +
            '}';
    }

    static RowDescription decode(ByteBuf in) {
        Objects.requireNonNull(in, "in must not be null");

        List<Field> fields = IntStream.range(0, in.readShort())
            .mapToObj(i -> Field.decode(in))
            .collect(Collectors.toList());

        return new RowDescription(fields);
    }

    /**
     * The field within the {@link RowDescription}.
     */
    public static final class Field {

        private final short column;

        private final int dataType;

        private final int dataTypeModifier;

        private final short dataTypeSize;

        private final Format format;

        private final String name;

        private final int table;

        /**
         * Creates a new message.
         *
         * @param column           the attribute number of the column
         * @param dataType         the object ID of the field's data type
         * @param dataTypeModifier the type modifier
         * @param dataTypeSize     the data type size
         * @param format           the format
         * @param name             the field name
         * @param table            the object ID of the table
         * @throws NullPointerException if {@code format} or {@code name} is {@code null}
         */
        public Field(short column, int dataType, int dataTypeModifier, short dataTypeSize, Format format, String name, int table) {
            this.column = column;
            this.dataType = dataType;
            this.dataTypeModifier = dataTypeModifier;
            this.dataTypeSize = dataTypeSize;
            this.format = Objects.requireNonNull(format, "format must not be null");
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.table = table;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Field that = (Field) o;
            return this.column == that.column &&
                this.dataType == that.dataType &&
                this.dataTypeModifier == that.dataTypeModifier &&
                this.dataTypeSize == that.dataTypeSize &&
                this.table == that.table &&
                this.format == that.format &&
                Objects.equals(this.name, that.name);
        }

        /**
         * Returns the attribute number of the column.
         *
         * @return the attribute number of the column
         */
        public short getColumn() {
            return this.column;
        }

        /**
         * Returns the object ID of the field's data type.
         *
         * @return the object ID of the field's data type
         */
        public int getDataType() {
            return this.dataType;
        }

        /**
         * Returns the type modifier.
         *
         * @return the type modifier
         */
        public int getDataTypeModifier() {
            return this.dataTypeModifier;
        }

        /**
         * Returns the data type size.
         *
         * @return the data type size
         */
        public short getDataTypeSize() {
            return this.dataTypeSize;
        }

        /**
         * Returns the format.
         *
         * @return the format
         */
        public Format getFormat() {
            return this.format;
        }

        /**
         * Returns the field name.
         *
         * @return the field name
         */
        public String getName() {
            return this.name;
        }

        /**
         * Returns the object ID of the table.
         *
         * @return the object ID of the table
         */
        public int getTable() {
            return this.table;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.column, this.dataType, this.dataTypeModifier, this.dataTypeSize, this.format, this.name, this.table);
        }

        @Override
        public String toString() {
            return "Field{" +
                "column=" + this.column +
                ", dataType=" + this.dataType +
                ", dataTypeModifier=" + this.dataTypeModifier +
                ", dataTypeSize=" + this.dataTypeSize +
                ", format=" + this.format +
                ", name='" + this.name + '\'' +
                ", table=" + this.table +
                '}';
        }

        static Field decode(ByteBuf in) {
            Objects.requireNonNull(in, "in must not be null");

            String name = readCStringUTF8(in);
            int table = in.readInt();
            short column = in.readShort();
            int dataType = in.readInt();
            short dataTypeSize = in.readShort();
            int dataTypeModifier = in.readInt();
            Format format = Format.valueOf(in.readShort());

            return new Field(column, dataType, dataTypeModifier, dataTypeSize, format, name, table);
        }

    }

}
