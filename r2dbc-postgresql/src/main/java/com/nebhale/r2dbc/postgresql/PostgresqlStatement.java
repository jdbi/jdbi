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

import com.nebhale.r2dbc.spi.Statement;
import reactor.core.publisher.Flux;

/**
 * A strongly typed implementation of {@link Statement} for a PostgreSQL database.
 */
public interface PostgresqlStatement extends Statement {

    @Override
    PostgresqlStatement add();

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException     if {@code identifier} is {@code null}
     * @throws IllegalArgumentException if {@code identifier} is not a {@link String} like {@code $1}, {@code $2}, etc.
     */
    @Override
    PostgresqlStatement bind(Object identifier, Object value);

    @Override
    PostgresqlStatement bind(Integer index, Object value);

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException     if {@code identifier} or {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code identifier} is not a {@link String} like {@code $1}, {@code $2}, etc.
     * @throws IllegalArgumentException if {@code type} is not an {@link Integer}
     */
    @Override
    PostgresqlStatement bindNull(Object identifier, Object type);

    @Override
    Flux<PostgresqlResult> execute();

    @Override
    Flux<PostgresqlResult> executeReturningGeneratedKeys();

}
