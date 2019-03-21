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
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * A wrapper for a {@link Statement} providing additional convenience APIs for running queries such as {@code SELECT}.
 */
public final class Query implements ResultBearing {

    private final Statement statement;

    Query(Statement statement) {
        this.statement = Assert.requireNonNull(statement, "statement must not be null");
    }

    /**
     * Save the current binding and create a new one.
     *
     * @return this {@link Statement}
     */
    public Query add() {
        this.statement.add();
        return this;
    }

    /**
     * Bind a value.
     *
     * @param identifier the identifier to bind to
     * @param value      the value to bind
     * @return this {@link Statement}
     * @throws IllegalArgumentException if {@code identifier} or {@code value} is {@code null}
     */
    public Query bind(Object identifier, Object value) {
        Assert.requireNonNull(identifier, "identifier must not be null");
        Assert.requireNonNull(value, "value must not be null");

        this.statement.bind(identifier, value);
        return this;
    }

    /**
     * Bind a {@code null} value.
     *
     * @param identifier the identifier to bind to
     * @param type       the type of null value
     * @return this {@link Statement}
     * @throws IllegalArgumentException if {@code identifier} or {@code type} is {@code null}
     */
    public Query bindNull(Object identifier, Class<?> type) {
        Assert.requireNonNull(identifier, "identifier must not be null");
        Assert.requireNonNull(type, "type must not be null");

        this.statement.bindNull(identifier, type);
        return this;
    }

    public <T> Flux<T> mapResult(Function<Result, ? extends Publisher<? extends T>> f) {
        Assert.requireNonNull(f, "f must not be null");

        return Flux
            .from(this.statement.execute())
            .flatMap(f::apply);
    }

    @Override
    public String toString() {
        return "Query{" +
            "statement=" + this.statement +
            '}';
    }

    Query bind(int identifier, Object value) {
        Assert.requireNonNull(value, "value must not be null");

        this.statement.bind(identifier, value);
        return this;
    }

}
