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

package com.nebhale.r2dbc.postgresql.authentication;

import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationMD5Password;
import com.nebhale.r2dbc.postgresql.message.frontend.PasswordMessage;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class MD5PasswordAuthenticationHandlerTest {

    @Test
    public void constructorNoPassword() {
        assertThatNullPointerException().isThrownBy(() -> new MD5PasswordAuthenticationHandler(null, "test-username"));
    }

    @Test
    public void constructorNoUsername() {
        assertThatNullPointerException().isThrownBy(() -> new MD5PasswordAuthenticationHandler("test-password", null));
    }

    @Test
    public void handle() {
        MD5PasswordAuthenticationHandler handler = new MD5PasswordAuthenticationHandler("test-username", "test-password");
        AuthenticationMD5Password message = new AuthenticationMD5Password(Unpooled.buffer().writeInt(100));

        assertThat(handler.handle(message)).isEqualTo(new PasswordMessage("md52f745aeea541bdd54449b0d78efa048b"));
    }

    @Test
    public void handleNoMessage() {
        assertThatNullPointerException().isThrownBy(() -> {
            MD5PasswordAuthenticationHandler handler = new MD5PasswordAuthenticationHandler("test-username", "test-password");

            handler.handle(null);
        });
    }

}
