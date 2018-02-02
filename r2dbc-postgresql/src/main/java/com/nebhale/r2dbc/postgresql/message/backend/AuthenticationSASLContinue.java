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

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * The AuthenticationSASLContinue message.
 */
public final class AuthenticationSASLContinue implements AuthenticationMessage {

    private final ByteBuf data;

    /**
     * Creates a new message.
     *
     * @param data SASL data, specific to the SASL mechanism being used
     * @throws NullPointerException if {@code data} is {@code null}
     */
    public AuthenticationSASLContinue(ByteBuf data) {
        this.data = requireNonNull(data, "data must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthenticationSASLContinue that = (AuthenticationSASLContinue) o;
        return Objects.equals(this.data, that.data);
    }

    /**
     * Returns SASL data, specific to the SASL mechanism being used.
     *
     * @return SASL data, specific to the SASL mechanism being used
     */
    public ByteBuf getData() {
        return this.data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data);
    }

    @Override
    public String toString() {
        return "AuthenticationSASLContinue{" +
            "data=" + this.data +
            '}';
    }

    static AuthenticationSASLContinue decode(ByteBuf in) {
        requireNonNull(in, "in must not be null");

        return new AuthenticationSASLContinue(in.readRetainedSlice(in.readableBytes()));
    }

}
