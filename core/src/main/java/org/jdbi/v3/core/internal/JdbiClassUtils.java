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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.statement.UnableToCreateStatementException;

import static java.lang.String.format;

import static org.jdbi.v3.core.internal.exceptions.Sneaky.throwAnyway;

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

    private static final Class<?>[] NO_PARAMS = new Class[0];

    /**
     * Create a new instance for a type with a no-args constructor.
     *
     * @param type The type to create.
     * @return An instance of the type, created by the no-args constructor.
     * @throws UnableToCreateStatementException If the type could not be instantiated.
     */
    public static <T> T checkedCreateInstance(Class<T> type) {
        return checkedCreateInstance(type, NO_PARAMS);
    }

    /**
     * Create a new instance for a type.
     *
     * @param type       The type to create.
     * @param parameters The type parameters for the constructor.
     * @param values     Type values for the constructor. The number of values must match the number of type parameters.
     * @return An instance of the type.
     */
    public static <T> T checkedCreateInstance(Class<T> type,
            Class<?>[] parameters,
            Object... values) {

        try {
            var methodHandle = MethodHandles.lookup().findConstructor(type, MethodType.methodType(void.class, parameters));
            methodHandle = methodHandle.asType(MethodType.methodType(type, parameters));
            return (T) methodHandle.invokeWithArguments(values);
        } catch (Throwable t) {
            throw throwAnyway(t);
        }
    }

    private static final ConcurrentMap<Class<?>, MethodHandle> METHOD_HANDLE_CACHE = new ConcurrentHashMap<>();

    /**
     * Inspect a type, find a matching constructor and return an instance. The method tries to match as many parameters as possible
     * to the available constructors, cutting off parameters from the end one-by-one until a matching constructor is found.
     *
     * @param type    The type that should be instantiated
     * @param types   Array of parameter types
     * @param invoker An implementation of a method invoker to create an instance of the type
     * @return An instance of the type created by the first constructor found when looking up
     */
    @SuppressWarnings("unchecked")
    public static <T> T findConstructorAndCreateInstance(Class<T> type,
            Class<?>[] types,
            MethodHandleInvoker invoker) {
        try {
            var ctorHandle = METHOD_HANDLE_CACHE.computeIfAbsent(type, t -> findCtorMethodHandleForParameters(t, types));
            return (T) invoker.createInstance(ctorHandle);
        } catch (Throwable t) {
            throw throwAnyway(t);
        }
    }

    /**
     * Inspect a type and find a matching constructor. The method tries to match as many parameters as possible
     * to the available constructors, cutting off parameters from the end one-by-one until a matching constructor is found.
     *
     * @param type  The type that should be instantiated.
     * @param types Array of parameter types.
     * @return a handle to the found constructor, with the argument list adjusted to drop excess parameters
     */
    @SuppressWarnings("unchecked")
    public static <T> MethodHandleHolder<T> findConstructor(Class<T> type, Class<?>... types) {

        var ctorHandle = findCtorMethodHandleForParameters(type, types);
        return invoker -> {
            try {
                return (T) invoker.createInstance(ctorHandle);
            } catch (Throwable t) {
                throw throwAnyway(t);
            }
        };
    }

    private static MethodHandle findCtorMethodHandleForParameters(Class<?> type, Class<?>... types) {
        Deque<Throwable> suppressedThrowables = new ArrayDeque<>();

        var constructors = type.getConstructors();

        for (int argCount = types.length; argCount >= 0; argCount--) {
            tryNextConstructor:
            for (var constructor : constructors) {
                if (constructor.getParameterCount() != argCount) {
                    continue;
                }

                for (int i = 0; i < argCount; i++) {
                    if (!constructor.getParameterTypes()[i].isAssignableFrom(types[i])) {
                        continue tryNextConstructor;
                    }
                }

                try {
                    var methodHandle = MethodHandles.lookup().unreflectConstructor(constructor);
                    if (argCount < types.length) {
                        // the method handle will always be called with all possible arguments.
                        // Using dropArguments will remove any argument that the method handle not
                        // need (because the actual c'tor takes less arguments). This allows calling invokeExact because
                        // the exposed method handle will always take all arguments.
                        methodHandle = MethodHandles.dropArguments(methodHandle, argCount,
                                Arrays.copyOfRange(types, argCount, types.length));
                    }
                    return methodHandle.asType(methodHandle.type().changeReturnType(Object.class));
                } catch (IllegalAccessException e) {
                    suppressedThrowables.add(e);
                }
            }
        }

        var failure = new NoSuchMethodException(format("No constructor for class '%s', loosely matching arguments %s", type.getName(), Arrays.toString(types)));
        suppressedThrowables.forEach(failure::addSuppressed);

        // return a method handle that will throw the no such method exception on invocation, thus deferring
        // the actual exception until invocation time.
        return MethodHandles.dropArguments(
                MethodHandles.insertArguments(MethodHandles.throwException(Object.class, Exception.class), 0, failure),
                0, types);
    }

    @FunctionalInterface
    public interface MethodHandleHolder<T> {
        T invoke(MethodHandleInvoker invoker);
    }

    @FunctionalInterface
    public interface MethodHandleInvoker {
        Object createInstance(MethodHandle handle) throws Throwable;
    }

    public static final class MethodKey {
        public final String name;
        public final MethodType type;

        public MethodKey(String name, MethodType type) {
            this.name = name;
            this.type = type;
        }

        public static MethodKey methodKey(Method method) {
            return new MethodKey(
                    method.getName(),
                    MethodType.methodType(
                            method.getReturnType(),
                            method.getParameterTypes()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodKey)) {
                return false;
            }
            MethodKey castObj = (MethodKey) obj;
            return name.equals(castObj.name) && type.equals(castObj.type);
        }

        @Override
        public String toString() {
            return "MethodKey[" + name + "(" + type.parameterList().stream()
                    .map(Class::toString).collect(Collectors.joining(",")) + ")]";
        }
    }
}
