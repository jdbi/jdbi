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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageUtils.readCStringUTF8;

/**
 * The AuthenticationSASL message.
 */
public final class AuthenticationSASL implements AuthenticationMessage {

    private static final byte TERMINAL = 0;

    private final List<String> authenticationMechanisms;

    /**
     * Creates a new message.
     *
     * @param authenticationMechanisms the names of SASL authentication mechanisms
     * @throws NullPointerException if {@code authenticationMechanisms} is {@code null}
     */
    public AuthenticationSASL(List<String> authenticationMechanisms) {
        this.authenticationMechanisms = Objects.requireNonNull(authenticationMechanisms, "authenticationMechanisms must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthenticationSASL that = (AuthenticationSASL) o;
        return Objects.equals(this.authenticationMechanisms, that.authenticationMechanisms);
    }

    /**
     * Returns the names of SASL authentication mechanisms.
     *
     * @return the names of SASL authentication mechanisms
     */
    public List<String> getAuthenticationMechanisms() {
        return this.authenticationMechanisms;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.authenticationMechanisms);
    }

    @Override
    public String toString() {
        return "AuthenticationSASL{" +
            "authenticationMechanisms=" + this.authenticationMechanisms +
            '}';
    }

    static AuthenticationSASL decode(ByteBuf in) {
        Objects.requireNonNull(in, "in must not be null");

        List<String> authenticationMechanisms = new ArrayList<>();

        while (TERMINAL != in.getByte(in.readerIndex())) {
            authenticationMechanisms.add(readCStringUTF8(in));
        }

        return new AuthenticationSASL(authenticationMechanisms);
    }

}
