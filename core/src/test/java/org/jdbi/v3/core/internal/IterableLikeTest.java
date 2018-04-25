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
package org.jdbi.v3.core.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IterableLikeTest {
    @Test
    public void testIntArray() {
        final Iterator<Object> it = IterableLike.of(new int[]{1, 2, 3});

        assertThat(it).containsExactly(1, 2, 3);
    }

    @Test
    public void testEmptyArray() {
        final Iterator<Object> it = IterableLike.of(new int[]{});

        assertThat(it).isEmpty();
    }

    @Test
    public void testOverflow() {
        final Iterator<?> it = IterableLike.of(new int[]{1});

        assertThat(it.hasNext()).isTrue();
        assertThat(it.next()).isEqualTo(1);

        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testOverflowOnEmpty() {
        final Iterator<?> it = IterableLike.of(new int[]{});

        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testObjectToIterator() {
        assertThatThrownBy(() -> IterableLike.of(new Object())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testOtherClassToIterator() {
        // or any other kind of object that isn't a java.lang.Object
        assertThatThrownBy(() -> IterableLike.of("bla")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testPrimitiveToIterator() {
        assertThatThrownBy(() -> IterableLike.of(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNullToIterator() {
        assertThatThrownBy(() -> IterableLike.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testEmptyArrayToIterator() {
        final Object[] out = toArray(IterableLike.of(new int[]{}));
        assertThat(out).isEmpty();
    }

    @Test
    public void testEmptyListToIterator() {
        final Object[] out = toArray(IterableLike.of(new ArrayList<Integer>()));
        assertThat(out).isEmpty();
    }

    @Test
    public void testListToIterator() {
        final List<String> in = new ArrayList<String>(2);
        in.add("1");
        in.add("2");

        final Object[] out = toArray(IterableLike.of(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testSetToIterator() {
        final Set<String> in = new HashSet<String>(2);
        in.add("1");
        in.add("2");

        final Object[] out = toArray(IterableLike.of(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testIterableToIterator() {
        final Iterable<String> in = new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                final List<String> tmp = new ArrayList<String>();
                tmp.add("1");
                tmp.add("2");

                return tmp.iterator();
            }
        };

        final Object[] out = toArray(IterableLike.of(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testStringArrayToIterator() {
        final String[] in = new String[]{"1", "2"};

        final Object[] out = toArray(IterableLike.of(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testPrimitiveArrayToIterator() {
        final int[] in = new int[]{1, 2};

        final Object[] out = toArray(IterableLike.of(in));

        assertThat(out).containsExactly(1, 2);
    }

    private static Object[] toArray(final Iterator<?> iterator) {
        final List<Object> out = new ArrayList<Object>();
        while (iterator.hasNext()) {
            out.add(iterator.next());
        }
        return out.toArray();
    }

    @Test
    public void testIsEmptyPrimitiveArray() {
        assertThat(IterableLike.isEmpty(new int[]{1, 2, 3})).isFalse();
    }

    @Test
    public void testIsEmptyEmptyPrimitiveArray() {
        assertThat(IterableLike.isEmpty(new int[]{})).isTrue();
    }

    @Test
    public void testIsEmptyObjectArray() {
        assertThat(IterableLike.isEmpty(new Object[]{"1", "2", "3"})).isFalse();
    }

    @Test
    public void testIsEmptyEmptyObjectArray() {
        assertThat(IterableLike.isEmpty(new Object[]{})).isTrue();
    }

    @Test
    public void testIsEmptyList() {
        final List<String> in = new ArrayList<String>();
        in.add("1");
        in.add("2");
        in.add("3");

        assertThat(IterableLike.isEmpty(in)).isFalse();
    }

    @Test
    public void testIsEmptyEmptyList() {
        assertThat(IterableLike.isEmpty(new ArrayList<String>())).isTrue();
    }

    @Test
    public void testIsEmptyObject() {
        assertThatThrownBy(() -> IterableLike.isEmpty(new Object())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIsEmptyPrimitive() {
        assertThatThrownBy(() -> IterableLike.isEmpty(5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIsEmptyNull() {
        assertThatThrownBy(() -> IterableLike.isEmpty(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
