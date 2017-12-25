/*
 * Copyright 2017-2017 the original author or authors.
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

package com.nebhale.r2dbc.postgresql.message;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public final class AuthenticationGSSContinue implements BackendMessage {

    private final ByteBuf authenticationData;

    public AuthenticationGSSContinue(ByteBuf authenticationData) {
        this.authenticationData = Objects.requireNonNull(authenticationData, "authenticationData must not be null");
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

    public ByteBuf getAuthenticationData() {
        return this.authenticationData;
    }

    @Override
    public int hashCode() {

        return Objects.hash(authenticationData);
    }

    @Override
    public String toString() {
        return "AuthenticationGSSContinue{" +
            "authenticationData=" + this.authenticationData +
            '}';
    }

    static AuthenticationGSSContinue decode(ByteBuf in) {
        return new AuthenticationGSSContinue(in.readRetainedSlice(in.readableBytes()));
    }

}
