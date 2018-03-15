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
import com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MockCodec<T> extends AbstractCodec<T> {

    private final Set<CanDecode> canDecodes;

    private final Map<Decoding, T> decodings;

    private final Map<T, Parameter> encodings;

    private MockCodec(Set<CanDecode> canDecodes, Map<Decoding, T> decodings, Map<T, Parameter> encodings, Class<T> type) {
        super(type);

        this.canDecodes = Objects.requireNonNull(canDecodes);
        this.decodings = Objects.requireNonNull(decodings);
        this.encodings = Objects.requireNonNull(encodings);
    }

    public static <T> Builder<T> builder(Class<T> type) {
        return new Builder<>(type);
    }

    public static <T> MockCodec<T> empty(Class<T> type) {
        return builder(type).build();
    }

    @Override
    public T decode(ByteBuf byteBuf, Format format, Class<? extends T> type) {
        Objects.requireNonNull(byteBuf);
        Objects.requireNonNull(format);
        Objects.requireNonNull(type);

        Decoding decoding = new Decoding(byteBuf, format);

        if (!this.decodings.containsKey(decoding)) {
            throw new AssertionError(String.format("Unexpected call to decode(ByteBuf, Format, Class) with values '%s', '%s', '%s'", byteBuf, format, type.getName()));
        }

        return this.decodings.get(decoding);

    }

    @Override
    public String toString() {
        return "StubCodec{" +
            "decodings=" + this.decodings +
            ", encodings=" + this.encodings +
            "} " + super.toString();
    }

    @Override
    boolean doCanDecode(Format format, PostgresqlObjectId type) {
        Objects.requireNonNull(format);
        Objects.requireNonNull(type);

        return this.canDecodes.contains(new CanDecode(format, type));
    }

    @Override
    Parameter doEncode(T value) {
        if (!this.encodings.containsKey(value)) {
            throw new AssertionError(String.format("Unexpected call to doEncode(T) with value '%s'", value));
        }

        return this.encodings.get(value);
    }

    public static final class Builder<T> {

        private final Set<CanDecode> canDecodes = new HashSet<>();

        private final Map<Decoding, T> decodings = new HashMap<>();

        private final Map<T, Parameter> encodings = new HashMap<>();

        private final Class<T> type;

        private Builder(Class<T> type) {
            this.type = type;
        }

        public MockCodec<T> build() {
            return new MockCodec<>(this.canDecodes, this.decodings, this.encodings, this.type);
        }

        public Builder<T> canDecode(Format format, PostgresqlObjectId type) {
            Objects.requireNonNull(format);
            Objects.requireNonNull(type);

            this.canDecodes.add(new CanDecode(format, type));
            return this;
        }

        public Builder<T> decoding(ByteBuf byteBuf, Format format, T value) {
            Objects.requireNonNull(byteBuf);
            Objects.requireNonNull(format);
            Objects.requireNonNull(value);

            this.decodings.put(new Decoding(byteBuf, format), value);
            return this;
        }

        public Builder<T> encoding(T value, Parameter parameter) {
            Objects.requireNonNull(value);
            Objects.requireNonNull(parameter);

            this.encodings.put(value, parameter);
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "decodings=" + this.decodings +
                ", encodings=" + this.encodings +
                ", type=" + this.type +
                '}';
        }
    }

    private static final class CanDecode {

        private final Format format;

        private final PostgresqlObjectId type;

        private CanDecode(Format format, PostgresqlObjectId type) {
            this.format = Objects.requireNonNull(format);
            this.type = Objects.requireNonNull(type);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CanDecode that = (CanDecode) o;
            return this.format == that.format &&
                this.type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.format, this.type);
        }

        @Override
        public String toString() {
            return "CanDecode{" +
                "format=" + this.format +
                ", type=" + this.type +
                '}';
        }

    }

    private static final class Decoding {

        private final ByteBuf byteBuf;

        private final Format format;

        private Decoding(ByteBuf byteBuf, Format format) {
            this.byteBuf = Objects.requireNonNull(byteBuf);
            this.format = Objects.requireNonNull(format);
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
            return Objects.equals(this.byteBuf, that.byteBuf) &&
                this.format == that.format;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.byteBuf, this.format);
        }

        @Override
        public String toString() {
            return "Decoding{" +
                "byteBuf=" + this.byteBuf +
                ", format=" + this.format +
                '}';
        }

    }

}
