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

import java.util.Iterator;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ReflectionArrayIteratorTest {
    @Test
    public void testIntArray() {
        final Iterator<Object> it = new ReflectionArrayIterator(new int[]{1, 2, 3});

        Assertions.assertThat(it).containsExactly(1, 2, 3);
    }

    @Test
    public void testEmptyArray() {
        final Iterator<Object> it = new ReflectionArrayIterator(new int[]{});

        Assertions.assertThat(it).isEmpty();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testOverflow() {
        final Iterator<?> it = new ReflectionArrayIterator(new int[]{1});

        Assertions.assertThat(it.hasNext()).isTrue();
        Assertions.assertThat(it.next()).isEqualTo(1);

        it.next();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testOverflowOnEmpty() {
        final Iterator<?> it = new ReflectionArrayIterator(new int[]{});

        it.next();
    }
}
