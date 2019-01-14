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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.generic.GenericType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;
import static org.jdbi.v3.core.generic.GenericTypes.resolveMapEntryType;

public class MapCollectorFactoryTest {
    private CollectorFactory factory = new MapCollectorFactory();

    @Test
    public void maps() {
        testMapType(new GenericType<Map<Long, String>>() {});
        testMapType(new GenericType<HashMap<Long, String>>() {});
        testMapType(new GenericType<LinkedHashMap<Long, String>>() {});
        testMapType(new GenericType<SortedMap<Long, String>>() {});
        testMapType(new GenericType<TreeMap<Long, String>>() {});
        testMapType(new GenericType<ConcurrentMap<Long, String>>() {});
        testMapType(new GenericType<ConcurrentHashMap<Long, String>>() {});
        testMapType(new GenericType<WeakHashMap<Long, String>>() {});
    }

    private <M extends Map<Long, String>> void testMapType(GenericType<M> genericType) {
        Type containerType = genericType.getType();
        Class<?> erasedType = getErasedType(containerType);
        assertThat(factory.accepts(containerType)).isTrue();
        assertThat(factory.accepts(erasedType)).isFalse();
        assertThat(factory.elementType(containerType)).contains(resolveMapEntryType(Long.class, String.class));

        Collector<Map.Entry<Long, String>, ?, M> collector = (Collector<Map.Entry<Long, String>, ?, M>) factory.build(containerType);
        assertThat(Stream.of(entry(1L, "foo"), entry(2L, "bar"), entry(3L, "baz")).collect(collector))
                .isInstanceOf(erasedType)
                .containsOnly(entry(1L, "foo"), entry(2L, "bar"), entry(3L, "baz"));
        assertThatThrownBy(() -> Stream.of(entry(1L, "foo"), entry(1L, "bar")).collect(collector))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple values");
    }
}
