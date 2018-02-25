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
 * The AuthenticationGSSContinue message.
 */
public final class AuthenticationGSSContinue implements AuthenticationMessage {

    private final ByteBuffer authenticationData;

    /**
     * Creates a new message.
     *
     * @param authenticationData GSSAPI or SSPI authentication data
     * @throws NullPointerException if {@code authenticationData} is {@code null}
     */
    public AuthenticationGSSContinue(ByteBuf authenticationData) {
        requireNonNull(authenticationData, "authenticationData must not be null");

        this.authenticationData = authenticationData.nioBuffer();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthenticationGSSContinue that = (AuthenticationGSSContinue) o;
        return Objects.equals(this.authenticationData, that.authenticationData);
    }

    /**
     * Returns GSSAPI or SSPI authentication data.
     *
     * @return GSSAPI or SSPI authentication data
     */
    public ByteBuffer getAuthenticationData() {
        return this.authenticationData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.authenticationData);
    }

    @Override
    public String toString() {
        return "AuthenticationGSSContinue{" +
            "authenticationData=" + this.authenticationData +
            '}';
    }

    static AuthenticationGSSContinue decode(ByteBuf in) {
        requireNonNull(in, "in must not be null");

        return new AuthenticationGSSContinue(in.readSlice(in.readableBytes()));
    }

}
