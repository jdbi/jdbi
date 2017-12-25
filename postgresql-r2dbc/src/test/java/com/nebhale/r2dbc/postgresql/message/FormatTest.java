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

package com.nebhale.r2dbc.postgresql.message;

import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public final class FormatTest {

    @Test
    public void getDiscriminatorBinary() {
        assertThat(BINARY.getDiscriminator()).isEqualTo((byte) 1);
    }

    @Test
    public void getDiscriminatorText() {
        assertThat(TEXT.getDiscriminator()).isEqualTo((byte) 0);
    }

    @Test
    public void valueOfBinary() {
        assertThat(Format.valueOf((short) 1)).isEqualTo(BINARY);
    }

    @Test
    public void valueOfInvalid() {
        assertThatIllegalArgumentException().isThrownBy(() -> Format.valueOf((short) -1))
            .withMessage("-1 is not a valid format");
    }

    @Test
    public void valueOfText() {
        assertThat(Format.valueOf((short) 0)).isEqualTo(TEXT);
    }

}
