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
package org.jdbi.v3.core.collector;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Factory methods for {@link Collector collectors} of optional types.
 */
public class OptionalCollectors {
  /**
   * Returns a {@code Collector} that accumulates 0 or 1 input elements into an {@code Optional<T>}.
   * The returned collector will throw {@code IllegalStateException} whenever 2 or more elements
   * are present in a stream. Null elements are mapped to {@code Optional.empty()}.
   *
   * @param <T> the collected type
   * @return a {@code Collector} which collects 0 or 1 input elements into an {@code Optional<T>}.
   */
  public static <T> Collector<T, ?, Optional<T>> toOptional() {
    return toOptional(Optional::empty, Optional::of);
  }

  /**
   * Returns a {@code Collector} that accumulates 0 or 1 input {@code Integer} elements into an
   * {@code OptionalInt}. The returned collector will throw {@code IllegalStateException} whenever
   * 2 or more elements are present in a stream. Null elements are mapped to
   * {@code OptionalInt.empty()}.
   *
   * @return a {@code Collector} which collects 0 or 1 input {@code Integer} elements into an
   * {@code OptionalInt}.
   */
  public static Collector<Integer, ?, OptionalInt> toOptionalInt() {
    return toOptional(OptionalInt::empty, OptionalInt::of);
  }

  /**
   * Returns a {@code Collector} that accumulates 0 or 1 input {@code Long} elements into an
   * {@code OptionalLong}. The returned collector will throw {@code IllegalStateException} whenever
   * 2 or more elements are present in a stream. Null elements are mapped to
   * {@code OptionalLong.empty()}.
   *
   * @return a {@code Collector} which collects 0 or 1 input @{code Long} elements into an
   * {@code OptionalLong}.
   */
  public static Collector<Long, ?, OptionalLong> toOptionalLong() {
    return toOptional(OptionalLong::empty, OptionalLong::of);
  }

  /**
   * Returns a {@code Collector} that accumulates 0 or 1 input {@code Double} elements into an
   * {@code OptionalDouble}. The returned collector will throw {@code IllegalStateException}
   * whenever 2 or more elements are present in a stream. Null elements are mapped to
   * {@code OptionalDouble.empty()}.
   *
   * @return a {@code Collector} which collects 0 or 1 input @{code Double} elements into an
   * {@code OptionalDouble}.
   */
  public static Collector<Double, ?, OptionalDouble> toOptionalDouble() {
    return toOptional(OptionalDouble::empty, OptionalDouble::of);
  }

  /**
   * Returns a {@code Collector} that accumulates 0 or 1 input elements into an arbitrary
   * optional-style container type. The returned collector will throw
   * {@code IllegalStateException} whenever 2 or more elements are present in a stream. Null elements
   * are mapped to an empty container.
   *
   * @param empty   Supplies an instance of the optional type with no value.
   * @param factory Returns an instance of the optional type with the input parameter as the value.
   * @param <T>     The optional element type.
   * @param <OPT_T> The optional type (including any element type arguments)
   * @return a {@code Collector} which collects 0 or 1 input elements into an arbitrary
   * optional-style container type.
   */
  public static <T, OPT_T> Collector<T, ?, OPT_T> toOptional(Supplier<OPT_T> empty,
                                                             Function<T, OPT_T> factory) {
    return Collector.of(
        () -> new OptionalBuilder<>(empty, factory),
        OptionalBuilder::set,
        OptionalBuilder::combine,
        OptionalBuilder::build);
  }
}
