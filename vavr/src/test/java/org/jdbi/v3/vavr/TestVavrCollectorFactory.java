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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestVavrCollectorFactory {

    private VavrCollectorFactory unit;

    @Before
    public void setUp() {
        unit = new VavrCollectorFactory();
    }

    @Test
    public void testAcceptCollectionImplementationTypes_shouldSucceed() {
        assertTrue(unit.accepts(new GenericType<Array<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<Vector<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<List<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<Stream<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<Queue<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<PriorityQueue<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<HashSet<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<LinkedHashSet<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<TreeSet<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<HashMap<?, ?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<LinkedHashMap<?, ?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<TreeMap<?, ?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<HashMultimap<?, ?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<LinkedHashMultimap<?, ?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<TreeMultimap<?, ?>>() {}.getType()));
    }

    @Test
    public void testAcceptNonVavrCollection_shouldFail() {
        assertFalse(unit.accepts(new GenericType<java.util.List<?>>() {}.getType()));
        assertFalse(unit.accepts(new GenericType<java.util.Map<?, ?>>() {}.getType()));
    }

    @Test
    public void testAcceptCollectionSuperTypes_shouldSucceed() {
        assertTrue(unit.accepts(new GenericType<Traversable<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<Seq<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<IndexedSeq<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<LinearSeq<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<Set<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<SortedSet<?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<Map<?, ?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<SortedMap<?, ?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<Multimap<?, ?>>() {}.getType()));
        assertTrue(unit.accepts(new GenericType<SortedMultimap<?, ?>>() {}.getType()));
    }

}