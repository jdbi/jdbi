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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public final class JdbiClassUtils {

    private JdbiClassUtils() {
        throw new UtilityClassException();
    }

    public static boolean isPresent(String klass) {
        try {
            Class.forName(klass);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

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

    public static <T> T createInstance(Class<T> factoryClass) {
        try {
            return factoryClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException(format("Unable to instantiate class %s", factoryClass), e);
        }
    }

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
}
