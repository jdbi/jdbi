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

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class MockRowMetadata implements RowMetadata {

    private final List<ColumnMetadata> columnMetadatas;

    private MockRowMetadata(List<ColumnMetadata> columnMetadatas) {
        this.columnMetadatas = requireNonNull(columnMetadatas);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MockRowMetadata empty() {
        return builder().build();
    }

    @Override
    public ColumnMetadata getColumnMetadata(Object identifier) {
        requireNonNull(identifier);

        return this.columnMetadatas.get((Integer) identifier);
    }

    @Override
    public List<ColumnMetadata> getColumnMetadatas() {
        return this.columnMetadatas;
    }

    @Override
    public String toString() {
        return "MockRowMetadata{" +
            "columnMetadatas=" + this.columnMetadatas +
            '}';
    }

    public static final class Builder {

        private final List<ColumnMetadata> columnMetadatas = new ArrayList<>();

        private Builder() {
        }

        public MockRowMetadata build() {
            return new MockRowMetadata(this.columnMetadatas);
        }

        public Builder columnMetadata(ColumnMetadata columnMetadata) {
            this.columnMetadatas.add(columnMetadata);
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "columnMetadatas=" + this.columnMetadatas +
                '}';
        }

    }

}
