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

import io.r2dbc.client.util.ReactiveUtils;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;

/**
 * An implementation of the Reactive Relational Database Connection API for PostgreSQL servers.
 */
public final class R2dbc {

    private final ConnectionFactory connectionFactory;

    /**
     * Create a new instance of {@link R2dbc}.
     *
     * @param connectionFactory a {@link ConnectionFactory} used to create {@link Connection}s when required
     * @throws NullPointerException if {@code connectionFactory} is {@code null}
     */
    public R2dbc(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory must not be null");
    }

    /**
     * Execute behavior within a transaction returning results.  The transaction is committed if the behavior completes successfully, and rolled back it produces an error.
     *
     * @param f   a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of results
     * @param <T> the type of results
     * @return a {@link Flux} of results
     * @throws NullPointerException if {@code f} is {@code null}
     * @see Connection#commitTransaction()
     * @see Connection#rollbackTransaction()
     */
    public <T> Flux<T> inTransaction(Function<Handle, ? extends Publisher<? extends T>> f) {
        Objects.requireNonNull(f, "f must not be null");

        return withHandle(handle -> handle.inTransaction(f));
    }

    /**
     * Open a {@link Handle} and return it for use.  Note that you the caller is responsible for closing the handle otherwise connections will be leaked.
     *
     * @return a new {@link Handle}, ready to use
     * @see Handle#close()
     */
    public Mono<Handle> open() {
        return Mono.from(
            this.connectionFactory.create())
            .map(Handle::new);
    }

    @Override
    public String toString() {
        return "R2dbc{" +
            "connectionFactory=" + this.connectionFactory +
            '}';
    }

    /**
     * Execute behavior with a {@link Handle} not returning results.
     *
     * @param f a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of results.  These results are discarded.
     * @return a {@link Mono} that execution is complete
     * @throws NullPointerException if {@code f} is {@code null}
     */
    public Mono<Void> useHandle(Function<Handle, ? extends Publisher<?>> f) {
        Objects.requireNonNull(f, "f must not be null");

        return withHandle(f)
            .then();
    }

    /**
     * Execute behavior within a transaction not returning results.  The transaction is committed if the behavior completes successfully, and rolled back it produces an error.
     *
     * @param f a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of results.  These results are discarded.
     * @return a {@link Mono} that execution is complete
     * @throws NullPointerException if {@code f} is {@code null}
     * @see Connection#commitTransaction()
     * @see Connection#rollbackTransaction()
     */
    public Mono<Void> useTransaction(Function<Handle, ? extends Publisher<?>> f) {
        Objects.requireNonNull(f, "f must not be null");

        return useHandle(handle -> handle.useTransaction(f));
    }

    /**
     * Execute behavior with a {@link Handle} returning results.
     *
     * @param f   a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of results
     * @param <T> the type of results
     * @return a {@link Flux} of results
     * @throws NullPointerException if {@code f} is {@code null}
     */
    public <T> Flux<T> withHandle(Function<Handle, ? extends Publisher<? extends T>> f) {
        Objects.requireNonNull(f, "f must not be null");

        return open()
            .flatMapMany(handle -> Flux.from(
                f.apply(handle))
                .concatWith(ReactiveUtils.typeSafe(handle::close))
                .onErrorResume(ReactiveUtils.appendError(handle::close)));
    }

}
