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

package com.nebhale.r2dbc.postgresql.codec;

import com.nebhale.r2dbc.postgresql.client.Parameter;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

public final class MockCodec<T> extends AbstractCodec<T> {

    private final Map<ByteBuf, T> binaryDecodings;

    private final Map<T, Parameter> encodings;

    private final Map<String, T> textDecodings;

    private MockCodec(Map<ByteBuf, T> binaryDecodings, Map<T, Parameter> encodings, Map<String, T> textDecodings, Class<T> type) {
        super(type);

        this.binaryDecodings = binaryDecodings;
        this.encodings = encodings;
        this.textDecodings = textDecodings;
    }

    public static <T> Builder<T> builder(Class<T> type) {
        return new Builder<>(type);
    }

    public static <T> MockCodec<T> empty(Class<T> type) {
        return builder(type).build();
    }

//    @Override
//    public T doDecode(ByteBuf byteBuf) {
//        if (!this.binaryDecodings.containsKey(byteBuf)) {
//            throw new AssertionError(String.format("Unexpected call to doDecodeBinary(String) with value '%s'", byteBuf));
//        }
//
//        return this.binaryDecodings.get(byteBuf);
//    }
//
//    @Override
//    public T doDecode(String s) {
//        if (!this.textDecodings.containsKey(s)) {
//            throw new AssertionError(String.format("Unexpected call to doDecodeText(String) with value '%s'", s));
//        }
//
//        return this.textDecodings.get(s);
//    }

    @Override
    public String toString() {
        return "StubCodec{" +
            "binaryDecodings=" + this.binaryDecodings +
            ", encodings=" + this.encodings +
            ", textDecodings=" + this.textDecodings +
            "} " + super.toString();
    }

    @Override
    Parameter doEncode(T value) {
        if (!this.encodings.containsKey(value)) {
            throw new AssertionError(String.format("Unexpected call to doEncode(T) with value '%s'", value));
        }

        return this.encodings.get(value);
    }

    public static final class Builder<T> {

        private final Map<ByteBuf, T> binaryDecodings = new HashMap<>();

        private final Map<T, Parameter> encodings = new HashMap<>();

        private final Map<String, T> textDecodings = new HashMap<>();

        private final Class<T> type;

        private Builder(Class<T> type) {
            this.type = type;
        }

        public Builder<T> binaryDecoding(ByteBuf byteBuf, T value) {
            this.binaryDecodings.put(byteBuf, value);
            return this;
        }

        public MockCodec<T> build() {
            return new MockCodec<>(this.binaryDecodings, this.encodings, this.textDecodings, this.type);
        }

        public Builder<T> encoding(T value, Parameter parameter) {
            this.encodings.put(value, parameter);
            return this;
        }

        public Builder<T> textDecoding(String s, T value) {
            this.textDecodings.put(s, value);
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "binaryDecodings=" + this.binaryDecodings +
                ", encodings=" + this.encodings +
                ", textDecodings=" + this.textDecodings +
                ", type=" + this.type +
                '}';
        }

    }
}
