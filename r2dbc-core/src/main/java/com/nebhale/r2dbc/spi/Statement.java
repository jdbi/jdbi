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

/**
 * A statement that can be executed multiple times in a prepared and optimized way.
 */
public interface Statement extends ResultProducer {

    /**
     * Save the current binding and create a new one.
     *
     * @return this {@link Statement}
     */
    Statement add();

    /**
     * Bind a value.
     *
     * @param identifier the identifier to bind to
     * @param value      the value to bind
     * @return this {@link Statement}
     */
    Statement bind(Object identifier, Object value);

    /**
     * Bind a value to an index.  Indexes are zero-based.
     *
     * @param index the index to bind to
     * @param value the value to bind
     * @return this {@link Statement}
     */
    Statement bind(Integer index, Object value);

    /**
     * Bind a {@code null} value.
     *
     * @param identifier the identifier to bind to
     * @param type       the type of null value
     * @return this {@link Statement}
     */
    Statement bindNull(Object identifier, Object type);

}
