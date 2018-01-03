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

import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageAssert.assertThat;
import static io.netty.util.CharsetUtil.UTF_8;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class StartupMessageTest {

    @Test
    public void constructorNoApplicationName() {
        assertThatNullPointerException().isThrownBy(() -> new StartupMessage(null, "test-database", "test-username"))
            .withMessage("applicationName must not be null");
    }

    @Test
    public void constructorNoUsername() {
        assertThatNullPointerException().isThrownBy(() -> new StartupMessage("test-application-name", "test-database", null))
            .withMessage("username must not be null");
    }

    @Test
    public void encode() {
        assertThat(new StartupMessage("test-application-name", "test-database", "test-username")).encoded()
            .isDeferred()
            .isEncodedAs(buffer -> {
                buffer
                    .writeInt(146)
                    .writeInt(196608);

                buffer.writeCharSequence("user", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("test-username", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("database", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("test-database", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("application_name", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("test-application-name", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("client_encoding", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("utf8", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("DateStyle", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("ISO", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("extra_float_digits", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("2", UTF_8);
                buffer.writeByte(0);

                buffer.writeByte(0);

                return buffer;
            });
    }

    @Test
    public void encodeNoDatabase() {
        assertThat(new StartupMessage("test-application-name", null, "test-username")).encoded()
            .isDeferred()
            .isEncodedAs(buffer -> {
                buffer
                    .writeInt(123)
                    .writeInt(196608);

                buffer.writeCharSequence("user", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("test-username", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("application_name", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("test-application-name", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("client_encoding", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("utf8", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("DateStyle", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("ISO", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("extra_float_digits", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("2", UTF_8);
                buffer.writeByte(0);

                buffer.writeByte(0);

                return buffer;
            });
    }

}
