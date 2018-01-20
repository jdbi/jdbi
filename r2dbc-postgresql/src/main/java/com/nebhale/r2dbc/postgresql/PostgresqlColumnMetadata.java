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

import com.nebhale.r2dbc.ColumnMetadata;
import com.nebhale.r2dbc.postgresql.message.backend.RowDescription.Field;

import java.util.Objects;

/**
 * An implementation of {@link ColumnMetadata} for a PostgreSQL database.
 */
public final class PostgresqlColumnMetadata implements ColumnMetadata {

    private final String name;

    PostgresqlColumnMetadata(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
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
        return Objects.equals(this.name, that.name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    @Override
    public String toString() {
        return "PostgresqlColumnMetadata{" +
            "name='" + this.name + '\'' +
            '}';
    }

    static PostgresqlColumnMetadata toColumnMetadata(Field field) {
        Objects.requireNonNull(field, "field must not be null");

        return new PostgresqlColumnMetadata(field.getName());
    }

}
