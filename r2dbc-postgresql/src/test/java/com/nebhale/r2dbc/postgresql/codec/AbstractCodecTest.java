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

package com.nebhale.r2dbc.postgresql.codec;

import com.nebhale.r2dbc.postgresql.client.Parameter;
import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.INT4;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.VARCHAR;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class AbstractCodecTest {

    @Test
    public void canDecode() {
        MockCodec<String> codec = MockCodec.builder(String.class)
            .canDecode(BINARY, VARCHAR)
            .build();

        assertThat(codec.canDecode(null, VARCHAR.getObjectId(), BINARY, String.class)).isFalse();
        assertThat(codec.canDecode(TEST.buffer(0), VARCHAR.getObjectId(), BINARY, Object.class)).isFalse();
        assertThat(codec.canDecode(TEST.buffer(0), VARCHAR.getObjectId(), BINARY, String.class)).isTrue();
    }

    @Test
    public void canDecodeNoFormat() {
        assertThatNullPointerException().isThrownBy(() -> MockCodec.empty(String.class).canDecode(null, 100, null, String.class))
            .withMessage("format must not be null");
    }

    @Test
    public void canDecodeNoType() {
        assertThatNullPointerException().isThrownBy(() -> MockCodec.empty(String.class).canDecode(null, 100, BINARY, null))
            .withMessage("type must not be null");
    }

    @Test
    public void canEncode() {
        assertThat(MockCodec.empty(String.class).canEncode("")).isTrue();
        assertThat(MockCodec.empty(String.class).canEncode(new Object())).isFalse();
    }

    @Test
    public void canEncodeNoValue() {
        assertThatNullPointerException().isThrownBy(() -> MockCodec.empty(String.class).canEncode(null))
            .withMessage("value must not be null");
    }

    @Test
    public void constructorNoType() {
        assertThatNullPointerException().isThrownBy(() -> MockCodec.empty(null))
            .withMessage("type must not be null");
    }

    @Test
    public void create() {
        Parameter parameter = AbstractCodec.create(TEXT, INT4, TEST.buffer(4).writeInt(100));

        assertThat(parameter).isEqualTo(new Parameter(TEXT, INT4.getObjectId(), TEST.buffer(4).writeInt(100)));
    }

    @Test
    public void createNoFormat() {
        assertThatNullPointerException().isThrownBy(() -> AbstractCodec.create(null, INT4, null))
            .withMessage("format must not be null");
    }

    @Test
    public void createNoType() {
        assertThatNullPointerException().isThrownBy(() -> AbstractCodec.create(TEXT, null, null))
            .withMessage("type must not be null");
    }

    @Test
    public void encode() {
        Parameter parameter = new Parameter(TEXT, INT4.getObjectId(), TEST.buffer(4).writeInt(100));
        Object value = new Object();

        MockCodec<Object> codec = MockCodec.builder(Object.class)
            .encoding(value, parameter)
            .build();

        assertThat(codec.doEncode(value)).isSameAs(parameter);
    }

    @Test
    public void encodeNoValue() {
        assertThatNullPointerException().isThrownBy(() -> MockCodec.empty(Object.class).encode(null))
            .withMessage("value must not be null");
    }

}
