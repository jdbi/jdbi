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

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class MockCodecs implements Codecs {

    public static final MockCodecs EMPTY = builder().build();

    private final Map<Object, Parameter> encodings;

    private MockCodecs(Map<Object, Parameter> encodings) {
        this.encodings = requireNonNull(encodings);
    }

    public static Builder builder() {
        return new Builder();
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
            "encodings=" + this.encodings +
            '}';
    }

    public static final class Builder {

        private final Map<Object, Parameter> encodings = new HashMap<>();

        private Builder() {
        }

        public MockCodecs build() {
            return new MockCodecs(this.encodings);
        }

        public Builder encoding(Object value, Parameter parameter) {
            this.encodings.put(value, requireNonNull(parameter));
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "encodings=" + this.encodings +
                '}';
        }

    }

}
