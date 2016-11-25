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
package org.jdbi.v3.sqlobject.customizers;

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
        BindList.Util.toIterator(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOtherClassToIterator()
    {
        BindList.Util.toIterator("bla"); // or any other kind of object that isn't a java.lang.Object
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveToIterator()
    {
        BindList.Util.toIterator(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIteratorToIterator()
    {
        BindList.Util.toIterator(new ArrayList<Object>().iterator());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullToIterator()
    {
        BindList.Util.toIterator(null);
    }

    @Test
    public void testEmptyArrayToIterator()
    {
        final Object[] out = toArray(BindList.Util.toIterator(new int[]{}));
        assertThat(out).isEmpty();
    }

    @Test
    public void testEmptyListToIterator()
    {
        final Object[] out = toArray(BindList.Util.toIterator(new ArrayList<Integer>()));
        assertThat(out).isEmpty();
    }

    @Test
    public void testListToIterator()
    {
        final List<String> in = new ArrayList<String>(2);
        in.add("1");
        in.add("2");

        final Object[] out = toArray(BindList.Util.toIterator(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testSetToIterator()
    {
        final Set<String> in = new HashSet<String>(2);
        in.add("1");
        in.add("2");

        final Object[] out = toArray(BindList.Util.toIterator(in));

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

        final Object[] out = toArray(BindList.Util.toIterator(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testStringArrayToIterator()
    {
        final String[] in = new String[]{"1", "2"};

        final Object[] out = toArray(BindList.Util.toIterator(in));

        assertThat(out).containsExactly("1", "2");
    }

    @Test
    public void testPrimitiveArrayToIterator()
    {
        final int[] in = new int[]{1, 2};

        final Object[] out = toArray(BindList.Util.toIterator(in));

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
    public void testSizePrimitiveArray()
    {
        assertThat(BindList.Util.size(new int[]{1, 2, 3})).isEqualTo(3);
    }

    @Test
    public void testSizeEmptyPrimitiveArray()
    {
        assertThat(BindList.Util.size(new int[]{})).isEqualTo(0);
    }

    @Test
    public void testSizeObjectArray()
    {
        assertThat(BindList.Util.size(new Object[]{"1", "2", "3"})).isEqualTo(3);
    }

    @Test
    public void testSizeEmptyObjectArray()
    {
        assertThat(BindList.Util.size(new Object[]{})).isEqualTo(0);
    }

    @Test
    public void testSizeList()
    {
        final List<String> in = new ArrayList<String>();
        in.add("1");
        in.add("2");
        in.add("3");

        assertThat(BindList.Util.size(in)).isEqualTo(3);
    }

    @Test
    public void testSizeEmptyList()
    {
        assertThat(BindList.Util.size(new ArrayList<String>())).isEqualTo(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizeObject()
    {
        BindList.Util.size(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizePrimitive()
    {
        BindList.Util.size(5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizeNull()
    {
        BindList.Util.size(null);
    }
}
