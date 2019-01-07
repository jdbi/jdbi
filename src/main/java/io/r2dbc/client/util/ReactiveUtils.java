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

package io.r2dbc.client.util;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utilities for working with Reactive flows.
 */
public final class ReactiveUtils {

    private ReactiveUtils() {
    }

    /**
     * Execute the {@link Publisher} provided by a {@link Supplier} and propagate the error that initiated this behavior.  Typically used with {@link Flux#onErrorResume(Function)} and
     * {@link Mono#onErrorResume(Function)}.
     *
     * @param s   a {@link Supplier} of a {@link Publisher} to execute when an error occurs
     * @param <T> the type passing through the flow
     * @return a {@link Mono#error(Throwable)} with the original error
     * @see Flux#onErrorResume(Function)
     * @see Mono#onErrorResume(Function)
     */
    public static <T> Function<Throwable, Mono<T>> appendError(Supplier<Publisher<?>> s) {
        Assert.requireNonNull(s, "s must not be null");

        return t ->
            Flux.from(s.get())
                .then(Mono.error(t));
    }

    /**
     * Convert a {@code Publisher<Void>} to a {@code Publisher<T>} allowing for type passthrough behavior.
     *
     * @param s   a {@link Supplier} of a {@link Publisher} to execute
     * @param <T> the type passing through the flow
     * @return {@link Mono#empty()} of the appropriate type
     */
    public static <T> Mono<T> typeSafe(Supplier<Publisher<Void>> s) {
        Assert.requireNonNull(s, "s must not be null");

        return Flux.from(s.get())
            .then(Mono.empty());
    }

}
