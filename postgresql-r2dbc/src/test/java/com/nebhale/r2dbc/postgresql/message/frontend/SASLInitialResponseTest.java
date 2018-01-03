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

import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageAssert.assertThat;
import static io.netty.util.CharsetUtil.UTF_8;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class SASLInitialResponseTest {

    @Test
    public void constructorNoName() {
        assertThatNullPointerException().isThrownBy(() -> new SASLInitialResponse(null, null))
            .withMessage("name must not be null");
    }

    @Test
    public void encode() {
        assertThat(new SASLInitialResponse(Unpooled.buffer().writeInt(100), "test-name")).encoded()
            .isDeferred()
            .isEncodedAs(buffer -> {
                buffer
                    .writeByte('p')
                    .writeInt(22);

                buffer.writeCharSequence("test-name", UTF_8);
                buffer.writeByte(0);

                buffer
                    .writeInt(4)
                    .writeInt(100);

                return buffer;
            });
    }

    @Test
    public void encodeNoInitialResponse() {
        assertThat(new SASLInitialResponse(null, "test-name")).encoded()
            .isDeferred()
            .isEncodedAs(buffer -> {
                buffer
                    .writeByte('p')
                    .writeInt(18);

                buffer.writeCharSequence("test-name", UTF_8);
                buffer.writeByte(0);

                buffer
                    .writeInt(-1);

                return buffer;
            });
    }

}
