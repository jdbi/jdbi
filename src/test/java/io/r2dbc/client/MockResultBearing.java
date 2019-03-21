/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.client;

import io.r2dbc.client.util.Assert;
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public final class MockResultBearing implements ResultBearing {

    private final Result result;

    private MockResultBearing(Result result) {
        this.result = Assert.requireNonNull(result, "result must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> Flux<T> mapResult(Function<Result, ? extends Publisher<? extends T>> f) {
        Assert.requireNonNull(f, "f must not be null");

        return Flux.from(f.apply(this.result));
    }

    @Override
    public String toString() {
        return "MockResultBearing{" +
            "result=" + this.result +
            '}';
    }

    public static final class Builder {

        private Result result;

        private Builder() {
        }

        public MockResultBearing build() {
            return new MockResultBearing(this.result);
        }

        public Builder result(Result result) {
            this.result = Assert.requireNonNull(result, "result must not be null");
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "result=" + this.result +
                '}';
        }

    }

}
