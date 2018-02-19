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
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.INT4;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.VARCHAR;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class IntegerCodecTest {

    @Test
    public void constructorNoByteBufAllocator() {
        assertThatNullPointerException().isThrownBy(() -> new IntegerCodec(null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    public void decode() {
        IntegerCodec codec = new IntegerCodec(TEST);

        assertThat(codec.decode(TEST.buffer(4).writeInt(100), BINARY, Integer.class)).isEqualTo(100);
        assertThat(codec.decode(ByteBufUtils.encode(TEST, "100"), TEXT, Integer.class)).isEqualTo(100);
    }

    @Test
    public void decodeNoByteBuf() {
        assertThatNullPointerException().isThrownBy(() -> new IntegerCodec(TEST).decode(null, BINARY, Integer.class))
            .withMessage("byteBuf must not be null");
    }

    @Test
    public void decodeNoFormat() {
        assertThatNullPointerException().isThrownBy(() -> new IntegerCodec(TEST).decode(TEST.buffer(0), null, Integer.class))
            .withMessage("format must not be null");
    }

    @Test
    public void doCanDecode() {
        IntegerCodec codec = new IntegerCodec(TEST);

        assertThat(codec.doCanDecode(BINARY, VARCHAR)).isFalse();
        assertThat(codec.doCanDecode(BINARY, INT4)).isTrue();
        assertThat(codec.doCanDecode(TEXT, INT4)).isTrue();
    }

    @Test
    public void doCanDecodeNoType() {
        assertThatNullPointerException().isThrownBy(() -> new IntegerCodec(TEST).doCanDecode(null, null))
            .withMessage("type must not be null");
    }

    @Test
    public void doEncode() {
        assertThat(new IntegerCodec(TEST).doEncode(100))
            .isEqualTo(new Parameter(BINARY, INT4.getObjectId(), TEST.buffer(4).writeInt(100)));
    }

    @Test
    public void doEncodeNoValue() {
        assertThatNullPointerException().isThrownBy(() -> new IntegerCodec(TEST).doEncode(null))
            .withMessage("value must not be null");
    }

}
