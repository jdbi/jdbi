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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jdbi.v3.core.internal.exceptions.Unchecked;

public class AnnotationFactory {
    private AnnotationFactory() {}

    public static <T extends Annotation> T create(Class<T> annotationType) {
        return create(annotationType, Collections.emptyMap());
    }
    public static <T extends Annotation> T create(Class<T> annotationType, Map<String, ?> values) {
        Arrays.stream(annotationType.getDeclaredMethods())
            .filter(m -> m.getDefaultValue() == null)
            .filter(m -> !values.containsKey(m.getName()))
            .findAny()
            .ifPresent(m -> {
                throw new IllegalArgumentException(String.format(
                        "Cannot synthesize annotation @%s from %s.class because it has attribute " + m.getName() + " without a default or specified value",
                        annotationType.getSimpleName(),
                        annotationType.getSimpleName()));
        });

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?>[] interfaces = {annotationType};
        InvocationHandler invocationHandler = getInvocationHandler(annotationType, values);

        @SuppressWarnings("unchecked")
        T annotation = (T) Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);

        return annotation;
    }

    private static <T extends Annotation> InvocationHandler getInvocationHandler(Class<T> annotationType, Map<String, ?> values) {
        final Function<Method, Object> getValue = method ->
            Optional.<Object>ofNullable(values.get(method.getName()))
                .orElseGet(method::getDefaultValue);
        final int hashCode = Arrays.stream(annotationType.getDeclaredMethods())
                .mapToInt(m -> memberHash(m.getName(), getValue.apply(m)))
                .sum();
        final String toString = "@" + annotationType.getName() + "("
                + values.entrySet()
                    .stream()
                    .sorted(Comparator.comparing(Entry::getKey))
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "))
                + ")";
        return (proxy, method, args) -> {
            String name = method.getName();
            if ("annotationType".equals(name) && method.getParameterCount() == 0) {
                return annotationType;
            }

            if ("equals".equals(name) && method.getParameterCount() == 1
                && Object.class.equals(method.getParameterTypes()[0])) {
                Annotation that = (Annotation) args[0];
                return annotationType.equals(that.annotationType()) && valuesEqual(annotationType, proxy, that);
            }

            if ("hashCode".equals(name) && method.getParameterCount() == 0) {
                return hashCode;
            }

            if ("toString".equals(name) && method.getParameterCount() == 0) {
                return toString;
            }

            if (method.getDeclaringClass() == annotationType) {
                return getValue.apply(method);
            }

            throw new IllegalStateException("Unknown method " + method + " for annotation type " + annotationType);
        };
    }

    private static int memberHash(String name, Object value) {
        return (127 * name.hashCode()) ^ valueHash(value);
    }

    private static int valueHash(Object value) {
        Class<? extends Object> valueClass = value.getClass();
        if (!valueClass.isArray()) {
            return value.hashCode();
        }
        return Unchecked.supplier(() ->
            (Integer) MethodHandles.publicLookup()
                .findStatic(Arrays.class, "hashCode", MethodType.methodType(int.class, valueClass))
                .invoke(value)).get();
    }

    private static <A extends Annotation> boolean valuesEqual(Class<A> annotationType, Object a, Object b) {
        for (Method m : annotationType.getDeclaredMethods()) {
            Function<Object, Object> invoker = Unchecked.function(m::invoke);
            Object valueA = invoker.apply(a);
            Object valueB = invoker.apply(b);
            if (valueA != null
             && valueB != null
             && valueA.getClass().isArray()
             && valueA.getClass().equals(valueB.getClass())
             && Boolean.FALSE.equals(Unchecked.supplier(() ->
                    MethodHandles.publicLookup()
                        .findStatic(Arrays.class, "equals", MethodType.methodType(boolean.class, valueA.getClass(), valueB.getClass()))
                        .invoke(a, b)).get())) {
                return false;
            }
            if (!Objects.equals(valueA, valueB)) {
                return false;
            }
        }
        return true;
    }
}
