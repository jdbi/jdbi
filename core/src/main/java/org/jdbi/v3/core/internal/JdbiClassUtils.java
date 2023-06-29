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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.internal.exceptions.CheckedCallable;
import org.jdbi.v3.core.internal.exceptions.Sneaky;

import static java.lang.String.format;

/**
 * Helper class for various internal reflection operations.
 */
public final class JdbiClassUtils {
    public static final Method EQUALS_METHOD;
    public static final Method HASHCODE_METHOD;
    public static final Method TOSTRING_METHOD;

    static {
        EQUALS_METHOD = JdbiClassUtils.methodLookup(Object.class, "equals", Object.class);
        HASHCODE_METHOD = JdbiClassUtils.methodLookup(Object.class, "hashCode");
        TOSTRING_METHOD = JdbiClassUtils.methodLookup(Object.class, "toString");
    }

    private JdbiClassUtils() {
        throw new UtilityClassException();
    }

    /**
     * Returns true if a specific class can be loaded.
     * @param klass The class
     * @return True if it can be loaded, false otherwise
     */
    public static boolean isPresent(String klass) {
        try {
            Class.forName(klass);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    /**
     * Lookup a specific method name related to a class. This helper tries {@link Class#getMethod(String, Class[])} first, then
     * falls back to {@link Class#getDeclaredMethod(String, Class[])}.
     * @param klass A class
     * @param methodName A method name
     * @param parameterTypes All parameter types for the method
     * @return A {@link Method} object
     * @throws IllegalStateException If the method could not be found
     */
    public static Method methodLookup(Class<?> klass, String methodName, Class<?>... parameterTypes) {
        try {
            return klass.getMethod(methodName, parameterTypes);
        } catch (ReflectiveOperationException | SecurityException e) {
            try {
                return klass.getDeclaredMethod(methodName, parameterTypes);
            } catch (ReflectiveOperationException | SecurityException e2) {
                e.addSuppressed(e2);
            }
            throw new IllegalStateException(format("can't find %s#%s%s", klass.getName(), methodName, Arrays.asList(parameterTypes)), e);
        }
    }

    /**
     * Lookup a specific method name related to a class. This helper tries {@link Class#getMethod(String, Class[])} first, then
     * falls back to {@link Class#getDeclaredMethod(String, Class[])}.
     * @param klass A class
     * @param methodName A method name
     * @param parameterTypes All parameter types for the method
     * @return A {@link Method} object wrapped in an {@link Optional} if the method could be found, {@link Optional#empty()} otherwise
     */
    public static Optional<Method> safeMethodLookup(Class<?> klass, String methodName, Class<?>... parameterTypes) {
        try {
            return Optional.of(klass.getMethod(methodName, parameterTypes));
        } catch (ReflectiveOperationException | SecurityException ignored) {
            try {
                return Optional.of(klass.getDeclaredMethod(methodName, parameterTypes));
            } catch (ReflectiveOperationException | SecurityException ignored2) {
                return Optional.empty();
            }
        }
    }

    /**
     * Creates a new instance from a {@link CheckedCallable} instance if possible.
     *
     * @param creator A {@link CheckedCallable} instance which returns
     *                a new instance or throws any of the exceptions out
     *                of {@link Class#getConstructor(Class[])} or
     *                {@link java.lang.reflect.Constructor#newInstance(Object...)}
     * @return A new instance wrapped in an {@link Optional} or {@link Optional#empty()}
     * if a {@link ReflectiveOperationException} or {@link SecurityException} occured
     * @param <T> The type of the new instance
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    public static <T> Optional<T> createInstanceIfPossible(CheckedCallable<T> creator) {
        try {
            return Optional.of(creator.call());
        } catch (InvocationTargetException e) {
            throw Sneaky.throwAnyway(e.getCause());
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return Optional.empty();
        } catch (Throwable t) {
            throw Sneaky.throwAnyway(t);
        }
    }

    /**
     * Returns all supertypes to a given type.
     * @param type A type
     * @return A {@link Stream} of {@link Class} objects
     */
    public static Stream<Class<?>> superTypes(Class<?> type) {
        Class<?>[] interfaces = type.getInterfaces();
        // collect into a set to deduplicate the classes found.
        // this can happen if e.g. a extends b and both implement c.
        Set<Class<?>> result = Stream.concat(
                Arrays.stream(interfaces).flatMap(JdbiClassUtils::superTypes),
                Arrays.stream(interfaces))
                .collect(Collectors.toSet());

        return result.stream();
    }

    private static final Object[] NO_ARGS = new Object[0];

    /**
     * Safely move arguments passed from from a varargs call to a call that expects an array of objects.
     * @param args A list of objects. May be null or empty
     * @return Returns an Array of objects. If the input was null, returns an empty array, otherwise all arguments as an array
     */
    public static Object[] safeVarargs(Object... args) {
        return (args == null) ? NO_ARGS : args;
    }
}
