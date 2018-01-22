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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * A row reducer that uses {@link LinkedHashMap} (which preserves insertion order) as a
 * result container, and returns {@code map.values().stream()} as the final result.
 *
 * <p>Implementors need only implement the {@link #accumulate(Object, RowView)} method.
 *
 * @param <K> the map key type--often the primary key type of {@code <V>}.
 * @param <V> the map value type, and the result element type--often the "master" object in a
 *     master/detail relation.
 */
public abstract class LinkedHashMapRowReducer<K, V> implements RowReducer<Map<K, V>, V> {
  @Override
  public Map<K, V> container() {
    return new LinkedHashMap<>();
  }

  @Override
  public Stream<V> stream(Map<K, V> container) {
    return container.values().stream();
  }

  public static <K, V> RowReducer<?, V> of(BiConsumer<Map<K, V>, RowView> accumulator) {
    return new LinkedHashMapRowReducer<K, V>() {
      @Override
      public void accumulate(Map<K, V> map, RowView rowView) {
        accumulator.accept(map, rowView);
      }
    };
  }
}
