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
import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.PasswordMessage;

import java.util.Objects;

/**
 * An implementation of {@link AuthenticationHandler} that handles {@link AuthenticationMD5Password} messages.
 */
public final class MD5PasswordAuthenticationHandler implements AuthenticationHandler {

    private final String password;

    private final String username;

    /**
     * Creates a new handler.
     *
     * @param password the password to use for authentication
     * @param username the username to use for authentication
     * @throws NullPointerException if {@code password} or {@code user} is {@code null}
     */
    public MD5PasswordAuthenticationHandler(String password, String username) {
        this.password = Objects.requireNonNull(password, "password must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code message} is {@code null}
     */
    @Override
    public FrontendMessage handle(AuthenticationMessage message) {
        Objects.requireNonNull(message, "message must not be null");

        if (!(message instanceof AuthenticationMD5Password)) {
            throw new IllegalArgumentException(String.format("Cannot handle %s", message.getClass()));
        }

        String shadow = new FluentMessageDigest("md5")
            .update("%s%s", this.password, this.username)
            .digest();

        String transfer = new FluentMessageDigest("md5")
            .update(shadow)
            .update(((AuthenticationMD5Password) message).getSalt())
            .digest();

        return new PasswordMessage(String.format("md5%s", transfer));
    }

}
