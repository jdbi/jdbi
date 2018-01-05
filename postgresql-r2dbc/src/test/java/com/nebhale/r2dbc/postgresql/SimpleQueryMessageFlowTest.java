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

import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.frontend.Query;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;

public final class SimpleQueryMessageFlowTest {

    @Test
    public void exchange() {
        ClientAssert.assertThat(client -> SimpleQueryMessageFlow
            .exchange(client, "test-query"))
            .receivingServerMessages(new CommandComplete("test", null, null))
            .makesRequests(requests -> requests
                .expectNext(new Query("test-query"))
                .verifyComplete())
            .andEmits(emissions -> emissions
                .expectNext(new CommandComplete("test", null, null))
                .verifyComplete())
            .verify();
    }

    @Test
    public void exchangeNoClient() {
        assertThatNullPointerException().isThrownBy(() -> SimpleQueryMessageFlow.exchange(null, "test-query"))
            .withMessage("client must not be null");
    }

    @Test
    public void exchangeNoQuery() {
        assertThatNullPointerException().isThrownBy(() -> SimpleQueryMessageFlow.exchange(mock(Client.class), null))
            .withMessage("query must not be null");
    }

}
