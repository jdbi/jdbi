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
import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.INT2;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.VARCHAR;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class ByteCodecTest {

    @Test
    public void constructorNoByteBufAllocator() {
        assertThatNullPointerException().isThrownBy(() -> new ByteCodec(null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    public void decode() {
        ByteCodec codec = new ByteCodec(TEST);

        assertThat(codec.decode(TEST.buffer(2).writeShort((byte) 100), BINARY, Byte.class)).isEqualTo((byte) 100);
        assertThat(codec.decode(ByteBufUtils.encode(TEST, "100"), TEXT, Byte.class)).isEqualTo((byte) 100);
    }

    @Test
    public void decodeNoByteBuf() {
        assertThatNullPointerException().isThrownBy(() -> new ByteCodec(TEST).decode(null, BINARY, Byte.class))
            .withMessage("byteBuf must not be null");
    }

    @Test
    public void decodeNoFormat() {
        assertThatNullPointerException().isThrownBy(() -> new ByteCodec(TEST).decode(TEST.buffer(0), null, Byte.class))
            .withMessage("format must not be null");
    }

    @Test
    public void doCanDecode() {
        ByteCodec codec = new ByteCodec(TEST);

        assertThat(codec.doCanDecode(BINARY, VARCHAR)).isFalse();
        assertThat(codec.doCanDecode(BINARY, INT2)).isTrue();
        assertThat(codec.doCanDecode(TEXT, INT2)).isTrue();
    }

    @Test
    public void doCanDecodeNoType() {
        assertThatNullPointerException().isThrownBy(() -> new ByteCodec(TEST).doCanDecode(null, null))
            .withMessage("type must not be null");
    }

    @Test
    public void doEncode() {
        assertThat(new ByteCodec(TEST).doEncode((byte) 100))
            .isEqualTo(new Parameter(BINARY, INT2.getObjectId(), TEST.buffer(2).writeShort(100)));
    }

    @Test
    public void doEncodeNoValue() {
        assertThatNullPointerException().isThrownBy(() -> new ByteCodec(TEST).doEncode(null))
            .withMessage("value must not be null");
    }

}
