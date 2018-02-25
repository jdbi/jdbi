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
import com.nebhale.r2dbc.postgresql.message.Format;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class MockCodecs implements Codecs {

    private final Map<Decoding, Object> decodings;

    private final Map<Object, Parameter> encodings;

    private MockCodecs(Map<Decoding, Object> decodings, Map<Object, Parameter> encodings) {
        this.decodings = requireNonNull(decodings);
        this.encodings = requireNonNull(encodings);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MockCodecs empty() {
        return builder().build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T decode(ByteBuf byteBuf, int dataType, Format format, Class<? extends T> type) {
        Decoding decoding = new Decoding(byteBuf, dataType, requireNonNull(format), requireNonNull(type));

        if (!this.decodings.containsKey(decoding)) {
            throw new AssertionError(String.format("Unexpected call to decode(ByteBuf,int,Format,Class<?>) with values '%s, %d, %s, %s'", byteBuf, dataType, format, type.getName()));
        }

        return (T) this.decodings.get(decoding);
    }

    @Override
    public Parameter encode(Object value) {
        if (!this.encodings.containsKey(value)) {
            throw new AssertionError(String.format("Unexpected call to encode(Object) with value '%s'", value));
        }

        return this.encodings.get(value);
    }

    @Override
    public String toString() {
        return "MockCodecs{" +
            "decodings=" + this.decodings +
            ", encodings=" + this.encodings +
            '}';
    }

    public static final class Builder {

        private final Map<Decoding, Object> decodings = new HashMap<>();

        private final Map<Object, Parameter> encodings = new HashMap<>();

        private Builder() {
        }

        public MockCodecs build() {
            return new MockCodecs(this.decodings, this.encodings);
        }

        public <T> Builder decoding(ByteBuf byteBuf, int dataType, Format format, Class<T> type, T value) {
            this.decodings.put(new Decoding(byteBuf, dataType, requireNonNull(format), requireNonNull(type)), value);
            return this;
        }

        public Builder encoding(Object value, Parameter parameter) {
            this.encodings.put(value, requireNonNull(parameter));
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "decodings=" + this.decodings +
                ", encodings=" + this.encodings +
                '}';
        }

    }

    private static final class Decoding {

        private final ByteBuf byteBuf;

        private final int dataType;

        private final Format format;

        private final Class<?> type;

        private Decoding(ByteBuf byteBuf, int dataType, Format format, Class<?> type) {
            this.byteBuf = byteBuf;
            this.dataType = dataType;
            this.format = requireNonNull(format);
            this.type = requireNonNull(type);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Decoding that = (Decoding) o;
            return this.dataType == that.dataType &&
                Objects.equals(this.byteBuf, that.byteBuf) &&
                this.format == that.format &&
                Objects.equals(this.type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.byteBuf, this.dataType, this.format, this.type);
        }

        @Override
        public String toString() {
            return "Decoding{" +
                "byteBuf=" + this.byteBuf +
                ", dataType=" + this.dataType +
                ", format=" + this.format +
                ", type=" + this.type +
                '}';
        }

    }

}
