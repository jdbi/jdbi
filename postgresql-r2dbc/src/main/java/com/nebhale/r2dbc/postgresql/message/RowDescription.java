/*
 * Copyright 2017-2017 the original author or authors.
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

package com.nebhale.r2dbc.postgresql.message;

import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.readCStringUTF8;

public final class RowDescription implements BackendMessage {

    private final List<Field> fields;

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
        List<Field> fields = IntStream.range(0, in.readShort())
            .mapToObj(i -> Field.decode(in))
            .collect(Collectors.toList());

        return new RowDescription(fields);
    }

    public static final class Field {

        private final short column;

        private final int dataType;

        private final int dataTypeModifier;

        private final short dataTypeSize;

        private final Format format;

        private final String name;

        private final int table;

        public Field(short column, int dataType, int dataTypeModifier, short dataTypeSize, Format format, String name, int table) {
            this.column = column;
            this.dataType = dataType;
            this.dataTypeModifier = dataTypeModifier;
            this.dataTypeSize = dataTypeSize;
            this.format = format;
            this.name = name;
            this.table = table;
        }

        static Field decode(ByteBuf in) {
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
