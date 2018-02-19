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
import com.nebhale.r2dbc.postgresql.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.INT4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class AbstractCodecTest {

    @Test
    public void canEncode() {
        assertThat(MockCodec.empty(String.class).canEncode("")).isTrue();
        assertThat(MockCodec.empty(String.class).canEncode(new Object())).isFalse();
    }

    @Test
    public void constructorNoType() {
        assertThatNullPointerException().isThrownBy(() -> MockCodec.empty(null))
            .withMessage("type must not be null");
    }

    @Test
    public void create() {
        Parameter parameter = AbstractCodec.create(TEXT, INT4, Unpooled.buffer().writeInt(100));

        assertThat(parameter).isEqualTo(new Parameter(TEXT, INT4.getObjectId(), Unpooled.buffer().writeInt(100)));
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

//    @Test
//    public void decodeBinary() {
//        ByteBuf byteBuf = Unpooled.buffer().writeInt(100);
//        Object value = new Object();
//
//        MockCodec<Object> codec = MockCodec.builder(Object.class)
//            .binaryDecoding(byteBuf, value)
//            .build();
//
//        assertThat(codec.decode(byteBuf, BINARY)).isSameAs(value);
//    }
//
//    @Test
//    public void decodeNoFormat() {
//        assertThatNullPointerException().isThrownBy(() -> MockCodec.empty(Object.class).decode(null, null))
//            .withMessage("format must not be null");
//    }
//
//    @Test
//    public void decodeNull() {
//        assertThat(MockCodec.empty(Object.class).decode(null, TEXT)).isNull();
//    }
//
//    @Test
//    public void decodeText() {
//        ByteBuf byteBuf = ByteBufUtils.encode(Unpooled.buffer(), "test-value");
//        Object value = new Object();
//
//        MockCodec<Object> codec = MockCodec.builder(Object.class)
//            .textDecoding("test-value", value)
//            .build();
//
//        assertThat(codec.decode(byteBuf, TEXT)).isSameAs(value);
//    }

    @Test
    public void encode() {
        Parameter parameter = new Parameter(TEXT, INT4.getObjectId(), Unpooled.buffer().writeInt(100));
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
