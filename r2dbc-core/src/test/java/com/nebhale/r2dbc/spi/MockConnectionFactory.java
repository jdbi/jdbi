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

import reactor.core.publisher.Mono;

public final class MockConnectionFactory implements ConnectionFactory {

    public static final MockConnectionFactory EMPTY = builder().build();

    private final Mono<Connection> connection;

    private MockConnectionFactory(Mono<Connection> connection) {
        this.connection = connection;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Mono<Connection> create() {
        if (this.connection == null) {
            throw new AssertionError("Unexpected call to create()");
        }

        return this.connection;
    }

    @Override
    public String toString() {
        return "MockConnectionFactory{" +
            "connection=" + this.connection +
            '}';
    }

    public static final class Builder {

        private Connection connection;

        private Builder() {
        }

        public MockConnectionFactory build() {
            return new MockConnectionFactory(this.connection == null ? null : Mono.just(this.connection));
        }

        public Builder connection(Connection connection) {
            this.connection = connection;
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "connection=" + this.connection +
                '}';
        }

    }

}
