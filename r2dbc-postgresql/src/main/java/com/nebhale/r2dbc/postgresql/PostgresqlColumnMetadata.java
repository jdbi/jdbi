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

import com.nebhale.r2dbc.postgresql.message.backend.RowDescription.Field;
import com.nebhale.r2dbc.spi.ColumnMetadata;

import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link ColumnMetadata} for a PostgreSQL database.
 */
public final class PostgresqlColumnMetadata implements ColumnMetadata {

    private final String name;

    private final Short precision;

    private final Integer type;

    PostgresqlColumnMetadata(String name, Short precision, Integer type) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.precision = Objects.requireNonNull(precision, "precision must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostgresqlColumnMetadata that = (PostgresqlColumnMetadata) o;
        return Objects.equals(this.name, that.name) &&
            Objects.equals(this.precision, that.precision) &&
            Objects.equals(this.type, that.type);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Optional<Integer> getPrecision() {
        return Optional.of(this.precision)
            .map(Short::intValue);
    }

    @Override
    public Integer getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.precision, this.type);
    }

    @Override
    public String toString() {
        return "PostgresqlColumnMetadata{" +
            "name='" + this.name + '\'' +
            ", precision=" + this.precision +
            ", type=" + this.type +
            '}';
    }

    static PostgresqlColumnMetadata toColumnMetadata(Field field) {
        Objects.requireNonNull(field, "field must not be null");

        return new PostgresqlColumnMetadata(field.getName(), field.getDataTypeSize(), field.getDataType());
    }

}
