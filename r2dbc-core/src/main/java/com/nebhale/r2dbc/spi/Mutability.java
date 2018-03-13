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

import java.util.Objects;

/**
 * SQL transaction mutability options.
 */
public enum Mutability {

    /**
     * Read-only mutability.
     */
    READ_ONLY("READ ONLY"),

    /**
     * Read-write mutability.
     */
    READ_WRITE("READ WRITE");

    private final String sql;

    Mutability(String sql) {
        this.sql = Objects.requireNonNull(sql, "sql must not be null");
    }

    /**
     * Returns the SQL string represented be each value.
     *
     * @return the SQL string represented be each value
     */
    public String asSql() {
        return this.sql;
    }

}
