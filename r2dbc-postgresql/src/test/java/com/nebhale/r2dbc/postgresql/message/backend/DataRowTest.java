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

import org.junit.Test;

import java.util.Collections;

import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageAssert.assertThat;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class DataRowTest {

    @Test
    public void constructorNoColumns() {
        assertThatNullPointerException().isThrownBy(() -> new DataRow(null))
            .withMessage("columns must not be null");
    }

    @Test
    public void decode() {
        assertThat(DataRow.class)
            .decoded(buffer -> buffer
                .writeShort(1)
                .writeInt(4)
                .writeInt(100))
            .isEqualTo(new DataRow(Collections.singletonList(TEST.buffer(4).writeInt(100))));
    }

    @Test
    public void decodeNullColumn() {
        assertThat(DataRow.class)
            .decoded(buffer -> buffer
                .writeShort(1)
                .writeInt(-1))
            .isEqualTo(new DataRow(Collections.singletonList(null)));
    }

}
