/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.result;

import java.util.stream.Stream;

/**
 * Reduces row data from a {@link java.sql.ResultSet} into a stream of result
 * elements. Useful for collapsing one-to-many joins.
 *
 * @param <C> mutable result container type
 * @param <R> result element type
 * @see ResultBearing#reduceRows(RowReducer)
 */
public interface RowReducer<C, R> {
    /**
     * Returns a new, empty result container.
     *
     * @return a new result container.
     */
    C container();

    /**
     * Accumulate data from the current row into the result container. Do not attempt
     * to accumulate the {@link RowView} itself into the result container--it is only
     * valid within the {@link RowReducer#accumulate(Object, RowView) accumulate()}
     * method invocation. Instead, extract mapped types from the RowView by calling
     * {@code RowView.getRow()} or {@code RowView.getColumn()} and store those values
     * in the container.
     *
     * @param container the result container
     * @param rowView row view over the current result set row.
     */
    void accumulate(C container, RowView rowView);

    /**
     * Returns a stream of result elements from the result container.
     *
     * @param container the result container
     * @return stream of result elements.
     */
    Stream<R> stream(C container);

}
