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

import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashMultimap;
import io.vavr.collection.HashSet;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.LinearSeq;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashMultimap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Multimap;
import io.vavr.collection.PriorityQueue;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.SortedMap;
import io.vavr.collection.SortedMultimap;
import io.vavr.collection.SortedSet;
import io.vavr.collection.Stream;
import io.vavr.collection.Traversable;
import io.vavr.collection.TreeMap;
import io.vavr.collection.TreeMultimap;
import io.vavr.collection.TreeSet;
import io.vavr.collection.Vector;
import org.jdbi.v3.core.generic.GenericType;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestVavrCollectorFactory {

    private VavrCollectorFactory unit;

    @Before
    public void setUp() {
        unit = new VavrCollectorFactory();
    }

    @Test
    public void testAcceptCollectionImplementationTypesShouldSucceed() {
        assertThat(unit.accepts(new GenericType<Array<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<Vector<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<List<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<Stream<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<Queue<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<PriorityQueue<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<HashSet<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<LinkedHashSet<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<TreeSet<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<HashMap<?, ?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<LinkedHashMap<?, ?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<TreeMap<?, ?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<HashMultimap<?, ?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<LinkedHashMultimap<?, ?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<TreeMultimap<?, ?>>() {}.getType())).isTrue();
    }

    @Test
    public void testAcceptNonVavrCollectionShouldFail() {
        assertThat(unit.accepts(new GenericType<java.util.List<?>>() {}.getType())).isFalse();
        assertThat(unit.accepts(new GenericType<java.util.Map<?, ?>>() {}.getType())).isFalse();
    }

    @Test
    public void testAcceptCollectionSuperTypesShouldSucceed() {
        assertThat(unit.accepts(new GenericType<Traversable<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<Seq<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<IndexedSeq<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<LinearSeq<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<Set<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<SortedSet<?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<Map<?, ?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<SortedMap<?, ?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<Multimap<?, ?>>() {}.getType())).isTrue();
        assertThat(unit.accepts(new GenericType<SortedMultimap<?, ?>>() {}.getType())).isTrue();
    }

}
