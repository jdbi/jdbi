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

package com.nebhale.r2dbc.postgresql.client;

import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.UNSPECIFIED;
import static com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class BindingTest {

    @Test
    public void addNoIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> new Binding().add(null, new Parameter(TEXT, 100, null)))
            .withMessage("identifier must not be null");
    }

    @Test
    public void addNoParameter() {
        assertThatNullPointerException().isThrownBy(() -> new Binding().add(1, null))
            .withMessage("parameter must not be null");
    }

    @Test
    public void empty() {
        Binding binding = new Binding();

        assertThat(binding.getParameterFormats()).isEmpty();
    }

    @Test
    public void getParameterFormats() {
        Binding binding = new Binding();
        binding.add(0, new Parameter(BINARY, 100, TEST.buffer(4).writeInt(200)));
        binding.add(2, new Parameter(BINARY, 100, TEST.buffer(4).writeInt(300)));

        assertThat(binding.getParameterFormats()).containsExactly(BINARY, TEXT, BINARY);
    }

    @Test
    public void getParameterTypes() {
        Binding binding = new Binding();
        binding.add(0, new Parameter(BINARY, 100, TEST.buffer(4).writeInt(200)));
        binding.add(2, new Parameter(BINARY, 100, TEST.buffer(4).writeInt(300)));

        assertThat(binding.getParameterTypes()).containsExactly(100, UNSPECIFIED.getObjectId(), 100);
    }

    @Test
    public void getParameterValues() {
        Binding binding = new Binding();
        binding.add(0, new Parameter(BINARY, 100, TEST.buffer(4).writeInt(200)));
        binding.add(2, new Parameter(BINARY, 100, TEST.buffer(4).writeInt(300)));

        assertThat(binding.getParameterValues()).containsExactly(TEST.buffer(4).writeInt(200), null, TEST.buffer(4).writeInt(300));
    }

}
