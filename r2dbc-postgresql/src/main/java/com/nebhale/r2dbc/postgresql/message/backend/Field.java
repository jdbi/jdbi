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

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageUtils.readCStringUTF8;
import static java.util.Objects.requireNonNull;

/**
 * The Field type returned as part of some {@link BackendMessage}s.
 */
public final class Field {

    private final FieldType type;

    private final String value;

    /**
     * Creates a new field.
     *
     * @param type  the type
     * @param value the value
     * @throws NullPointerException if {@code type} or {@code value} is {@code null}
     */
    public Field(FieldType type, String value) {
        this.type = requireNonNull(type, "type must not be null");
        this.value = requireNonNull(value, "value must not be null");
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

    /**
     * Returns the type.
     *
     * @return the type
     */
    public FieldType getType() {
        return this.type;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
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
        requireNonNull(in, "in must not be null");

        List<Field> fields = new ArrayList<>();

        while (in.readableBytes() > 0) {
            byte discriminator = in.readByte();

            if (discriminator == 0) {
                break;
            }

            fields.add(new Field(FieldType.valueOf(discriminator), readCStringUTF8(in)));
        }

        return fields;
    }

    /**
     * An enumeration of field types.
     */
    public enum FieldType {

        /**
         * Code: the SQLSTATE code for the error. Not localizable. Always present.
         */
        CODE('C'),

        /**
         * Column name: if the error was associated with a specific table column, the name of the column. (Refer to the schema and table name fields to identify the table.)
         */
        COLUMN_NAME('c'),

        /**
         * Constraint name: if the error was associated with a specific constraint, the name of the constraint. Refer to fields listed above for the associated table or domain. (For this purpose,
         * indexes are treated as constraints, even if they weren't created with constraint syntax.)
         */
        CONSTRAINT_NAME('n'),

        /**
         * Data type name: if the error was associated with a specific data type, the name of the data type. (Refer to the schema name field for the name of the data type's schema.)
         */
        DATA_TYPE_NAME('d'),

        /**
         * Detail: an optional secondary error message carrying more detail about the problem. Might run to multiple lines.
         */
        DETAIL('D'),

        /**
         * File: the file name of the source-code location where the error was reported.
         */
        FILE('F'),

        /**
         * Hint: an optional suggestion what to do about the problem. This is intended to differ from Detail in that it offers advice (potentially inappropriate) rather than hard facts. Might run
         * to multiple lines.
         */
        HINT('H'),

        /**
         * Internal position: this is defined the same as the P field, but it is used when the cursor position refers to an internally generated command rather than the one submitted by the client.
         * The q field will always appear when this field appears.
         */
        INTERNAL_POSITION('p'),

        /**
         * Internal query: the text of a failed internally-generated command. This could be, for example, a SQL query issued by a PL/pgSQL function.
         */
        INTERNAL_QUERY('q'),

        /**
         * Line: the line number of the source-code location where the error was reported.
         */
        LINE('L'),

        /**
         * Message: the primary human-readable error message. This should be accurate but terse (typically one line). Always present.
         */
        MESSAGE('M'),

        /**
         * Position: the field value is a decimal ASCII integer, indicating an error cursor position as an index into the original query string. The first character has index 1, and positions are
         * measured in characters not bytes.
         */
        POSITION('P'),

        /**
         * Routine: the name of the source-code routine reporting the error.
         */
        ROUTINE('R'),

        /**
         * Schema name: if the error was associated with a specific database object, the name of the schema containing that object, if any.
         */
        SCHEMA_NAME('s'),

        /**
         * Severity: the field contents are {@code ERROR}, {@code FATAL}, or {@code PANIC} (in an error message), or {@code WARNING}, {@code NOTICE}, {@code DEBUG}, {@code INFO}, or {@code LOG} (in
         * a notice message), or a localized translation of one of these. Always present.
         */
        SEVERITY_LOCALIZED('S'),

        /**
         * Severity: the field contents are {@code ERROR}, {@code FATAL}, or {@code PANIC} (in an error message), or {@code WARNING}, {@code NOTICE}, {@code DEBUG}, {@code INFO}, or {@code LOG} (in
         * a notice message). This is identical to the {@code S} field except that the contents are never localized. This is present only in messages generated by PostgreSQL versions 9.6 and later.
         */
        SEVERITY_NON_LOCALIZED('V'),

        /**
         * Table name: if the error was associated with a specific table, the name of the table. (Refer to the schema name field for the name of the table's schema.)
         */
        TABLE_NAME('t'),

        /**
         * An unknown field type.
         */
        UNKNOWN('\0'),

        /**
         * Where: an indication of the context in which the error occurred. Presently this includes a call stack traceback of active procedural language functions and internally-generated queries.
         * The trace is one entry per line, most recent first.
         */
        WHERE('W');

        private final char discriminator;

        FieldType(char discriminator) {
            this.discriminator = discriminator;
        }

        static FieldType valueOf(byte b) {
            return Arrays.stream(FieldType.values())
                .filter(type -> type.discriminator == b)
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}
