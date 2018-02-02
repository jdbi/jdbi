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

/**
 * Represents the results of a query against a database.
 */
public interface Result {

    /**
     * Returns the metadata for the results of a query against a database.  May be empty if the query did not return any rows.
     *
     * @return the metadata for the results of a query against a database
     */
    Publisher<? extends RowMetadata> getRowMetadata();

    /**
     * Returns the rows that are the results of a query against a database.  May be empty if the query did not return any rows.
     *
     * @return the rows that are the results of a query against a database
     */
    Publisher<? extends Row> getRows();

    /**
     * Returns the number of rows updated by a query against a database.  May be empty if the query did not update any rows.
     *
     * @return the number of rows updated by a query against a database
     */
    Publisher<Integer> getRowsUpdated();

}
