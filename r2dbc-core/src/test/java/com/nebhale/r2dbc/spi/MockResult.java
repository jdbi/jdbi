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

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class MockResult implements Result {

    public static final MockResult EMPTY = builder().build();

    private final Flux<RowMetadata> rowMetadata;

    private final Flux<Row> rows;

    private final Flux<Integer> rowsUpdated;

    private MockResult(Flux<RowMetadata> rowMetadata, Flux<Row> rows, Flux<Integer> rowsUpdated) {
        this.rowMetadata = requireNonNull(rowMetadata);
        this.rows = requireNonNull(rows);
        this.rowsUpdated = requireNonNull(rowsUpdated);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Flux<RowMetadata> getRowMetadata() {
        return this.rowMetadata;
    }

    @Override
    public Flux<Row> getRows() {
        return this.rows;
    }

    @Override
    public Flux<Integer> getRowsUpdated() {
        return this.rowsUpdated;
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

        private final List<RowMetadata> rowMetadata = new ArrayList<>();

        private final List<Row> rows = new ArrayList<>();

        private final List<Integer> rowsUpdated = new ArrayList<>();

        private Builder() {
        }

        public MockResult build() {
            return new MockResult(Flux.fromIterable(this.rowMetadata), Flux.fromIterable(this.rows), Flux.fromIterable(this.rowsUpdated));
        }

        public Builder row(Row row) {
            this.rows.add(requireNonNull(row));
            return this;
        }

        public Builder rowMetadata(RowMetadata rowMetadata) {
            this.rowMetadata.add(requireNonNull(rowMetadata));
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
