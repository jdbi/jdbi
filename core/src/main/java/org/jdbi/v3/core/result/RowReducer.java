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
 * @param <A> the mutable accumulator type
 * @param <R> result element type
 * @see ResultBearing#reduceRows(RowReducer)
 */
public interface RowReducer<A, R> {
  /**
   * Returns a new accumulator in a "blank" state.
   *
   * @return a new accumulator.
   */
  A createAccumulator();

  /**
   * Folds the current row data into the accumulator.
   *
   * @param accumulator the accumulator
   * @param rowView row view of the current row of the result set
   */
  void accumulate(A accumulator, RowView rowView);

  /**
   * Returns the final result from the accumulator as a stream of elements.
   *
   * @param accumulator the accumulator
   * @return stream of result elements.
   */
  Stream<R> stream(A accumulator);
}
