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

package io.r2dbc.client;

import io.r2dbc.client.util.Assert;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An interface indicating that a type returns results.
 */
public interface ResultBearing {

    /**
     * Transforms the {@link Result}s that are returned from execution.
     *
     * @param f   a {@link Function} used to transform each {@link Result} into a {@code Publisher} of values
     * @param <T> the type of results
     * @return the values resulting from the {@link Result} transformation
     * @throws IllegalArgumentException if {@code f} is {@code null}
     * @see #mapRow(Function)
     * @see #mapRow(BiFunction)
     */
    <T> Flux<T> mapResult(Function<Result, ? extends Publisher<? extends T>> f);

    /**
     * Transforms each {@link Row} and {@link RowMetadata} pair into an object.
     *
     * @param f   a {@link BiFunction} used to transform each {@link Row} and {@link RowMetadata} pair into an object
     * @param <T> the type of results
     * @return the values resulting from the {@link Row} and {@link RowMetadata} transformation
     * @throws IllegalArgumentException if {@code f} is {@code null}
     */
    default <T> Flux<T> mapRow(BiFunction<Row, RowMetadata, ? extends T> f) {
        Assert.requireNonNull(f, "f must not be null");

        return mapResult(result -> result.map(f));
    }

    /**
     * Transforms each {@link Row} into an object.
     *
     * @param f   a {@link Function} used to transform each {@link Row} into an object
     * @param <T> the type of the results
     * @return the values resulting from the {@link Row} transformation
     * @throws IllegalArgumentException if {@code f} is {@code null}
     */
    default <T> Flux<T> mapRow(Function<Row, ? extends T> f) {
        Assert.requireNonNull(f, "f must not be null");

        return mapRow((row, rowMetadata) -> f.apply(row));
    }

}
