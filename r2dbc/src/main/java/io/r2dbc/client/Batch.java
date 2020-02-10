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

/**
 * A wrapper for a {@link io.r2dbc.spi.Batch} providing additional convenience APIs
 */
public final class Batch implements ResultBearing {

    private final io.r2dbc.spi.Batch batch;

    Batch(io.r2dbc.spi.Batch batch) {
        this.batch = Assert.requireNonNull(batch, "batch must not be null");
    }

    /**
     * Add a statement to this batch.
     *
     * @param sql the statement to add
     * @return this {@link Batch}
     * @throws IllegalArgumentException if {@code sql} is {@code null}
     */
    public Batch add(String sql) {
        Assert.requireNonNull(sql, "sql must not be null");

        this.batch.add(sql);
        return this;
    }

    public <T> Flux<T> mapResult(Function<Result, ? extends Publisher<? extends T>> mappingFunction) {
        Assert.requireNonNull(mappingFunction, "mappingFunction must not be null");

        return Flux.from(this.batch.execute())
            .flatMap(mappingFunction);
    }

    @Override
    public String toString() {
        return "Batch{" +
            "batch=" + this.batch +
            '}';
    }

}
