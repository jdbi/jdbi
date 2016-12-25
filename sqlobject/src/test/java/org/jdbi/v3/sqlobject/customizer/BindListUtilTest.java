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
package org.jdbi.v3.sqlobject.customizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BindListUtilTest
{
    @Test(expected = IllegalArgumentException.class)
    public void testObjectToIterator()
    {
        BindListUtil.toIterator(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOtherClassToIterator()
    {
        BindListUtil.toIterator("bla"); // or any other kind of object that isn't a java.lang.Object
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveToIterator()
    {
        BindListUtil.toIterator(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIteratorToIterator()
    {
        BindListUtil.toIterator(new ArrayList<>().iterator());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullToIterator()
    {
        BindListUtil.toIterator(null);
    }

    @Test
    public void testEmptyArrayToIterator()
    {
        final Object[] out = toArray(BindListUtil.toIterator(new int[]{}));
        assertThat(out).isEmpty();
    }

    @Test
    public void testEmptyListToIterator()
    {
        final Object[] out = toArray(BindListUtil.toIterator(new ArrayList<Integer>()));
        assertThat(out).isEmpty();
    }

    @Test
    public void testListToIterator()
    {
        final List<String> in = new ArrayList<String>(2);
        in.add("1");
        in.add("2");

        final Object[] out = toArray(BindListUtil.toIterator(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testSetToIterator()
    {
        final Set<String> in = new HashSet<String>(2);
        in.add("1");
        in.add("2");

        final Object[] out = toArray(BindListUtil.toIterator(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testIterableToIterator()
    {
        final Iterable<String> in = new Iterable<String>()
        {
            @Override
            public Iterator<String> iterator()
            {
                final List<String> tmp = new ArrayList<String>();
                tmp.add("1");
                tmp.add("2");

                return tmp.iterator();
            }
        };

        final Object[] out = toArray(BindListUtil.toIterator(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testStringArrayToIterator()
    {
        final String[] in = new String[]{"1", "2"};

        final Object[] out = toArray(BindListUtil.toIterator(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testPrimitiveArrayToIterator()
    {
        final int[] in = new int[]{1, 2};

        final Object[] out = toArray(BindListUtil.toIterator(in));

        assertThat(out).containsExactly(1, 2);
    }

    private static Object[] toArray(final Iterator iterator)
    {
        final List<Object> out = new ArrayList<Object>();
        while (iterator.hasNext())
        {
            out.add(iterator.next());
        }
        return out.toArray();
    }

    @Test
    public void testIsEmptyPrimitiveArray()
    {
        assertThat(BindListUtil.isEmpty(new int[]{1, 2, 3})).isFalse();
    }

    @Test
    public void testIsEmptyEmptyPrimitiveArray()
    {
        assertThat(BindListUtil.isEmpty(new int[]{})).isTrue();
    }

    @Test
    public void testIsEmptyObjectArray()
    {
        assertThat(BindListUtil.isEmpty(new Object[]{"1", "2", "3"})).isFalse();
    }

    @Test
    public void testIsEmptyEmptyObjectArray()
    {
        assertThat(BindListUtil.isEmpty(new Object[]{})).isTrue();
    }

    @Test
    public void testIsEmptyList()
    {
        final List<String> in = new ArrayList<String>();
        in.add("1");
        in.add("2");
        in.add("3");

        assertThat(BindListUtil.isEmpty(in)).isFalse();
    }

    @Test
    public void testIsEmptyEmptyList()
    {
        assertThat(BindListUtil.isEmpty(new ArrayList<String>())).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsEmptyObject()
    {
        BindListUtil.isEmpty(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsEmptyPrimitive()
    {
        BindListUtil.isEmpty(5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsEmptyNull()
    {
        BindListUtil.isEmpty(null);
    }
}
