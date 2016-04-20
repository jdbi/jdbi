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

import static com.google.common.collect.Iterables.toArray;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Comparator;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import org.jdbi.v3.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestGuavaCollectors {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugins();

    private Collection<Integer> expected;

    @Before
    public void addData() {
        ImmutableList.Builder<Integer> expected = ImmutableList.builder();
        for (int i = 10; i > 0; i--) {
            db.getSharedHandle().execute("insert into something(name, intValue) values (?, ?)", Integer.toString(i), i);
            expected.add(i);
        }
        this.expected = expected.build();
    }

    @Test
    public void immutableList() {
        ImmutableList<Integer> list = db.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toImmutableList());

        assertEquals(expected.size(), list.size());
        assertThat(list, hasItems(toArray(expected, Integer.class)));
    }

    @Test
    public void immutableSet() {
        ImmutableSet<Integer> list = db.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toImmutableSet());

        assertEquals(expected.size(), list.size());
        assertThat(list, hasItems(toArray(expected, Integer.class)));
    }

    @Test
    public void immutableSortedSet() {
        ImmutableSortedSet<Integer> list = db.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toImmutableSortedSet());

        assertEquals(expected.size(), list.size());
        assertThat(list, hasItems(toArray(expected, Integer.class)));
    }

    @Test
    public void immutableSortedSetWithComparator() {
        Comparator<Integer> comparator = Comparator.<Integer>naturalOrder().reversed();
        ImmutableSortedSet<Integer> list = db.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toImmutableSortedSet(comparator));

        assertEquals(expected.size(), list.size());
        assertThat(list, hasItems(toArray(expected, Integer.class)));
    }

    @Test
    public void optionalPresent() {
        Optional<Integer> shouldBePresent = db.getSharedHandle().createQuery("select intValue from something where intValue = 1")
                .mapTo(int.class)
                .collect(GuavaCollectors.toOptional());
        assertTrue(shouldBePresent.isPresent());
        assertThat(shouldBePresent.get(), equalTo(1));
    }

    @Test
    public void optionalAbsent() {
        Optional<Integer> shouldBeAbsent = db.getSharedHandle().createQuery("select intValue from something where intValue = 100")
                .mapTo(int.class)
                .collect(GuavaCollectors.toOptional());
        assertFalse(shouldBeAbsent.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void optionalMultiple() {
        db.getSharedHandle().createQuery("select intValue from something")
                .mapTo(int.class)
                .collect(GuavaCollectors.toOptional());
    }
}
