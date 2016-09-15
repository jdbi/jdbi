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

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * Implements Iterator for unidentified arrays that have been cast to Object. Note that its elements will be returned as Object, primitives included (will be autoboxed).
 */
class ReflectionArrayIterator implements Iterator<Object>
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
}
