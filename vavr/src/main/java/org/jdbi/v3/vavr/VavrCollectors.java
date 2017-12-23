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
package org.jdbi.v3.vavr;

import java.util.stream.Collector;

import io.vavr.control.Option;
import org.jdbi.v3.core.collector.OptionalCollectors;

public class VavrCollectors {
  /**
   * Returns a {@code Collector} that accumulates 0 or 1 input elements into an {@code Option<T>}.
   * The returned collector will throw {@code IllegalStateException} whenever 2 or more elements
   * are present in a stream. Null elements are mapped to {@code Option.none()}.
   *
   * @param <T> the collected type
   * @return a {@code Collector} which collects 0 or 1 input elements into an {@code Option<T>}.
   */
  public static <T> Collector<T, ?, Option<T>> toOption() {
    return OptionalCollectors.toOptional(Option::none, Option::of);
  }
}
