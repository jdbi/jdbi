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
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InaccessibleObjectException;
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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
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

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

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
            var ctorHandle = METHOD_HANDLE_CACHE.computeIfAbsent(type, t -> findCtorMethodHandleForParameters(t, t.getConstructors(), LOOKUP::unreflectConstructor, types));
            return (T) invoker.createInstance(ctorHandle);
        } catch (Throwable t) {
            throw throwAnyway(t);
        }
    }

    /**
     * Inspect a type and find a matching constructor. The method tries to match as many parameters as possible
     * to the available constructors, cutting off parameters from the end one-by-one until a matching constructor is found.
     * <p>
     * This method does not apply the {@link ReflectionMappers} accessible object strategy. Use it only for types that
     * must be public, such as Jdbi configuration classes. For user data types, use
     * {@link #findConstructor(ConfigRegistry, Class, Class[])}.
     *
     * @param type  The type that should be instantiated.
     * @param types Array of parameter types.
     * @return a handle to the found constructor, with the argument list adjusted to drop excess parameters
     */
    public static <T> MethodHandleHolder<T> findConstructor(Class<T> type, Class<?>... types) {
        return holderFor(findCtorMethodHandleForParameters(type, type.getConstructors(), LOOKUP::unreflectConstructor, types));
    }

    /**
     * Inspect a type and find a matching constructor. The method tries to match as many parameters as possible
     * to the declared constructors, cutting off parameters from the end one-by-one until a matching constructor is found.
     * Constructors that are not accessible to Jdbi are made accessible according to the
     * {@link ReflectionMappers#setAccessibleObjectStrategy(java.util.function.Consumer) accessible object strategy}.
     *
     * @param config The configuration that provides the accessible object strategy.
     * @param type   The type that should be instantiated.
     * @param types  Array of parameter types.
     * @return a handle to the found constructor, with the argument list adjusted to drop excess parameters
     */
    public static <T> MethodHandleHolder<T> findConstructor(ConfigRegistry config, Class<T> type, Class<?>... types) {
        return holderFor(findCtorMethodHandleForParameters(type, type.getDeclaredConstructors(),
                constructor -> accessibleHandle(config, constructor, LOOKUP::unreflectConstructor), types));
    }

    /**
     * Create a method handle for a method of a user type. If the method is not accessible to Jdbi, it is made accessible
     * according to the {@link ReflectionMappers#setAccessibleObjectStrategy(java.util.function.Consumer) accessible object strategy}.
     *
     * @param config The configuration that provides the accessible object strategy.
     * @param method The method.
     * @return A method handle for the method.
     * @throws IllegalArgumentException If the method is not accessible.
     */
    public static MethodHandle unreflect(ConfigRegistry config, Method method) {
        try {
            return accessibleHandle(config, method, LOOKUP::unreflect);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Create a method handle for a constructor of a user type. If the constructor is not accessible to Jdbi, it is made accessible
     * according to the {@link ReflectionMappers#setAccessibleObjectStrategy(java.util.function.Consumer) accessible object strategy}.
     *
     * @param config      The configuration that provides the accessible object strategy.
     * @param constructor The constructor.
     * @return A method handle for the constructor.
     * @throws IllegalArgumentException If the constructor is not accessible.
     */
    public static MethodHandle unreflectConstructor(ConfigRegistry config, Constructor<?> constructor) {
        try {
            return accessibleHandle(config, constructor, LOOKUP::unreflectConstructor);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    // The strategy gets a copy of the member: the original can be shared, e.g. through the JDK
    // Introspector cache, and an accessible flag set on it would reach every Jdbi instance in the JVM.
    private static <M extends Executable> MethodHandle accessibleHandle(ConfigRegistry config,
            M member,
            Unreflector<M> unreflector) throws IllegalAccessException {
        try {
            return unreflector.unreflect(member);
        } catch (IllegalAccessException e) {
            try {
                return unreflector.unreflect(config.get(ReflectionMappers.class).makeAccessible(copyOf(member)));
            } catch (IllegalAccessException | InaccessibleObjectException | SecurityException e2) {
                var declaringClass = member.getDeclaringClass();
                var failure = new IllegalAccessException(format(
                        "Jdbi can not access %s. Make %s and the member public, or enable the ReflectionMappers accessible object strategy%s.",
                        member, declaringClass.getName(), openPackageHint(declaringClass)));
                failure.initCause(e2);
                failure.addSuppressed(e);
                throw failure;
            }
        }
    }

    private static String openPackageHint(Class<?> type) {
        if (!type.getModule().isNamed()) {
            return "";
        }
        final Module jdbi = JdbiClassUtils.class.getModule();
        return format(" and open package %s to %s",
                type.getPackageName(), jdbi.isNamed() ? "module " + jdbi.getName() : "ALL-UNNAMED");
    }

    // Each getDeclared* call returns new objects. Method.equals also compares the return type, so a
    // covariant bridge method matches only itself.
    @SuppressWarnings("unchecked")
    private static <M extends Executable> M copyOf(M member) {
        final Class<?> declaringClass = member.getDeclaringClass();
        final Executable[] candidates = member instanceof Method
                ? declaringClass.getDeclaredMethods()
                : declaringClass.getDeclaredConstructors();
        return (M) Arrays.stream(candidates)
                .filter(member::equals)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(format("can't find %s on %s", member, declaringClass.getName())));
    }

    private static <T> MethodHandleHolder<T> holderFor(MethodHandle ctorHandle) {
        return invoker -> {
            try {
                @SuppressWarnings("unchecked")
                var instance = (T) invoker.createInstance(ctorHandle);
                return instance;
            } catch (Throwable t) {
                throw throwAnyway(t);
            }
        };
    }

    private static MethodHandle findCtorMethodHandleForParameters(Class<?> type,
            Constructor<?>[] constructors,
            Unreflector<Constructor<?>> unreflector,
            Class<?>... types) {
        Deque<Throwable> accessFailures = new ArrayDeque<>();

        for (int argCount = types.length; argCount >= 0; argCount--) {
            for (var constructor : constructors) {
                if (!parametersMatch(constructor, argCount, types)) {
                    continue;
                }

                try {
                    var methodHandle = unreflector.unreflect(constructor);
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
                    accessFailures.add(e);
                }
            }
        }

        var failure = new NoSuchMethodException(format("No constructor for class '%s', loosely matching arguments %s", type.getName(), Arrays.toString(types)));
        if (!accessFailures.isEmpty()) {
            failure.initCause(accessFailures.removeFirst());
        }
        accessFailures.forEach(failure::addSuppressed);

        // return a method handle that will throw the no such method exception on invocation, thus deferring
        // the actual exception until invocation time.
        return MethodHandles.dropArguments(
                MethodHandles.insertArguments(MethodHandles.throwException(Object.class, Exception.class), 0, failure),
                0, types);
    }

    private static boolean parametersMatch(Constructor<?> constructor, int argCount, Class<?>[] types) {
        if (constructor.getParameterCount() != argCount) {
            return false;
        }
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < argCount; i++) {
            if (!parameterTypes[i].isAssignableFrom(types[i])) {
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    private interface Unreflector<M> {
        MethodHandle unreflect(M member) throws IllegalAccessException;
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
            if (obj instanceof MethodKey other) {
                return name.equals(other.name) && type.equals(other.type);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "MethodKey[" + name + "(" + type.parameterList().stream()
                    .map(Class::toString).collect(Collectors.joining(",")) + ")]";
        }
    }
}
