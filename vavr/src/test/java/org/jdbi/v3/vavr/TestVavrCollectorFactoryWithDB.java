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

import io.vavr.Tuple2;
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
import io.vavr.collection.SortedSet;
import io.vavr.collection.Stream;
import io.vavr.collection.Traversable;
import io.vavr.collection.TreeMap;
import io.vavr.collection.TreeMultimap;
import io.vavr.collection.TreeSet;
import io.vavr.collection.Vector;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestVavrCollectorFactoryWithDB {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something())
            .withPlugins(new SqlObjectPlugin(), new VavrPlugin());

    private Seq<Integer> expected = List.range(0, 10);
    private Map<Integer, String> expectedMap = expected.toMap(i -> new Tuple2<>(i, i + "asString"));

    @BeforeEach
    public void addData() {
        for (Integer i : expected) {
            h2Extension.getSharedHandle().execute("insert into something(name, intValue) values (?, ?)", Integer.toString(i) + "asString", i);
        }
    }

    @Test
    public void testToConcreteCollectionTypesShouldSucceed() {
        testType(new GenericType<Array<Integer>>() {});
        testType(new GenericType<Vector<Integer>>() {});
        testType(new GenericType<List<Integer>>() {});
        testType(new GenericType<Stream<Integer>>() {});
        testType(new GenericType<Queue<Integer>>() {});
        testType(new GenericType<PriorityQueue<Integer>>() {});
        testType(new GenericType<HashSet<Integer>>() {});
        testType(new GenericType<LinkedHashSet<Integer>>() {});
        testType(new GenericType<TreeSet<Integer>>() {});
    }

    private <T extends Iterable<Integer>> void testType(GenericType<T> containerType) {
        T values = h2Extension.getSharedHandle().createQuery("select intValue from something")
            .collectInto(containerType);
        assertThat(values).hasSameElementsAs(expected);
    }

    @Test
    public void testToAbstractCollectionTypesShouldSucceed() {
        testType(new GenericType<Traversable<Integer>>() {});
        testType(new GenericType<Seq<Integer>>() {});
        testType(new GenericType<IndexedSeq<Integer>>() {});
        testType(new GenericType<LinearSeq<Integer>>() {});
        testType(new GenericType<Set<Integer>>() {});
        testType(new GenericType<SortedSet<Integer>>() {});
    }

    @Test
    public void testMapCollector() {
        testMapType(new GenericType<HashMap<Integer, String>>() {});
        testMapType(new GenericType<LinkedHashMap<Integer, String>>() {});
        testMapType(new GenericType<TreeMap<Integer, String>>() {});

        testMapType(new GenericType<HashMultimap<Integer, String>>() {});
        testMapType(new GenericType<LinkedHashMultimap<Integer, String>>() {});
        testMapType(new GenericType<TreeMultimap<Integer, String>>() {});
    }

    private <T extends Traversable<Tuple2<Integer, String>>> void testMapType(GenericType<T> containerType) {
        T values = h2Extension.getSharedHandle().createQuery("select intValue, name from something")
            .collectInto(containerType);
        assertThat(values).hasSameElementsAs(expectedMap);
    }

    @Test
    public void testMapCollectorReversedShouldFail() {
        assertThatThrownBy(() -> h2Extension.getSharedHandle()
            .createQuery("select intValue, name from something")
            .collectInto(new GenericType<HashMap<String, Integer>>() {}))
                .isInstanceOf(ResultSetException.class);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testMultimapValuesAddAnotherDataSetShouldHave2ValuesForEachKey() {
        final int offset = 10;
        for (Integer i : expected) {
            h2Extension.getSharedHandle().execute("insert into something(name, intValue) values (?, ?)", Integer.toString(i + offset) + "asString", i);
        }

        Multimap<Integer, String> result = h2Extension.getSharedHandle().createQuery("select intValue, name from something")
            .collectInto(new GenericType<Multimap<Integer, String>>() {});

        assertThat(result).hasSize(expected.size() * 2);
        expected.forEach(i -> assertThat(result.apply(i))
                .hasSameElementsAs(List.of(i + "asString", (i + 10) + "asString")));
    }
}
