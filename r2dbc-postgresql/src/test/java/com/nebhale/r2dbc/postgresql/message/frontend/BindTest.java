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

package com.nebhale.r2dbc.postgresql.message.frontend;

import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Collections;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageAssert.assertThat;
import static io.netty.util.CharsetUtil.UTF_8;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class BindTest {

    @Test
    public void constructorNoNames() {
        assertThatNullPointerException().isThrownBy(() -> new Bind(null, Collections.singletonList(BINARY), Collections.singletonList(Unpooled.buffer().writeInt(100)),
            Collections.singletonList(BINARY), "test-source"))
            .withMessage("name must not be null");
    }

    @Test
    public void constructorNoParameterFormats() {
        assertThatNullPointerException().isThrownBy(() -> new Bind("test-name", null, Collections.singletonList(Unpooled.buffer().writeInt(100)), Collections.singletonList(BINARY), "test-source"))
            .withMessage("parameterFormats must not be null");
    }

    @Test
    public void constructorNoParameters() {
        assertThatNullPointerException().isThrownBy(() -> new Bind("test-name", Collections.singletonList(BINARY), null, Collections.singletonList(BINARY), "test-source"))
            .withMessage("parameters must not be null");
    }

    @Test
    public void constructorNoResultFormats() {
        assertThatNullPointerException().isThrownBy(() -> new Bind("test-name", Collections.singletonList(BINARY), Collections.singletonList(Unpooled.buffer().writeInt(100)), null, "test-source"))
            .withMessage("resultFormats must not be null");
    }

    @Test
    public void constructorNoSource() {
        assertThatNullPointerException().isThrownBy(() -> new Bind("test-name", Collections.singletonList(BINARY), Collections.singletonList(Unpooled.buffer().writeInt(100)),
            Collections.singletonList(BINARY), null))
            .withMessage("source must not be null");
    }

    @Test
    public void encode() {
        assertThat(new Bind("test-name", Collections.singletonList(BINARY), Collections.singletonList(Unpooled.buffer().writeInt(100)), Collections.singletonList(BINARY), "test-source")).encoded()
            .isDeferred()
            .isEncodedAs(buffer -> {
                buffer
                    .writeByte('B')
                    .writeInt(44);

                buffer.writeCharSequence("test-name", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("test-source", UTF_8);
                buffer.writeByte(0);

                buffer
                    .writeShort(1)
                    .writeShort(1)
                    .writeShort(1)
                    .writeInt(4)
                    .writeInt(100)
                    .writeShort(1)
                    .writeShort(1);

                return buffer;
            });
    }

    @Test
    public void encodeNullParameter() {
        assertThat(new Bind("test-name", Collections.singletonList(BINARY), Collections.singletonList(null), Collections.singletonList(BINARY), "test-source")).encoded()
            .isDeferred()
            .isEncodedAs(buffer -> {
                buffer
                    .writeByte('B')
                    .writeInt(40);

                buffer.writeCharSequence("test-name", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("test-source", UTF_8);
                buffer.writeByte(0);

                buffer
                    .writeShort(1)
                    .writeShort(1)
                    .writeShort(1)
                    .writeInt(-1)
                    .writeShort(1)
                    .writeShort(1);

                return buffer;
            });

    }

}
