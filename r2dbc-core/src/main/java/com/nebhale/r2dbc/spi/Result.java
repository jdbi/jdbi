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

import org.reactivestreams.Publisher;

import java.util.function.BiFunction;

/**
 * Represents the results of a query against a database.
 */
public interface Result {

    /**
     * Returns the number of rows updated by a query against a database.  May be empty if the query did not update any rows.
     *
     * @return the number of rows updated by a query against a database
     */
    Publisher<Integer> getRowsUpdated();

    /**
     * Returns a mapping of the rows that are the results of a query against a database.  May be empty if the query did not return any rows.
     *
     * @param f   the {@link BiFunction} that maps a {@link Row} and {@link RowMetadata} to a value
     * @param <T> the type of the mapped value
     * @return a mapping of the rows that are the results of a query against a database
     * @throws NullPointerException if {@code f} is {@code null}
     */
    <T> Publisher<T> map(BiFunction<Row, RowMetadata, ? extends T> f);

}
