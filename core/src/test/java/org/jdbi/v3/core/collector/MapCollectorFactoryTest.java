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
import static org.assertj.core.api.Assertions.entry;

public class MapCollectorFactoryTest {
    private MapCollectorFactory factory = new MapCollectorFactory();

    @Test
    public void accepts() throws Exception {
        assertThat(factory.accepts(new GenericType<Map<String,Long>>(){}.getType())).isTrue();
        assertThat(factory.accepts(new GenericType<LinkedHashMap<String,Long>>(){}.getType())).isTrue();
        assertThat(factory.accepts(new GenericType<HashMap<String,Long>>(){}.getType())).isTrue();
        assertThat(factory.accepts(new GenericType<TreeMap<String,Long>>(){}.getType())).isTrue();
        assertThat(factory.accepts(new GenericType<SortedMap<String,Long>>(){}.getType())).isTrue();
        assertThat(factory.accepts(new GenericType<ConcurrentMap<String,Long>>(){}.getType())).isTrue();
        assertThat(factory.accepts(new GenericType<ConcurrentHashMap<String,Long>>(){}.getType())).isTrue();
        assertThat(factory.accepts(new GenericType<WeakHashMap<String,Long>>(){}.getType())).isTrue();

        assertThat(factory.accepts(Map.class)).isFalse();
        assertThat(factory.accepts(new GenericType<Map.Entry<String,Long>>(){}.getType())).isFalse();
    }

    @Test
    public void elementType() throws Exception {
        assertThat(factory.elementType(new GenericType<Map<String,Long>>(){}.getType()))
                .contains(new GenericType<Map.Entry<String,Long>>(){}.getType());
    }

    @Test
    public void build() throws Exception {
        testBuild(new GenericType<Map<String, Integer>>() {}, LinkedHashMap.class);
        testBuild(new GenericType<LinkedHashMap<String, Integer>>() {}, LinkedHashMap.class);
        testBuild(new GenericType<HashMap<String, Integer>>() {}, HashMap.class);
        testBuild(new GenericType<TreeMap<String, Integer>>() {}, TreeMap.class);
        testBuild(new GenericType<SortedMap<String, Integer>>() {}, TreeMap.class);
        testBuild(new GenericType<ConcurrentMap<String, Integer>>() {}, ConcurrentHashMap.class);
        testBuild(new GenericType<WeakHashMap<String, Integer>>() {}, WeakHashMap.class);
    }

    @SuppressWarnings("unchecked")
    private <K,V,G extends Map<K,V>,M extends Map> void testBuild(GenericType<G> genericType, Class<M> expectedType) {
        Collector<Map.Entry<String, Integer>, ?, Map<String, Integer>> collector =
                (Collector<Map.Entry<String, Integer>, ?, Map<String, Integer>>)
                        factory.build(genericType.getType());

        Stream<Map.Entry<String, Integer>> stream = Stream.of(entry("foo", 1), entry("bar", 2), entry("baz", 3));

        assertThat(stream.collect(collector))
                .isInstanceOf(expectedType)
                .containsOnly(entry("foo", 1), entry("bar", 2), entry("baz", 3));
    }
}
