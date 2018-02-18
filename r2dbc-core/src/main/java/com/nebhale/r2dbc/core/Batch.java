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

package com.nebhale.r2dbc.core;

import com.nebhale.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A wrapper for a {@link com.nebhale.r2dbc.spi.Batch} providing additional convenience APIs
 */
public final class Batch {

    private final com.nebhale.r2dbc.spi.Batch batch;

    Batch(com.nebhale.r2dbc.spi.Batch batch) {
        this.batch = requireNonNull(batch, "batch must not be null");
    }

    /**
     * Add a statement to this batch.
     *
     * @param sql the statement to add
     * @return this {@link Batch}
     */
    public Batch add(String sql) {
        this.batch.add(sql);
        return this;
    }

    /**
     * Executes the {@link Batch} and transforms the {@link Result}s that are returned.
     *
     * @param f   a {@link Function} used to transform each {@link Result} into another value
     * @param <T> the type of results
     * @return the values resulting from the {@link Result} transformation
     * @throws NullPointerException if {@code f} is {@code null}
     */
    public <T> Flux<T> execute(Function<Result, ? extends Publisher<? extends T>> f) {
        Objects.requireNonNull(f, "f must not be null");

        return Flux.from(this.batch.execute())
            .flatMap(f::apply);
    }

    @Override
    public String toString() {
        return "Batch{" +
            "batch=" + this.batch +
            '}';
    }

}
