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
package org.jdbi.v3.guava;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdbi.v3.core.collector.JdbiCollectors;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.entry;

public class TestGuavaCollectors {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugins();

    private Collection<Integer> expected;

    @Before
    public void addData() {
        ImmutableList.Builder<Integer> ints = ImmutableList.builder();
        for (int i = 0; i < 10; i++) {
            dbRule.getSharedHandle().execute("insert into something(name, intValue) values (?, ?)", Integer.toString(i), i);
            ints.add(i);
        }
        this.expected = ints.build();
    }

    @Test
    public void immutableList() {
        ImmutableList<Integer> list = dbRule.getSharedHandle().createQuery("select intValue from something")
                .collectInto(new GenericType<ImmutableList<Integer>>(){});

        assertThat(list).containsOnlyElementsOf(expected);
    }

    @Test
    public void immutableSet() {
        ImmutableSet<Integer> set = dbRule.getSharedHandle().createQuery("select intValue from something")
                .collectInto(new GenericType<ImmutableSet<Integer>>(){});

        assertThat(set).containsOnlyElementsOf(expected);
    }

    @Test
    public void immutableSortedSet() {
        ImmutableSortedSet<Integer> set = dbRule.getSharedHandle().createQuery("select intValue from something")
                .collectInto(new GenericType<ImmutableSortedSet<Integer>>(){});

        assertThat(set).containsExactlyElementsOf(expected);
    }

    @Test
    public void immutableSortedSetWithComparator() {
        Comparator<Integer> comparator = Comparator.<Integer>naturalOrder().reversed();
        ImmutableSortedSet<Integer> set = dbRule.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(ImmutableSortedSet.toImmutableSortedSet(comparator));

        assertThat(set).containsExactlyElementsOf(expected.stream()
                .sorted(comparator)
                .collect(Collectors.toList()));
    }

    @Test
    public void optionalPresent() {
        Optional<Integer> shouldBePresent = dbRule.getSharedHandle().createQuery("select intValue from something where intValue = 1")
                .collectInto(new GenericType<Optional<Integer>>(){});
        assertThat(shouldBePresent).contains(1);
    }

    @Test
    public void optionalAbsent() {
        Optional<Integer> shouldBeAbsent = dbRule.getSharedHandle().createQuery("select intValue from something where intValue = 100")
                .collectInto(new GenericType<Optional<Integer>>(){});
        assertThat(shouldBeAbsent).isAbsent();
    }

    @Test
    public void optionalMultiple() {
        assertThatThrownBy(() -> dbRule.getSharedHandle().createQuery("select intValue from something")
            .collectInto(new GenericType<Optional<Integer>>() {})).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void mapCollectors() {
        testMapCollector(ImmutableMap.class, new GenericType<ImmutableMap<Long, String>>(){});
        testMapCollector(BiMap.class, new GenericType<BiMap<Long, String>>(){});
    }

    @SuppressWarnings("unchecked")
    private <M extends Map<Long, String>> void testMapCollector(Class<? extends Map> erasedType, GenericType<M> genericType) {
        JdbiCollectors registry = dbRule.getJdbi().getConfig(JdbiCollectors.class);

        assertThat(registry.findElementTypeFor(genericType.getType()))
                .contains(new GenericType<Map.Entry<Long, String>>(){}.getType());

        Collector<Map.Entry<Long, String>, ?, M> collector = (Collector<Map.Entry<Long, String>, ?, M>) registry
                .findFor(genericType.getType())
                .orElseThrow(() -> new IllegalStateException("Missing collector for " + genericType));

        M map = Stream.of(entry(1L, "foo"), entry(2L, "bar")).collect(collector);
        assertThat(map)
                .isInstanceOf(erasedType)
                .containsExactly(entry(1L, "foo"), entry(2L, "bar"));
    }

    @Test
    public void multimapCollectors() {
        testMultimapCollector(ImmutableMultimap.class, new GenericType<ImmutableMultimap<Long, String>>(){});
        testMultimapCollector(ImmutableListMultimap.class, new GenericType<ImmutableListMultimap<Long, String>>(){});
        testMultimapCollector(ImmutableSetMultimap.class, new GenericType<ImmutableSetMultimap<Long, String>>(){});
        testMultimapCollector(Multimap.class, new GenericType<Multimap<Long, String>>(){});
        testMultimapCollector(ListMultimap.class, new GenericType<ListMultimap<Long, String>>(){});
        testMultimapCollector(ArrayListMultimap.class, new GenericType<ArrayListMultimap<Long, String>>(){});
        testMultimapCollector(LinkedListMultimap.class, new GenericType<LinkedListMultimap<Long, String>>(){});
        testMultimapCollector(SetMultimap.class, new GenericType<SetMultimap<Long, String>>(){});
        testMultimapCollector(HashMultimap.class, new GenericType<HashMultimap<Long, String>>(){});
        testMultimapCollector(TreeMultimap.class, new GenericType<TreeMultimap<Long, String>>(){});
    }

    @SuppressWarnings("unchecked")
    private <M extends Multimap<Long, String>> void testMultimapCollector(Class<? extends Multimap> erasedType, GenericType<M> genericType) {
        JdbiCollectors registry = dbRule.getJdbi().getConfig(JdbiCollectors.class);

        assertThat(registry.findElementTypeFor(genericType.getType()))
                .contains(new GenericType<Map.Entry<Long, String>>(){}.getType());

        Collector<Map.Entry<Long, String>, ?, M> collector = (Collector<Map.Entry<Long, String>, ?, M>) registry
                .findFor(genericType.getType())
                .orElseThrow(() -> new IllegalStateException("Missing collector for " + genericType));

        M map = Stream.of(entry(1L, "foo"), entry(2L, "bar")).collect(collector);
        assertThat(map)
                .isInstanceOf(erasedType)
                .containsAllEntriesOf(ImmutableMultimap.of(1L, "foo", 2L, "bar"));
    }
}
