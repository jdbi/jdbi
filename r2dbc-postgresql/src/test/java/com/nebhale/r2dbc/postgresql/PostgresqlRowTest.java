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

import com.nebhale.r2dbc.postgresql.codec.MockCodecs;
import com.nebhale.r2dbc.postgresql.message.backend.DataRow;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class PostgresqlRowTest {

    @Test
    public void constructorNoCodecs() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlRow(null, Collections.emptyList()))
            .withMessage("codecs must not be null");
    }

    @Test
    public void constructorNoColumns() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlRow(MockCodecs.EMPTY, null))
            .withMessage("columns must not be null");
    }

    @Test
    public void toRow() {
        PostgresqlRow row = PostgresqlRow.toRow(MockCodecs.EMPTY, new DataRow(Collections.singletonList(Unpooled.buffer().writeInt(100))));

        assertThat(row.getColumns()).hasSize(1);
    }

    @Test
    public void toRowNoCodec() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlRow.toRow(null, new DataRow(Collections.singletonList(Unpooled.buffer().writeInt(100)))))
            .withMessage("codecs must not be null");
    }

    @Test
    public void toRowNoDataRow() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlRow.toRow(MockCodecs.EMPTY, null))
            .withMessage("dataRow must not be null");
    }

}
