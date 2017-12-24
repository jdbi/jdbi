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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.readCStringUTF8;

public final class Field {

    private final FieldType type;

    private final String value;

    public Field(FieldType type, String value) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Field field = (Field) o;
        return this.type == field.type &&
            Objects.equals(this.value, field.value);
    }

    public FieldType getType() {
        return this.type;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.value);
    }

    @Override
    public String toString() {
        return "Field{" +
            "type=" + this.type +
            ", value='" + this.value + '\'' +
            '}';
    }

    static List<Field> decode(ByteBuf in) {
        List<Field> fields = new ArrayList<>();

        while (in.readableBytes() > 0) {
            FieldType type = FieldType.valueOf(in.readByte());

            if (FieldType.TERMINAL == type) {
                break;
            }

            fields.add(new Field(type, readCStringUTF8(in)));
        }

        return fields;
    }

    public enum FieldType {

        CODE('C'),
        COLUMN_NAME('c'),
        CONSTRAINT_NAME('n'),
        DATA_TYPE_NAME('d'),
        DETAIL('D'),
        FILE('F'),
        HINT('H'),
        INTERNAL_POSITION('p'),
        INTERNAL_QUERY('q'),
        LINE('L'),
        ROUTINE('R'),
        MESSAGE('M'),
        POSITION('P'),
        SCHEMA_NAME('s'),
        SEVERITY_LOCALIZED('S'),
        SEVERITY_NON_LOCALIZED('V'),
        TABLE_NAME('t'),
        TERMINAL('\0'),
        UNKNOWN('\0'),
        WHERE('W');

        private final char discriminator;

        FieldType(char discriminator) {
            this.discriminator = discriminator;
        }

        @Override
        public String toString() {
            return "FieldType{" +
                "discriminator=" + this.discriminator +
                "} " + super.toString();
        }

        static FieldType valueOf(byte b) {
            return Arrays.stream(FieldType.values())
                .filter(type -> type.discriminator == b)
                .findFirst()
                .orElse(UNKNOWN);
        }
    }

}
