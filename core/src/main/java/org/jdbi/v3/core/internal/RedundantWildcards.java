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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Compares types while ignoring each wildcard {@code ? extends B} whose bound {@code B} has no proper subtypes, so that
 * {@code List<? extends String>} and {@code List<String>} are equivalent. Both types admit exactly the same values.
 * <p>
 * A bound has no proper subtypes when it is a final class, an array of primitives or of such a type, or a parameterized
 * final class whose type arguments and owner type arguments contain no other wildcard.
 * <p>
 * The Kotlin compiler always writes the wildcard into the argument of a supertype, such as an anonymous
 * {@code GenericType} subclass or a class that implements {@code ColumnMapper<List<String>>}. It drops the wildcard from
 * a parameter for a final element type that is not an enum, and never writes it into a return type.
 * See <a href="https://youtrack.jetbrains.com/issue/KT-77120">KT-77120</a>.
 */
public final class RedundantWildcards {
    private RedundantWildcards() {
        throw new UtilityClassException();
    }

    /**
     * Returns a predicate that tests whether a type is equivalent to {@code type}.
     *
     * @param type the type to match
     * @return the predicate
     */
    public static Predicate<Type> matcher(Type type) {
        return candidate -> equivalent(type, candidate);
    }

    static boolean equivalent(Type a, Type b) {
        if (a.equals(b)) {
            return true;
        }
        Type x = unwrap(a);
        Type y = unwrap(b);
        if (x instanceof ParameterizedType px && y instanceof ParameterizedType py) {
            return px.getRawType().equals(py.getRawType())
                && equivalentOrBothNull(px.getOwnerType(), py.getOwnerType())
                && allEquivalent(px.getActualTypeArguments(), py.getActualTypeArguments());
        }
        if (x instanceof WildcardType wx && y instanceof WildcardType wy) {
            return allEquivalent(wx.getUpperBounds(), wy.getUpperBounds())
                && allEquivalent(wx.getLowerBounds(), wy.getLowerBounds());
        }
        if (x instanceof GenericArrayType gx && y instanceof GenericArrayType gy) {
            return equivalent(gx.getGenericComponentType(), gy.getGenericComponentType());
        }
        return x.equals(y);
    }

    private static boolean equivalentOrBothNull(Type a, Type b) {
        return a == null ? b == null : b != null && equivalent(a, b);
    }

    private static boolean allEquivalent(Type[] a, Type[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!equivalent(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    private static Type unwrap(Type type) {
        return type instanceof WildcardType wildcard && isRedundant(wildcard) ? wildcard.getUpperBounds()[0] : type;
    }

    private static boolean isRedundant(WildcardType wildcard) {
        Type[] upperBounds = wildcard.getUpperBounds();
        return wildcard.getLowerBounds().length == 0 && upperBounds.length == 1 && hasNoProperSubtypes(upperBounds[0]);
    }

    private static boolean hasNoProperSubtypes(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.isArray()
                ? clazz.getComponentType().isPrimitive() || hasNoProperSubtypes(clazz.getComponentType())
                : Modifier.isFinal(clazz.getModifiers());
        }
        if (type instanceof ParameterizedType parameterized) {
            return hasNoProperSubtypes(parameterized.getRawType()) && hasOnlyRedundantWildcards(parameterized);
        }
        return type instanceof GenericArrayType array && hasNoProperSubtypes(array.getGenericComponentType());
    }

    private static boolean hasOnlyRedundantWildcards(ParameterizedType parameterized) {
        boolean arguments = Stream.of(parameterized.getActualTypeArguments())
            .allMatch(argument -> !(argument instanceof WildcardType wildcard) || isRedundant(wildcard));
        return arguments && (!(parameterized.getOwnerType() instanceof ParameterizedType owner) || hasOnlyRedundantWildcards(owner));
    }
}
