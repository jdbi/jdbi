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
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.INT4;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class DefaultCodecsTest {

    @Test
    public void constructorNoByteBufAllocator() {
        assertThatNullPointerException().isThrownBy(() -> new DefaultCodecs(null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    public void decode() {
        assertThat(new DefaultCodecs(TEST).decode(TEST.buffer(4).writeInt(100), INT4.getObjectId(), BINARY, Integer.class))
            .isEqualTo(100);
    }

    @Test
    public void decodeNoFormat() {
        assertThatNullPointerException().isThrownBy(() -> new DefaultCodecs(TEST).decode(TEST.buffer(4), INT4.getObjectId(), null, Object.class))
            .withMessage("format must not be null");
    }

    @Test
    public void decodeNoType() {
        assertThatNullPointerException().isThrownBy(() -> new DefaultCodecs(TEST).decode(TEST.buffer(4), INT4.getObjectId(), BINARY, null))
            .withMessage("type must not be null");
    }

    @Test
    public void decodeUnsupportedType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DefaultCodecs(TEST).decode(TEST.buffer(4), INT4.getObjectId(), BINARY, Object.class))
            .withMessage("Cannot decode value of type java.lang.Object");
    }

    @Test
    public void encode() {
        Parameter parameter = new DefaultCodecs(TEST).encode(100);

        assertThat(parameter).isEqualTo(new Parameter(BINARY, INT4.getObjectId(), TEST.buffer(4).writeInt(100)));
    }

    @Test
    public void encodeUnsupportedType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DefaultCodecs(TEST).encode(new Object()))
            .withMessage("Cannot encode parameter of type java.lang.Object");
    }

}
