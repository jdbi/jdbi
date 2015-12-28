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

import static org.jdbi.v3.Types.getErasedType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;

import org.hamcrest.CoreMatchers;
import org.jdbi.v3.GenericType;
import org.jdbi.v3.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestGuavaCollectors {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugins();
    private final List<Integer> expected = new ArrayList<>();

    @Parameters(name="{0}")
    public static Object[][] data() {
        return new Object[][] {
            { new GenericType<ImmutableList<Integer>>() {}, f(ImmutableList::copyOf) },
            { new GenericType<ImmutableSet<Integer>>() {}, f(ImmutableSet::copyOf) },
            { new GenericType<ImmutableSortedSet<Integer>>() {}, f(ImmutableSortedSet::copyOf) }
        };
    }

    /* fool the type inferencing */
    private static Function<Collection<Integer>, Collection<Integer>> f(Function<Collection<Integer>, Collection<Integer>> f) {
        return f;
    }

    @Parameter(value=0)
    public GenericType<? extends Collection<Integer>> type;

    @Parameter(value=1)
    public Function<List<Integer>, List<Integer>> transformer;

    @Before
    public void addData() {
        for (int i = 10; i > 0; i--) {
            db.getSharedHandle().execute("insert into something(name, intValue) values (?, ?)", Integer.toString(i), i);
            expected.add(i);
        }
    }

    @Test
    public void test() throws Exception {
        Collection<Integer> collection = db.getSharedHandle().createQuery("select intValue from something")
            .mapTo(int.class)
            .collectInto(type);

        // Same type
        assertTrue(getErasedType(type.getType()).isInstance(collection));

        // Same elements, same order
        assertEquals(expected.size(), collection.size());
        assertThat(new ArrayList<>(collection), CoreMatchers.hasItems(
                Iterables.toArray(transformer.apply(expected), Object.class)));
    }
}
