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

package com.nebhale.r2dbc.postgresql;

import com.nebhale.r2dbc.RowMetadata;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An implementation of {@link RowMetadata} for a PostgreSQL database.
 */
public final class PostgresqlRowMetadata implements RowMetadata {

    private final List<PostgresqlColumnMetadata> columnMetadata;

    PostgresqlRowMetadata(List<PostgresqlColumnMetadata> columnMetadata) {
        this.columnMetadata = Objects.requireNonNull(columnMetadata, "columnMetadata must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostgresqlRowMetadata that = (PostgresqlRowMetadata) o;
        return Objects.equals(this.columnMetadata, that.columnMetadata);
    }

    @Override
    public List<PostgresqlColumnMetadata> getColumnMetadata() {
        return this.columnMetadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.columnMetadata);
    }

    @Override
    public String toString() {
        return "PostgresqlRowMetadata{" +
            "columnMetadata=" + this.columnMetadata +
            '}';
    }

    static PostgresqlRowMetadata toRowMetadata(RowDescription rowDescription) {
        Objects.requireNonNull(rowDescription, "rowDescription must not be null");

        return new PostgresqlRowMetadata(rowDescription.getFields().stream()
            .map(PostgresqlColumnMetadata::toColumnMetadata)
            .collect(Collectors.toList()));
    }

}
