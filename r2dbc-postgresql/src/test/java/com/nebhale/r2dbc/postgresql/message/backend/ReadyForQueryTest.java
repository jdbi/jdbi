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

import com.nebhale.r2dbc.postgresql.message.backend.ReadyForQuery.TransactionStatus;
import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageAssert.assertThat;
import static com.nebhale.r2dbc.postgresql.message.backend.ReadyForQuery.TransactionStatus.ERROR;
import static com.nebhale.r2dbc.postgresql.message.backend.ReadyForQuery.TransactionStatus.IDLE;
import static com.nebhale.r2dbc.postgresql.message.backend.ReadyForQuery.TransactionStatus.TRANSACTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class ReadyForQueryTest {

    @Test
    public void constructorNoTransactionStatus() {
        assertThatNullPointerException().isThrownBy(() -> new ReadyForQuery(null))
            .withMessage("transactionStatus must not be null");
    }

    @Test
    public void decode() {
        assertThat(ReadyForQuery.class)
            .decoded(buffer -> buffer.writeByte('I'))
            .isEqualTo(new ReadyForQuery(IDLE));
    }

    @Test
    public void valueOfError() {
        assertThat(TransactionStatus.valueOf((byte) 'E')).isEqualTo(ERROR);
    }

    @Test
    public void valueOfIdle() {
        assertThat(TransactionStatus.valueOf((byte) 'I')).isEqualTo(IDLE);
    }

    @Test
    public void valueOfInvalid() {
        assertThatIllegalArgumentException().isThrownBy(() -> TransactionStatus.valueOf((byte) 'A'))
            .withMessage("A is not a valid transaction status");
    }

    @Test
    public void valueOfTransaction() {
        assertThat(TransactionStatus.valueOf((byte) 'T')).isEqualTo(TRANSACTION);
    }

}
