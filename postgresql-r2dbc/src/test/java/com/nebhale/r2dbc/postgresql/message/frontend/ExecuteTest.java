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

import static com.nebhale.r2dbc.postgresql.message.frontend.Execute.NO_LIMIT;
import static com.nebhale.r2dbc.postgresql.message.frontend.Execute.UNNAMED_PORTAL;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class ExecuteTest {

    @Test
    public void constructorNoName() {
        assertThatNullPointerException().isThrownBy(() -> new Execute(null, NO_LIMIT))
            .withMessage("name must not be null");
    }

    @Test
    public void encode() {
        assertThat(new Execute(UNNAMED_PORTAL, NO_LIMIT)).encoded()
            .isDeferred()
            .isEncodedAs(buffer -> {
                return buffer
                    .writeByte('E')
                    .writeInt(9)
                    .writeByte(0)
                    .writeInt(0);
            });
    }

}
