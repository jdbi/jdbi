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

package com.nebhale.r2dbc.postgresql.message.backend;

import org.junit.Test;

import java.util.Collections;

import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class ParameterDescriptionTest {

    @Test
    public void constructorNoParameters() {
        assertThatNullPointerException().isThrownBy(() -> new ParameterDescription(null))
            .withMessage("parameters must not be null");
    }

    @Test
    public void decode() {
        assertThat(ParameterDescription.class)
            .decoded(buffer -> buffer
                .writeShort(1)
                .writeInt(100))
            .isEqualTo(new ParameterDescription(Collections.singletonList(100)));
    }

}
