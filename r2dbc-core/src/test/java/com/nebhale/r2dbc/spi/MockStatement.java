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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MockStatement implements Statement {

    private final List<Map<Object, Object>> bindings = new ArrayList<>();

    private final Flux<Result> results;

    private boolean addCalled = false;

    private Map<Object, Object> current;

    private MockStatement(Flux<Result> results) {
        this.results = Objects.requireNonNull(results);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MockStatement empty() {
        return builder().build();
    }

    @Override
    public MockStatement add() {
        this.addCalled = true;
        this.current = null;
        return this;
    }

    @Override
    public MockStatement bind(Object identifier, Object value) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(value);

        getCurrent().put(identifier, value);
        return this;
    }

    @Override
    public MockStatement bind(Integer index, Object value) {
        Objects.requireNonNull(index);
        Objects.requireNonNull(value);

        getCurrent().put(index, value);
        return this;
    }

    @Override
    public MockStatement bindNull(Object identifier, Object type) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(type);

        getCurrent().put(identifier, type);
        return this;
    }

    @Override
    public Flux<Result> execute() {
        return this.results;
    }

    @Override
    public Publisher<? extends Result> executeReturningGeneratedKeys() {
        return execute();
    }

    public List<Map<Object, Object>> getBindings() {
        return this.bindings;
    }

    public boolean isAddCalled() {
        return this.addCalled;
    }

    @Override
    public String toString() {
        return "MockStatement{" +
            "bindings=" + this.bindings +
            ", results=" + this.results +
            ", current=" + this.current +
            '}';
    }

    private Map<Object, Object> getCurrent() {
        if (this.current == null) {
            this.current = new HashMap<>();
            this.bindings.add(this.current);
        }

        return this.current;
    }

    public static final class Builder {

        private final List<Result> results = new ArrayList<>();

        private Builder() {
        }

        public MockStatement build() {
            return new MockStatement(Flux.fromIterable(this.results));
        }

        public Builder result(Result result) {
            Objects.requireNonNull(result);

            this.results.add(result);
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "results=" + this.results +
                '}';
        }

    }

}
