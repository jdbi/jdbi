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
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.statement.UnableToCreateStatementException;

import static java.lang.String.format;

/**
 * Helper class for various internal reflection operations.
 */
public final class JdbiClassUtils {

    /** Constant for {@link Object#equals(Object)}. */
    public static final Method EQUALS_METHOD = methodLookup(Object.class, "equals", Object.class);

    /** Constant for {@link Object#hashCode()}. */
    public static final Method HASHCODE_METHOD = methodLookup(Object.class, "hashCode");

    /** Constant for {@link Object#toString()}. */
    public static final Method TOSTRING_METHOD = methodLookup(Object.class, "toString");


    private JdbiClassUtils() {
        throw new UtilityClassException();
    }

    /**
     * Returns true if a specific class can be loaded.
     *
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
     *
     * @param klass          A class
     * @param methodName     A method name
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
     *
     * @param klass          A class
     * @param methodName     A method name
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
     * Returns all supertypes to a given type.
     *
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
     *
     * @param args A list of objects. May be null or empty
     * @return Returns an Array of objects. If the input was null, returns an empty array, otherwise all arguments as an array
     */
    public static Object[] safeVarargs(Object... args) {
        return (args == null) ? NO_ARGS : args;
    }

    private static final BiFunction<Class<?>, Throwable, RuntimeException> UNABLE_TO_CREATE_STATEMENT_HANDLER = (t, e) ->
            new UnableToCreateStatementException(format("Unable to instantiate '%s':", t.getName()), e);

    private static final Class<?>[] NO_PARAMS = new Class[0];

    /**
     * Create a new instance for a type with a no-args constructor.
     *
     * @param type The type to create.
     * @return An instance of the type, created by the no-args constructor.
     * @throws UnableToCreateStatementException If the type could not be instantiated.
     */
    public static <T> T checkedCreateInstance(Class<T> type) {
        return checkedCreateInstance(type, NO_PARAMS, UNABLE_TO_CREATE_STATEMENT_HANDLER);
    }

    /**
     * Create a new instance for a type.
     *
     * @param type       The type to create.
     * @param parameters The type parameters for the constructor.
     * @param f          An error function. If creating the instance throws a Throwable, process it and return a runtime exception for reporting.
     * @param values     Type values for the constructor. The number of values must match the number of type parameters.
     * @return AN instance of the type.
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    private static <T> T checkedCreateInstance(Class<T> type, Class<?>[] parameters, BiFunction<Class<?>, Throwable, RuntimeException> f, Object... values) {
        try {
            return type.getConstructor(parameters).newInstance(values);
        } catch (InvocationTargetException e) {
            throw f.apply(type, e.getCause());
        } catch (ReflectiveOperationException | SecurityException e) {
            throw f.apply(type, e);
        }
    }

    private static final BiFunction<Class<?>, Throwable, RuntimeException> DEFAULT_EXCEPTION_HANDLER = (t, e) ->
             e instanceof RuntimeException
                     ? (RuntimeException) e
                     : new IllegalStateException(format("Unable to instantiate '%s':", t.getName()), e);

    /**
     * Inspect a type, find a matching constructor and return an instance. The method tries to match as many parameters as possible
     * to the available constructors, cutting off parameters from the end one-by-one until a matching constructor is found.
     *
     * @param type       The type that should be instantiated.
     * @param types      Array of parameter types.
     * @param parameters Parameters for the constructor.
     * @return An {@link Optional} wrapping the instantiated type or {@link Optional#empty()} if no matching constructor was found.
     */
    public static <T> T findConstructorAndCreateInstance(Class<T> type, Class<?>[] types, Object... parameters) {
        return findConstructorAndCreateInstance(type, types, DEFAULT_EXCEPTION_HANDLER, parameters);
    }

    @SuppressWarnings("PMD.PreserveStackTrace")
    private static <T> T findConstructorAndCreateInstance(Class<T> type,
            Class<?>[] types,
            BiFunction<Class<?>, Throwable, RuntimeException> f,
            Object... parameters) {
        if (types.length != parameters.length) {
            throw new IllegalArgumentException(format("%d types but %d parameters. Can not create instance!", types.length, parameters.length));
        }

        var constructors = type.getConstructors();

        for (int argCount = types.length; argCount >= 0; argCount--) {
            for (var constructor : constructors) {
                if (constructor.getParameterCount() != argCount) {
                    continue; // for
                }

                boolean match = true;
                for (int i = 0; i < argCount; i++) {
                    if (!constructor.getParameterTypes()[i].isAssignableFrom(types[i])) {
                        match = false;
                        break; // for(int i = 0; ...
                    }
                }
                if (!match) {
                    continue; // for(Constructor...
                }

                try {
                    return type.cast(constructor.newInstance(Arrays.copyOf(parameters, argCount)));
                } catch (InvocationTargetException e) {
                    throw f.apply(type, e.getCause());
                } catch (ReflectiveOperationException | SecurityException e) {
                    throw f.apply(type, e);
                }
            }
        }

        throw f.apply(type, null);
    }
}
