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

import com.nebhale.r2dbc.postgresql.PostgresqlRow.Column;
import com.nebhale.r2dbc.postgresql.codec.MockCodecs;
import com.nebhale.r2dbc.postgresql.message.backend.DataRow;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class PostgresqlRowTest {

    private final List<Column> columns = Arrays.asList(
        new Column(TEST.buffer(4).writeInt(100), 200, BINARY, "test-name-1"),
        new Column(TEST.buffer(4).writeInt(300), 400, TEXT, "test-name-2")
    );

    @Test
    public void constructorNoCodecs() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlRow(null, Collections.emptyList()))
            .withMessage("codecs must not be null");
    }

    @Test
    public void constructorNoColumns() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlRow(MockCodecs.empty(), null))
            .withMessage("columns must not be null");
    }

    @Test
    public void getAfterRelease() {
        Object value = new Object();

        MockCodecs codecs = MockCodecs.builder()
            .decoding(TEST.buffer(4).writeInt(300), 400, TEXT, Object.class, value)
            .build();

        PostgresqlRow row = new PostgresqlRow(codecs, this.columns);
        row.release();

        assertThatIllegalStateException().isThrownBy(() -> row.get("test-name-2", Object.class))
            .withMessage("Value cannot be retrieved after row has been released");
    }

    @Test
    public void getIndex() {
        Object value = new Object();

        MockCodecs codecs = MockCodecs.builder()
            .decoding(TEST.buffer(4).writeInt(300), 400, TEXT, Object.class, value)
            .build();

        assertThat(new PostgresqlRow(codecs, this.columns).get(1, Object.class)).isSameAs(value);
    }

    @Test
    public void getInvalidIndex() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PostgresqlRow(MockCodecs.empty(), this.columns).get(2, Object.class))
            .withMessage("Column index 2 is larger than the number of columns 2");
    }

    @Test
    public void getInvalidName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PostgresqlRow(MockCodecs.empty(), this.columns).get("test-name-3", Object.class))
            .withMessage("Column name 'test-name-3' does not exist in column names [test-name-1, test-name-2]");
    }

    @Test
    public void getName() {
        Object value = new Object();

        MockCodecs codecs = MockCodecs.builder()
            .decoding(TEST.buffer(4).writeInt(300), 400, TEXT, Object.class, value)
            .build();

        assertThat(new PostgresqlRow(codecs, this.columns).get("test-name-2", Object.class)).isSameAs(value);
    }

    @Test
    public void getNoIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlRow(MockCodecs.empty(), this.columns).get(null, Object.class))
            .withMessage("identifier must not be null");
    }

    @Test
    public void getNoType() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlRow(MockCodecs.empty(), this.columns).get(new Object(), null))
            .withMessage("type must not be null");
    }

    @Test
    public void getWrongIdentifierType() {
        Object identifier = new Object();

        assertThatIllegalArgumentException().isThrownBy(() -> new PostgresqlRow(MockCodecs.empty(), this.columns).get(identifier, Object.class))
            .withMessage("Identifier '%s' is not a valid identifier. Should either be an Integer index or a String column name.", identifier);
    }

    @Test
    public void toRow() {
        Object value = new Object();

        MockCodecs codecs = MockCodecs.builder()
            .decoding(TEST.buffer(4).writeInt(100), 300, TEXT, Object.class, value)
            .build();

        PostgresqlRow row = PostgresqlRow.toRow(codecs, new DataRow(Collections.singletonList(TEST.buffer(4).writeInt(100))),
            new RowDescription(Collections.singletonList(new RowDescription.Field((short) 200, 300, (short) 400, (short) 500, TEXT, "test-name-1", 600))));

        assertThat(row.get(0, Object.class)).isSameAs(value);
    }

    @Test
    public void toRowNoCodecs() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlRow.toRow(null, new DataRow(Collections.singletonList(TEST.buffer(4).writeInt(100))),
            new RowDescription(Collections.emptyList())))
            .withMessage("codecs must not be null");
    }

    @Test
    public void toRowNoDataRow() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlRow.toRow(MockCodecs.empty(), null, new RowDescription(Collections.emptyList())))
            .withMessage("dataRow must not be null");
    }

    @Test
    public void toRowNoRowDescription() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlRow.toRow(MockCodecs.empty(), new DataRow(Collections.singletonList(TEST.buffer(4).writeInt(100))), null))
            .withMessage("rowDescription must not be null");
    }

}
