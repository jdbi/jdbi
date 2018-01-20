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

package com.nebhale.r2dbc.postgresql;

import com.nebhale.r2dbc.postgresql.message.Format;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription.Field;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class PostgresqlRowMetadataTest {

    @Test
    public void constructorNoColumnMetadata() {
        assertThatNullPointerException().isThrownBy(() -> new PostgresqlRowMetadata(null))
            .withMessage("columnMetadata must not be null");
    }

    @Test
    public void toRowMetadata() {
        PostgresqlRowMetadata rowMetadata = PostgresqlRowMetadata.toRowMetadata(
            new RowDescription(Collections.singletonList(new Field((short) -100, -200, -300, (short) -400, Format.TEXT, "test-name", -500))));

        assertThat(rowMetadata.getColumnMetadata()).hasSize(1);
    }

    @Test
    public void toRowMetadataNoRowDescription() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlRowMetadata.toRowMetadata(null))
            .withMessage("rowDescription must not be null");
    }

}