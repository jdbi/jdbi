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

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jdbi.v3.core.generic.GenericTypes;

/**
 * Implements Iterator methods for unidentified arrays and Iterable things that do not
 * have a more specific type than Object. Note that its elements will be returned as
 * Object, primitives included (will be autoboxed).
 */
public class IterableLike {
    private IterableLike() {
        throw new UtilityClassException();
    }

    /**
     * @param maybeIterable the object that might be iterable
     * @return whether {@code IterableLike} can iterate over the given object
     */
    public static boolean isIterable(Object maybeIterable) {
        return maybeIterable instanceof Iterator<?>
            || maybeIterable instanceof Iterable<?>
            || maybeIterable.getClass().isArray();
    }

    /**
     * Given an iterable object (which may be a iterator, iterable, primitive
     * or reference array), return an iterator over its (possibly boxed) elements.
     *
     * @param iterable the iterable-like
     * @return an iterator of the given array's elements
     */
    @SuppressWarnings("unchecked")
    public static Iterator<Object> of(Object iterable) {
        if (iterable == null) {
            throw new IllegalArgumentException("can't iterate null");
        }
        if (iterable instanceof Iterator<?>) {
            return (Iterator<Object>) iterable;
        } else if (iterable instanceof Iterable<?>) {
            return ((Iterable<Object>) iterable).iterator();
        }

        Class<?> klass = iterable.getClass();
        if (!klass.isArray()) {
            throw new IllegalArgumentException(getTypeWarning(klass));
        }

        if (klass.getComponentType().isPrimitive()) {
            return new PrimitiveArrayIterator(iterable);
        }
        return Arrays.asList((Object[]) iterable).iterator();
    }

    /** Given an iterable-like object, try to determine its static (i.e, without looking at contents) element type. */
    public static Optional<Type> elementTypeOf(Object iterable) {
        return elementTypeOf(iterable.getClass());
    }

    /** Given an iterable-like type, try to determine its static (i.e, without looking at contents) element type. */
    public static Optional<Type> elementTypeOf(Type type) {
        final Class<?> rawClass = GenericTypes.getErasedType(type);
        if (rawClass.isArray()) {
            return Optional.of(rawClass.getComponentType());
        } else if (Iterable.class.isAssignableFrom(rawClass)) {
            return GenericTypes.findGenericParameter(type, Iterable.class);
        } else if (Iterator.class.isAssignableFrom(rawClass)) {
            return GenericTypes.findGenericParameter(type, Iterator.class);
        } else { // not an iterable-like
            return Optional.empty();
        }
    }

    /**
     * Given an iterable object (which may be a iterator, iterable, primitive
     * or reference array), return a {@link Stream} over its (possibly boxed) elements.
     *
     * @return a stream of the given array's elements
     */
    public static Stream<Object> stream(Object iterable) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(of(iterable), Spliterator.ORDERED), false);
    }

    /**
     * Given an iterable object (which may be a iterator, iterable, primitive
     * or reference array), return an iterable over its (possibly boxed) elements.
     * @param iterable the iterable-like to create a real Iterable for
     * @return the created Iterable
     */
    public static Iterable<Object> iterable(Object iterable) {
        return () -> of(iterable);
    }

    /**
     * Attempt to determine if a iterable-like is empty, preferably without iterating.
     * @param obj the iterable-like to check for emptiness
     * @return emptiness to fill your heart
     */
    public static boolean isEmpty(final Object obj) {
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

    /**
     * Collect an iterable-like into a newly allocated ArrayList.
     * @param iterable the iterable-like to collect
     * @return a new list with the elements
     */
    public static List<Object> toList(Object iterable) {
        List<Object> result = new ArrayList<Object>();
        of(iterable).forEachRemaining(result::add);
        return result;
    }

    private static String getTypeWarning(final Class<?> type) {
        return "argument must be one of the following: Iterable, or an array/varargs (primitive or complex type); was " + type.getName() + " instead";
    }

    static class PrimitiveArrayIterator implements Iterator<Object> {
        private int index = 0;
        private final int size;
        private final Object arr;

        /**
         * @throws IllegalArgumentException if obj is not an array
         */
        PrimitiveArrayIterator(final Object obj) {
            size = Array.getLength(obj);
            arr = obj;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public Object next() {
            if (hasNext()) {
                return Array.get(arr, index++);
            } else {
                throw new NoSuchElementException("only " + size + " elements available");
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
