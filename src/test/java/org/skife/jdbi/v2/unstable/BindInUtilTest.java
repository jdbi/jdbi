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
package org.skife.jdbi.v2.unstable;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class BindInUtilTest
{
    @Test(expected = IllegalArgumentException.class)
    public void testObjectToIterator()
    {
        BindIn.Util.toIterator(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOtherClassToIterator()
    {
        BindIn.Util.toIterator("bla"); // or any other kind of object that isn't a java.lang.Object
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveToIterator()
    {
        BindIn.Util.toIterator(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIteratorToIterator()
    {
        BindIn.Util.toIterator(new ArrayList<Object>().iterator());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullToIterator()
    {
        BindIn.Util.toIterator(null);
    }

    @Test
    public void testEmptyArrayToIterator()
    {
        final Object[] out = toArray(BindIn.Util.toIterator(new int[]{}));

        Assert.assertEquals(0, out.length);
    }

    @Test
    public void testEmptyListToIterator()
    {
        final Object[] out = toArray(BindIn.Util.toIterator(new ArrayList<Integer>()));

        Assert.assertEquals(0, out.length);
    }

    @Test
    public void testListToIterator()
    {
        final List<String> in = new ArrayList<String>(2);
        in.add("1");
        in.add("2");

        final Object[] out = toArray(BindIn.Util.toIterator(in));

        Assert.assertEquals(in.size(), out.length);
        Assert.assertThat(Arrays.asList(out), CoreMatchers.hasItems((Object) "1", "2"));
    }

    @Test
    public void testSetToIterator()
    {
        final Set<String> in = new HashSet<String>(2);
        in.add("1");
        in.add("2");

        final Object[] out = toArray(BindIn.Util.toIterator(in));

        Assert.assertEquals(in.size(), out.length);
        Assert.assertThat(Arrays.asList(out), CoreMatchers.hasItems((Object) "1", "2"));
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

        final Object[] out = toArray(BindIn.Util.toIterator(in));

        Assert.assertEquals(2, out.length);
        Assert.assertThat(Arrays.asList(out), CoreMatchers.hasItems((Object) "1", "2"));
    }

    @Test
    public void testStringArrayToIterator()
    {
        final String[] in = new String[]{"1", "2"};

        final Object[] out = toArray(BindIn.Util.toIterator(in));

        Assert.assertEquals(in.length, out.length);
        Assert.assertThat(Arrays.asList(out), CoreMatchers.hasItems((Object) "1", "2"));
    }

    @Test
    public void testPrimitiveArrayToIterator()
    {
        final int[] in = new int[]{1, 2};

        final Object[] out = toArray(BindIn.Util.toIterator(in));

        Assert.assertEquals(in.length, out.length);
        Assert.assertThat(Arrays.asList(out), CoreMatchers.hasItems((Object) 1, 2));
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
        Assert.assertEquals(3, BindIn.Util.size(new int[]{1, 2, 3}));
    }

    @Test
    public void testSizeEmptyPrimitiveArray()
    {
        Assert.assertEquals(0, BindIn.Util.size(new int[]{}));
    }

    @Test
    public void testSizeObjectArray()
    {
        Assert.assertEquals(3, BindIn.Util.size(new Object[]{"1", "2", "3"}));
    }

    @Test
    public void testSizeEmptyObjectArray()
    {
        Assert.assertEquals(0, BindIn.Util.size(new Object[]{}));
    }

    @Test
    public void testSizeList()
    {
        final List<String> in = new ArrayList<String>();
        in.add("1");
        in.add("2");
        in.add("3");

        Assert.assertEquals(3, BindIn.Util.size(in));
    }

    @Test
    public void testSizeEmptyList()
    {
        Assert.assertEquals(0, BindIn.Util.size(new ArrayList<String>()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizeObject()
    {
        BindIn.Util.size(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizePrimitive()
    {
        BindIn.Util.size(5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizeNull()
    {
        BindIn.Util.size(null);
    }
}
