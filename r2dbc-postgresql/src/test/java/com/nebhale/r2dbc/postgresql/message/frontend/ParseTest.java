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

import java.util.Collections;

import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageAssert.assertThat;
import static com.nebhale.r2dbc.postgresql.message.frontend.Parse.UNSPECIFIED;
import static io.netty.util.CharsetUtil.UTF_8;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class ParseTest {

    @Test
    public void constructorNoName() {
        assertThatNullPointerException().isThrownBy(() -> new Parse(null, Collections.emptyList(), "test-query"))
            .withMessage("name must not be null");
    }

    @Test
    public void constructorNoParameters() {
        assertThatNullPointerException().isThrownBy(() -> new Parse("test-name", null, "test-query"))
            .withMessage("parameters must not be null");
    }

    @Test
    public void constructorNoQuery() {
        assertThatNullPointerException().isThrownBy(() -> new Parse("test-name", Collections.emptyList(), null))
            .withMessage("query must not be null");
    }

    @Test
    public void encode() {
        assertThat(new Parse("test-name", Collections.singletonList(UNSPECIFIED), "test-query")).encoded()
            .isDeferred()
            .isEncodedAs(buffer -> {
                buffer
                    .writeByte('P')
                    .writeInt(31);

                buffer.writeCharSequence("test-name", UTF_8);
                buffer.writeByte(0);

                buffer.writeCharSequence("test-query", UTF_8);
                buffer.writeByte(0);

                buffer
                    .writeShort(1)
                    .writeInt(0);

                return buffer;
            });
    }

}
