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
package org.jdbi.v3.core.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

import com.google.common.base.Verify;

/**
 * Implements Iterator for unidentified arrays that have been cast to Object. Note that its elements will be returned as Object, primitives included (will be autoboxed).
 */
public class ReflectionArrayIterator implements Iterator<Object>
{
    private int index = 0;
    private final int size;
    private final Object arr;

    /**
     * @throws IllegalArgumentException if obj is not an array
     */
    ReflectionArrayIterator(final Object obj)
    {
        size = Array.getLength(obj);
        arr = obj;
    }

    @Override
    public boolean hasNext()
    {
        return index < size;
    }

    @Override
    public Object next()
    {
        if (hasNext())
        {
            return Array.get(arr, index++);
        } else
        {
            throw new ArrayIndexOutOfBoundsException("only " + size + " elements available");
        }
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @return whether {@code ReflectionArrayIterator} can iterate over the given object
     */
    public static boolean isIterable(Object maybeIterable)
    {
        return  maybeIterable instanceof Iterator<?> ||
                maybeIterable instanceof Iterable<?> ||
                maybeIterable.getClass().isArray();
    }

    /**
     * Given an iterable object (which may be a iterator, iterable, primitive
     * or reference array), return an iterator over its (possibly boxed) elements.
     *
     * @return an iterator of the given array's elements
     */
    @SuppressWarnings("unchecked")
    public static Iterator<Object> of(Object iterable)
    {
        if (iterable instanceof Iterator<?>) {
            return (Iterator<Object>) iterable;
        }
        else if (iterable instanceof Iterable<?>) {
            return ((Iterable<Object>) iterable).iterator();
        }

        Class<? extends Object> klass = iterable.getClass();
        Verify.verify(klass.isArray(), "'%s' not an iterable", klass);
        if (klass.getComponentType().isPrimitive())
        {
            return new ReflectionArrayIterator(iterable);
        }
        return Arrays.asList((Object[])iterable).iterator();
    }
}
