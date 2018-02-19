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

import java.time.LocalDateTime;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.MONEY;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.TIMESTAMP;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.VARCHAR;
import static com.nebhale.r2dbc.postgresql.util.ByteBufUtils.encode;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class LocalDateTimeCodecTest {

    @Test
    public void constructorNoByteBufAllocator() {
        assertThatNullPointerException().isThrownBy(() -> new LocalDateTimeCodec(null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    public void decode() {
        LocalDateTime localDateTime = LocalDateTime.now();

        assertThat(new LocalDateTimeCodec(TEST).decode(encode(TEST, localDateTime.toString()), TEXT, LocalDateTime.class))
            .isEqualTo(localDateTime);
    }

    @Test
    public void decodeNoByteBuf() {
        assertThatNullPointerException().isThrownBy(() -> new LocalDateTimeCodec(TEST).decode(null, TEXT, LocalDateTime.class))
            .withMessage("byteBuf must not be null");
    }

    @Test
    public void doCanDecode() {
        LocalDateTimeCodec codec = new LocalDateTimeCodec(TEST);

        assertThat(codec.doCanDecode(BINARY, TIMESTAMP)).isFalse();
        assertThat(codec.doCanDecode(TEXT, MONEY)).isFalse();
        assertThat(codec.doCanDecode(TEXT, TIMESTAMP)).isTrue();
    }

    @Test
    public void doCanDecodeNoFormat() {
        assertThatNullPointerException().isThrownBy(() -> new LocalDateTimeCodec(TEST).doCanDecode(null, VARCHAR))
            .withMessage("format must not be null");
    }

    @Test
    public void doCanDecodeNoType() {
        assertThatNullPointerException().isThrownBy(() -> new LocalDateTimeCodec(TEST).doCanDecode(TEXT, null))
            .withMessage("type must not be null");
    }

    @Test
    public void doEncode() {
        LocalDateTime localDateTime = LocalDateTime.now();

        assertThat(new LocalDateTimeCodec(TEST).doEncode(localDateTime))
            .isEqualTo(new Parameter(TEXT, TIMESTAMP.getObjectId(), encode(TEST, localDateTime.toString())));
    }

    @Test
    public void doEncodeNoValue() {
        assertThatNullPointerException().isThrownBy(() -> new LocalDateTimeCodec(TEST).doEncode(null))
            .withMessage("value must not be null");
    }

}
