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

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.function.Function;

/**
 * A wrapper for a {@link Statement} providing additional convenience APIs for running updates such as {@code INSERT} and {@code DELETE}.
 */
public final class Update {

    private final Statement statement;

    Update(Statement statement) {
        this.statement = Objects.requireNonNull(statement, "statement must not be null");
    }

    /**
     * Save the current binding and create a new one.
     *
     * @return this {@link Statement}
     */
    public Update add() {
        this.statement.add();
        return this;
    }

    /**
     * Bind a value.
     *
     * @param identifier the identifier to bind to
     * @param value      the value to bind
     * @return this {@link Statement}
     * @throws NullPointerException if {@code identifier} or {@code value} is {@code null}
     */
    public Update bind(Object identifier, Object value) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        Objects.requireNonNull(value, "value must not be null");

        this.statement.bind(identifier, value);
        return this;
    }

    /**
     * Bind a {@code null} value.
     *
     * @param identifier the identifier to bind to
     * @param type       the type of null value
     * @return this {@link Statement}
     * @throws NullPointerException if {@code identifier} or {@code type} is {@code null}
     */
    public Update bindNull(Object identifier, Object type) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        Objects.requireNonNull(type, "type must not be null");

        this.statement.bindNull(identifier, type);
        return this;
    }

    /**
     * Executes the update and returns the number of rows that were updated.
     *
     * @return the number of rows that were updated
     */
    public Flux<Integer> execute() {
        return Flux
            .from(this.statement.execute())
            .flatMap(Result::getRowsUpdated);
    }

    /**
     * Executes one or more SQL statements and returns the {@link ResultBearing}s, including any generated keys.
     *
     * @return the {@link ResultBearing}s, returned by each statement
     */
    public Flux<ResultBearing> executeReturningGeneratedKeys() {
        return Flux
            .from(this.statement.executeReturningGeneratedKeys())
            .map(SimpleResultBearing::new);
    }

    @Override
    public String toString() {
        return "Update{" +
            "statement=" + this.statement +
            '}';
    }

    Update bind(int index, Object value) {
        Objects.requireNonNull(value, "value must not be null");

        this.statement.bind(index, value);
        return this;
    }

    private static final class SimpleResultBearing implements ResultBearing {

        private final Result result;

        private SimpleResultBearing(Result result) {
            this.result = result;
        }

        @Override
        public <T> Flux<T> mapResult(Function<Result, ? extends Publisher<? extends T>> f) {
            Objects.requireNonNull(f, "f must not be null");

            return Flux.from(f.apply(this.result));
        }

        @Override
        public String toString() {
            return "SimpleResultBearing{" +
                "result=" + this.result +
                '}';
        }

    }

}
