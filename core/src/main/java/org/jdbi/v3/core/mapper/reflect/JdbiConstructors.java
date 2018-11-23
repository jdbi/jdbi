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
package org.jdbi.v3.core.mapper.reflect;

import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for {@link JdbiConstructor} annotation.
 */
public class JdbiConstructors {
    private JdbiConstructors() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Find an invokable instance factory, such as a constructor or a static factory method.
     * Prefer an {@link JdbiConstructor} annotated constructor or static factory method if
     * one is present. Throws if multiple or zero candidates are found.
     *
     * @param <T>  the type to inspect
     * @param type the type to inspect
     * @return the preferred constructor or static factory method
     */
    static <T> InstanceFactory<T> findFactoryFor(Class<T> type) {
        @SuppressWarnings("unchecked")
        final Constructor<T>[] constructors = (Constructor<T>[]) type.getDeclaredConstructors();

        List<Constructor<T>> explicitConstructors = Stream.of(constructors)
            .filter(constructor -> constructor.isAnnotationPresent(JdbiConstructor.class))
            .collect(Collectors.toList());

        List<Method> explicitFactoryMethods = Stream.of(type.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(JdbiConstructor.class))
            .collect(Collectors.toList());

        if (explicitConstructors.size() + explicitFactoryMethods.size() > 1) {
            throw new IllegalArgumentException(type + " may have at most one constructor or static factory method annotated @JdbiConstructor");
        }
        if (explicitConstructors.size() == 1) {
            return new ConstructorInstanceFactory<>(explicitConstructors.get(0));
        }
        if (explicitFactoryMethods.size() == 1) {
            return new StaticMethodInstanceFactory<>(type, explicitFactoryMethods.get(0));
        }

        return new ConstructorInstanceFactory<>(findImplicitConstructorFor(type));
    }

    /**
     * Find an invokable constructor.  Prefer an {@link JdbiConstructor} annotated
     * one if present.  Throws if multiple or zero candidates are found.
     *
     * @param <T>  the type to inspect
     * @param type the type to inspect
     * @return the preferred constructor
     */
    public static <T> Constructor<T> findConstructorFor(Class<T> type) {
        @SuppressWarnings("unchecked")
        final Constructor<T>[] constructors = (Constructor<T>[]) type.getDeclaredConstructors();

        List<Constructor<T>> annotatedConstructors = Stream.of(constructors)
                .filter(c -> c.isAnnotationPresent(JdbiConstructor.class))
                .collect(Collectors.toList());

        if (annotatedConstructors.size() > 1) {
            throw new IllegalArgumentException(type + " may have at most one constructor annotated @JdbiConstructor");
        } else if (annotatedConstructors.size() == 1) {
            return annotatedConstructors.get(0);
        }

        return findImplicitConstructorFor(type);
    }

    private static <T> Constructor<T> findImplicitConstructorFor(Class<T> type) {
        @SuppressWarnings("unchecked")
        final Constructor<T>[] constructors = (Constructor<T>[]) type.getDeclaredConstructors();

        List<Constructor<T>> annotatedConstructors = Stream.of(constructors)
                .filter(c -> c.isAnnotationPresent(ConstructorProperties.class))
                .collect(Collectors.toList());

        if (annotatedConstructors.size() > 1) {
            throw new IllegalArgumentException(type + " may have at most one constructor annotated @ConstructorProperties");
        } else if (annotatedConstructors.size() == 1) {
            return annotatedConstructors.get(0);
        }

        if (constructors.length != 1) {
            throw new IllegalArgumentException(type + " must have exactly one constructor, or specify it with @JdbiConstructor or @ConstructorProperties");
        }
        return constructors[0];
    }
}
