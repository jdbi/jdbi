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
 * The FunctionCallResponse message.
 */
public final class FunctionCallResponse implements BackendMessage {

    private static final int NULL = -1;

    private final ByteBuf value;

    /**
     * Creates a new message.
     *
     * @param value the value of the function result, in the format indicated by the associated format code.
     */
    public FunctionCallResponse(ByteBuf value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FunctionCallResponse that = (FunctionCallResponse) o;
        return Objects.equals(this.value, that.value);
    }

    /**
     * Returns the value of the function result, in the format indicated by the associated format code.
     *
     * @return the value of the function result, in the format indicated by the associated format code
     */
    public ByteBuf getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
    }

    @Override
    public String toString() {
        return "FunctionCallResponse{" +
            "value=" + this.value +
            '}';
    }

    static FunctionCallResponse decode(ByteBuf in) {
        requireNonNull(in, "in must not be null");

        int length = in.readInt();
        ByteBuf value = NULL == length ? null : in.readRetainedSlice(length);

        return new FunctionCallResponse(value);
    }

}
