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
import java.util.stream.Stream;

/**
 * A row reducer that uses {@link LinkedHashMap} (which preserves insertion order) as a
 * result container, and returns {@code map.values().stream()} as the final result.
 *
 * <p>Implementors need only implement the {@link #accumulate(Object, RowView)} method.
 *
 * @param <K> the map key type--often the primary key type of {@code <V>}.
 * @param <V> the map value type, and the result element type--often the "master" object in a
 *            master/detail relation.
 */
@FunctionalInterface
public interface LinkedHashMapRowReducer<K, V> extends RowReducer<Map<K, V>, V> {
    @Override
    default Map<K, V> container() {
        return new LinkedHashMap<>();
    }

    @Override
    default Stream<V> stream(Map<K, V> container) {
        return container.values().stream();
    }
}
