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
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.INT4;
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
    public void encode() {
        Parameter parameter = new DefaultCodecs(UnpooledByteBufAllocator.DEFAULT).encode(100);

        assertThat(parameter).isEqualTo(new Parameter(BINARY, INT4.getObjectId(), Unpooled.buffer(4).writeInt(100)));
    }

    @Test
    public void encodeUnsupportedType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DefaultCodecs(UnpooledByteBufAllocator.DEFAULT).encode(new Object()))
            .withMessage("Unknown parameter of type java.lang.Object");
    }

}
