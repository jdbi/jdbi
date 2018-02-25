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

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * The AuthenticationMD5Password message.
 */
public final class AuthenticationMD5Password implements AuthenticationMessage {

    private final ByteBuffer salt;

    /**
     * Creates a new message.
     *
     * @param salt the salt to use when encrypting the password
     * @throws NullPointerException if {@code salt} is {@code null}
     */
    public AuthenticationMD5Password(ByteBuf salt) {
        requireNonNull(salt, "salt must not be null");

        this.salt = salt.nioBuffer();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthenticationMD5Password that = (AuthenticationMD5Password) o;
        return Objects.equals(this.salt, that.salt);
    }

    /**
     * Returns the salt to use when encrypting the password.
     *
     * @return the salt to use when encrypting the password
     */
    public ByteBuffer getSalt() {
        return this.salt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.salt);
    }

    @Override
    public String toString() {
        return "AuthenticationMD5Password{" +
            "salt=" + this.salt +
            '}';
    }

    static AuthenticationMD5Password decode(ByteBuf in) {
        requireNonNull(in, "in must not be null");

        return new AuthenticationMD5Password(in.readSlice(4));
    }

}
