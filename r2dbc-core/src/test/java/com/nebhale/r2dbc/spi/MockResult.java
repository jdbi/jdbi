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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class MockResult implements Result {

    private final Mono<RowMetadata> rowMetadata;

    private final Flux<Row> rows;

    private final Flux<Integer> rowsUpdated;

    private MockResult(Mono<RowMetadata> rowMetadata, Flux<Row> rows, Flux<Integer> rowsUpdated) {
        this.rowMetadata = requireNonNull(rowMetadata);
        this.rows = requireNonNull(rows);
        this.rowsUpdated = requireNonNull(rowsUpdated);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MockResult empty() {
        return builder().build();
    }

    @Override
    public Flux<Integer> getRowsUpdated() {
        return this.rowsUpdated;
    }

    @Override
    public <T> Flux<T> map(BiFunction<Row, RowMetadata, ? extends T> f) {
        return this.rows
            .zipWith(this.rowMetadata.repeat())
            .map((tuple) -> {
                Row row = tuple.getT1();
                RowMetadata rowMetadata = tuple.getT2();

                return f.apply(row, rowMetadata);
            });
    }

    @Override
    public String toString() {
        return "MockResult{" +
            "rowMetadata=" + this.rowMetadata +
            ", rows=" + this.rows +
            ", rowsUpdated=" + this.rowsUpdated +
            '}';
    }

    public static final class Builder {

        private final List<Row> rows = new ArrayList<>();

        private final List<Integer> rowsUpdated = new ArrayList<>();

        private RowMetadata rowMetadata;

        private Builder() {
        }

        public MockResult build() {
            return new MockResult(Mono.justOrEmpty(this.rowMetadata), Flux.fromIterable(this.rows), Flux.fromIterable(this.rowsUpdated));
        }

        public Builder row(Row... rows) {
            Stream.of(rows)
                .peek(Objects::requireNonNull)
                .forEach(this.rows::add);

            return this;
        }

        public Builder rowMetadata(RowMetadata rowMetadata) {
            this.rowMetadata = requireNonNull(rowMetadata);
            return this;
        }

        public Builder rowsUpdated(Integer rowsUpdated) {
            this.rowsUpdated.add(requireNonNull(rowsUpdated));
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "rowMetadata=" + this.rowMetadata +
                ", rows=" + this.rows +
                ", rowsUpdated=" + this.rowsUpdated +
                '}';
        }

    }

}
