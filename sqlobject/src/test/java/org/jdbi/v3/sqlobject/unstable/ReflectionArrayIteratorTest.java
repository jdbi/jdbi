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
package org.jdbi.v3.sqlobject.unstable;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public class ReflectionArrayIteratorTest
{
    @Test
    public void testIntArray()
    {
        final Iterator it = new ReflectionArrayIterator(new int[]{1, 2, 3});

        Assert.assertTrue(it.hasNext());
        Assert.assertEquals(1, it.next());

        Assert.assertTrue(it.hasNext());
        Assert.assertEquals(2, it.next());

        Assert.assertTrue(it.hasNext());
        Assert.assertEquals(3, it.next());

        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void testEmptyArray()
    {
        final Iterator it = new ReflectionArrayIterator(new int[]{});

        Assert.assertFalse(it.hasNext());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testOverflow()
    {
        final Iterator it = new ReflectionArrayIterator(new int[]{1});

        Assert.assertTrue(it.hasNext());
        Assert.assertEquals(1, it.next());

        it.next();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testOverflowOnEmpty()
    {
        final Iterator it = new ReflectionArrayIterator(new int[]{});

        it.next();
    }
}
