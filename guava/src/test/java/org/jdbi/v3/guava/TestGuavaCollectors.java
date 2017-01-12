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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestGuavaCollectors {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugins();

    private Collection<Integer> expected;

    @Before
    public void addData() {
        ImmutableList.Builder<Integer> expected = ImmutableList.builder();
        for (int i = 0; i < 10; i++) {
            dbRule.getSharedHandle().execute("insert into something(name, intValue) values (?, ?)", Integer.toString(i), i);
            expected.add(i);
        }
        this.expected = expected.build();
    }

    @Test
    public void immutableList() {
        ImmutableList<Integer> list = dbRule.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toImmutableList());

        assertThat(list).containsOnlyElementsOf(expected);
    }

    @Test
    public void immutableSet() {
        ImmutableSet<Integer> set = dbRule.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toImmutableSet());

        assertThat(set).containsOnlyElementsOf(expected);
    }

    @Test
    public void immutableSortedSet() {
        ImmutableSortedSet<Integer> set = dbRule.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toImmutableSortedSet());

        assertThat(set).containsExactlyElementsOf(expected);
    }

    @Test
    public void immutableSortedSetWithComparator() {
        Comparator<Integer> comparator = Comparator.<Integer>naturalOrder().reversed();
        ImmutableSortedSet<Integer> set = dbRule.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toImmutableSortedSet(comparator));

        assertThat(set).containsExactlyElementsOf(expected.stream()
                .sorted(comparator)
                .collect(Collectors.toList()));
    }

    @Test
    public void optionalPresent() {
        Optional<Integer> shouldBePresent = dbRule.getSharedHandle().createQuery("select intValue from something where intValue = 1")
                .mapTo(int.class)
                .collect(GuavaCollectors.toOptional());
        assertThat(shouldBePresent).contains(1);
    }

    @Test
    public void optionalAbsent() {
        Optional<Integer> shouldBeAbsent = dbRule.getSharedHandle().createQuery("select intValue from something where intValue = 100")
                .mapTo(int.class)
                .collect(GuavaCollectors.toOptional());
        assertThat(shouldBeAbsent).isAbsent();
    }

    @Test(expected = IllegalStateException.class)
    public void optionalMultiple() {
        dbRule.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toOptional());
    }
}
