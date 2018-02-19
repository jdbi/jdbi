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

import com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType;
import io.netty.buffer.ByteBuf;
import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.CODE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.COLUMN_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.CONSTRAINT_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.DATA_TYPE_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.DETAIL;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.FILE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.HINT;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.INTERNAL_POSITION;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.INTERNAL_QUERY;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.LINE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.MESSAGE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.POSITION;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.ROUTINE;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.SCHEMA_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.SEVERITY_LOCALIZED;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.SEVERITY_NON_LOCALIZED;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.TABLE_NAME;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.UNKNOWN;
import static com.nebhale.r2dbc.postgresql.message.backend.Field.FieldType.WHERE;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static io.netty.util.CharsetUtil.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class FieldTest {

    @Test
    public void constructorNoType() {
        assertThatNullPointerException().isThrownBy(() -> new Field(null, "test-value"))
            .withMessage("type must not be null");
    }

    @Test
    public void constructorNoValue() {
        assertThatNullPointerException().isThrownBy(() -> new Field(CODE, null))
            .withMessage("value must not be null");
    }

    @Test
    public void decode() {
        ByteBuf buffer = TEST.buffer()
            .writeByte('C');

        buffer.writeCharSequence("test-value", UTF_8);
        buffer.writeByte(0);
        buffer.writeByte(0);

        assertThat(Field.decode(buffer)).containsExactly(new Field(CODE, "test-value"));
    }

    @Test
    public void valueOfCode() {
        assertThat(FieldType.valueOf((byte) 'C')).isEqualTo(CODE);
    }

    @Test
    public void valueOfColumnName() {
        assertThat(FieldType.valueOf((byte) 'c')).isEqualTo(COLUMN_NAME);
    }

    @Test
    public void valueOfConstraintName() {
        assertThat(FieldType.valueOf((byte) 'n')).isEqualTo(CONSTRAINT_NAME);
    }

    @Test
    public void valueOfDataTypeName() {
        assertThat(FieldType.valueOf((byte) 'd')).isEqualTo(DATA_TYPE_NAME);
    }

    @Test
    public void valueOfDetail() {
        assertThat(FieldType.valueOf((byte) 'D')).isEqualTo(DETAIL);
    }

    @Test
    public void valueOfFile() {
        assertThat(FieldType.valueOf((byte) 'F')).isEqualTo(FILE);
    }

    @Test
    public void valueOfHint() {
        assertThat(FieldType.valueOf((byte) 'H')).isEqualTo(HINT);
    }

    @Test
    public void valueOfInternalPosition() {
        assertThat(FieldType.valueOf((byte) 'p')).isEqualTo(INTERNAL_POSITION);
    }

    @Test
    public void valueOfInternalQuery() {
        assertThat(FieldType.valueOf((byte) 'q')).isEqualTo(INTERNAL_QUERY);
    }

    @Test
    public void valueOfLine() {
        assertThat(FieldType.valueOf((byte) 'L')).isEqualTo(LINE);
    }

    @Test
    public void valueOfMessage() {
        assertThat(FieldType.valueOf((byte) 'M')).isEqualTo(MESSAGE);
    }

    @Test
    public void valueOfPosition() {
        assertThat(FieldType.valueOf((byte) 'P')).isEqualTo(POSITION);
    }

    @Test
    public void valueOfRoutine() {
        assertThat(FieldType.valueOf((byte) 'R')).isEqualTo(ROUTINE);
    }

    @Test
    public void valueOfSchemaName() {
        assertThat(FieldType.valueOf((byte) 's')).isEqualTo(SCHEMA_NAME);
    }

    @Test
    public void valueOfSeverityLocalized() {
        assertThat(FieldType.valueOf((byte) 'S')).isEqualTo(SEVERITY_LOCALIZED);
    }

    @Test
    public void valueOfSeverityNonLocalized() {
        assertThat(FieldType.valueOf((byte) 'V')).isEqualTo(SEVERITY_NON_LOCALIZED);
    }

    @Test
    public void valueOfTableName() {
        assertThat(FieldType.valueOf((byte) 't')).isEqualTo(TABLE_NAME);
    }

    @Test
    public void valueOfUnknown() {
        assertThat(FieldType.valueOf((byte) 'X')).isEqualTo(UNKNOWN);
    }

    @Test
    public void valueOfWhere() {
        assertThat(FieldType.valueOf((byte) 'W')).isEqualTo(WHERE);
    }

}
