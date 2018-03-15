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

package com.nebhale.r2dbc.spi;

import com.nebhale.r2dbc.core.nullability.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class MockColumnMetadata implements ColumnMetadata {

    public static final String EMPTY_NAME = "empty-name";

    public static final Integer EMPTY_TYPE = Integer.MAX_VALUE;

    private final String name;

    private final Integer precision;

    private final Integer type;

    private MockColumnMetadata(String name, @Nullable Integer precision, Integer type) {
        this.name = Objects.requireNonNull(name);
        this.precision = precision;
        this.type = Objects.requireNonNull(type);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MockColumnMetadata empty() {
        return builder()
            .name(EMPTY_NAME)
            .type(EMPTY_TYPE)
            .build();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Optional<Integer> getPrecision() {
        return Optional.ofNullable(this.precision);
    }

    @Override
    public Integer getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "MockColumnMetadata{" +
            "name='" + this.name + '\'' +
            ", precision=" + this.precision +
            ", type=" + this.type +
            '}';
    }

    public static final class Builder {

        private String name;

        private Integer precision;

        private Integer type;

        private Builder() {
        }

        public MockColumnMetadata build() {
            return new MockColumnMetadata(this.name, this.precision, this.type);
        }

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        public Builder precision(Integer precision) {
            this.precision = Objects.requireNonNull(precision);
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "name='" + this.name + '\'' +
                ", precision=" + this.precision +
                ", type=" + this.type +
                '}';
        }

        public Builder type(Integer type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

    }

}
