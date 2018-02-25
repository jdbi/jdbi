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
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.FLOAT8;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.VARCHAR;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class DoubleCodecTest {

    @Test
    public void constructorNoByteBufAllocator() {
        assertThatNullPointerException().isThrownBy(() -> new DoubleCodec(null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    public void decode() {
        DoubleCodec codec = new DoubleCodec(TEST);

        assertThat(codec.decode(TEST.buffer(8).writeDouble(100.0d), BINARY, Double.class)).isEqualTo(100.0d);
        assertThat(codec.decode(ByteBufUtils.encode(TEST, "100.0"), TEXT, Double.class)).isEqualTo(100.0d);
    }

    @Test
    public void decodeNoByteBuf() {
        assertThatNullPointerException().isThrownBy(() -> new DoubleCodec(TEST).decode(null, BINARY, Double.class))
            .withMessage("byteBuf must not be null");
    }

    @Test
    public void decodeNoFormat() {
        assertThatNullPointerException().isThrownBy(() -> new DoubleCodec(TEST).decode(TEST.buffer(0), null, Double.class))
            .withMessage("format must not be null");
    }

    @Test
    public void doCanDecode() {
        DoubleCodec codec = new DoubleCodec(TEST);

        assertThat(codec.doCanDecode(BINARY, VARCHAR)).isFalse();
        assertThat(codec.doCanDecode(BINARY, FLOAT8)).isTrue();
        assertThat(codec.doCanDecode(TEXT, FLOAT8)).isTrue();
    }

    @Test
    public void doCanDecodeNoType() {
        assertThatNullPointerException().isThrownBy(() -> new DoubleCodec(TEST).doCanDecode(null, null))
            .withMessage("type must not be null");
    }

    @Test
    public void doEncode() {
        assertThat(new DoubleCodec(TEST).doEncode(100d))
            .isEqualTo(new Parameter(BINARY, FLOAT8.getObjectId(), TEST.buffer(8).writeDouble(100)));
    }

    @Test
    public void doEncodeNoValue() {
        assertThatNullPointerException().isThrownBy(() -> new DoubleCodec(TEST).doEncode(null))
            .withMessage("value must not be null");
    }

}
