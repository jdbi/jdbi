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

import com.nebhale.r2dbc.postgresql.authentication.AuthenticationHandler;
import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationMD5Password;
import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationOk;
import com.nebhale.r2dbc.postgresql.message.backend.CommandComplete;
import com.nebhale.r2dbc.postgresql.message.frontend.PasswordMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.StartupMessage;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import reactor.test.StepVerifier;

import static com.nebhale.r2dbc.postgresql.ClientAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class StartupMessageFlowTest {

    private final AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class, RETURNS_SMART_NULLS);

    @Test
    public void exchangeAuthenticationMessage() {
        when(this.authenticationHandler.handle(new AuthenticationMD5Password(Unpooled.buffer().writeInt(100)))).thenReturn(new PasswordMessage("test-password"));

        assertThat(client -> StartupMessageFlow
            .exchange("test-application-name", this.authenticationHandler, client, "test-database", "test-username"))
            .receivingServerMessages(new AuthenticationMD5Password(Unpooled.buffer().writeInt(100)), AuthenticationOk.INSTANCE)
            .makesRequests(requests -> requests
                .expectNext(new StartupMessage("test-application-name", "test-database", "test-username"))
                .expectNext(new PasswordMessage("test-password"))
                .verifyComplete())
            .andEmits(StepVerifier.LastStep::verifyComplete)
            .verify();
    }

    @Test
    public void exchangeAuthenticationMessageFail() {
        when(this.authenticationHandler.handle(new AuthenticationMD5Password(Unpooled.buffer().writeInt(100)))).thenThrow(new IllegalArgumentException());

        assertThat(client -> StartupMessageFlow
            .exchange("test-application-name", this.authenticationHandler, client, "test-database", "test-username"))
            .receivingServerMessages(new AuthenticationMD5Password(Unpooled.buffer().writeInt(100)), AuthenticationOk.INSTANCE)
            .makesRequests(requests -> requests
                .expectNext(new StartupMessage("test-application-name", "test-database", "test-username"))
                .verifyError(IllegalArgumentException.class))
            .andEmits(emissions -> emissions
                .verifyError(IllegalArgumentException.class))
            .verify();
    }

    @Test
    public void exchangeAuthenticationOk() {
        assertThat(client -> StartupMessageFlow
            .exchange("test-application-name", this.authenticationHandler, client, "test-database", "test-username"))
            .receivingServerMessages(AuthenticationOk.INSTANCE)
            .makesRequests(requests -> requests
                .expectNext(new StartupMessage("test-application-name", "test-database", "test-username"))
                .verifyComplete())
            .andEmits(StepVerifier.LastStep::verifyComplete)
            .verify();
    }

    @Test
    public void exchangeAuthenticationOther() {
        assertThat(client -> StartupMessageFlow
            .exchange("test-application-name", this.authenticationHandler, client, "test-database", "test-username"))
            .receivingServerMessages(new CommandComplete("test", null, null), AuthenticationOk.INSTANCE)
            .makesRequests(requests -> requests
                .expectNext(new StartupMessage("test-application-name", "test-database", "test-username"))
                .verifyComplete())
            .andEmits(emissions -> emissions
                .expectNext(new CommandComplete("test", null, null))
                .verifyComplete())
            .verify();
    }

    @Test
    public void exchangeNoApplicationName() {
        assertThatNullPointerException().isThrownBy(() -> StartupMessageFlow.exchange(null, this.authenticationHandler, mock(Client.class), "test-database", "test-username"))
            .withMessage("applicationName must not be null");
    }

    @Test
    public void exchangeNoAuthenticationHandler() {
        assertThatNullPointerException().isThrownBy(() -> StartupMessageFlow.exchange("test-application-name", null, mock(Client.class), "test-database", "test-username"))
            .withMessage("authenticationHandler must not be null");
    }

    @Test
    public void exchangeNoClient() {
        assertThatNullPointerException().isThrownBy(() -> StartupMessageFlow.exchange("test-application-name", this.authenticationHandler, null, "test-database", "test-username"))
            .withMessage("client must not be null");
    }

    @Test
    public void exchangeNoUsername() {
        assertThatNullPointerException().isThrownBy(() -> StartupMessageFlow.exchange("test-application-name", this.authenticationHandler, mock(Client.class), "test-database", null))
            .withMessage("username must not be null");
    }

}
