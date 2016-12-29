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

import org.jdbi.v3.core.internal.ReflectionArrayIterator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

final class BindListUtil {
    private BindListUtil() {
    }

    static Iterator<?> toIterator(final Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("cannot make iterator of null");
        }

        if (obj instanceof Iterable) {
            return ((Iterable<?>) obj).iterator();
        }

        if (obj.getClass().isArray()) {
            if (obj instanceof Object[]) {
                return Arrays.asList((Object[]) obj).iterator();
            } else {
                return ReflectionArrayIterator.of(obj);
            }
        }

        throw new IllegalArgumentException(getTypeWarning(obj.getClass()));
    }

    static boolean isEmpty(final Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("cannot determine emptiness of null");
        }

        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }

        if (obj instanceof Iterable) {
            return !((Iterable<?>) obj).iterator().hasNext();
        }

        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }

        throw new IllegalArgumentException(getTypeWarning(obj.getClass()));
    }

    private static String getTypeWarning(final Class<?> type) {
        return "argument must be one of the following: Iterable, or an array/varargs (primitive or complex type); was " + type.getName() + " instead";
    }
}
