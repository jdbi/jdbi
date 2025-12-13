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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jdbi.v3.core.internal.exceptions.Sneaky;

import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;

class ConstructorInstanceFactory<T> extends InstanceFactory<T> {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Map<Constructor<?>, ConstructorHandleAndTypes> CONSTRUCTOR_CACHE = synchronizedMap(new WeakHashMap<>());

    private final Constructor<T> constructor;
    private final List<Type> types;
    private final MethodHandle constructorHandle;

    ConstructorInstanceFactory(Constructor<T> constructor) {
        super(constructor);
        this.constructor = requireNonNull(constructor, "constructor is null");
        ConstructorHandleAndTypes constructorHandleAndTypes = getConstructorHandleAndTypes(constructor, super::getTypes);
        this.types = constructorHandleAndTypes.getTypes();
        this.constructorHandle = constructorHandleAndTypes.getConstructorHandle();
    }

    @Override
    List<Type> getTypes() {
        return types;
    }

    @SuppressWarnings("unchecked")
    @Override
    T newInstance(Object... params) {
        try {
            return (T) constructorHandle.invokeWithArguments(params);
        } catch (Throwable e) {
            throw Sneaky.throwAnyway(e);
        }
    }

    @Override
    public String toString() {
        return constructor.toString();
    }

    // Note: this can be removed once https://bugs.openjdk.org/browse/JDK-8320575 is resolved
    private static <T> boolean isGenericInformationLost(Constructor<T> factory) {
        // If there is more than one constructor, ensure that the factory constructor is the canonical constructor.
        // If so it will need to get the type parameters from the declared fields.
        if (factory.getParameters().length != getFields(factory)
            .count()) {
            return false;
        }
        boolean lossDetected = false;
        for (int i = 0; i < factory.getParameters().length; i++) {
            Parameter parameter = factory.getParameters()[i];
            Field field = factory.getDeclaringClass().getDeclaredFields()[i];
            if (!parameter.getName().equals(field.getName()) || !parameter.getType().equals(field.getType())) {
                return false;
            }
            // Check if the generic type information is lost, due to jdk21 record constructor: https://bugs.openjdk.org/browse/JDK-8320575
            if (parameter.getType().getTypeParameters().length > 0
                && !parameter.getParameterizedType().equals(field.getGenericType())) {
                lossDetected = true;
            }
        }
        return lossDetected;
    }

    private static <T> ConstructorHandleAndTypes getConstructorHandleAndTypes(Constructor<T> constructor, Supplier<List<Type>> defaultSupplier) {
        return CONSTRUCTOR_CACHE.computeIfAbsent(constructor, ctor -> computeConstructorHandleAndTypes(ctor, defaultSupplier));
    }

    private static <T> ConstructorHandleAndTypes computeConstructorHandleAndTypes(Constructor<T> constructor, Supplier<List<Type>> defaultSupplier) {
        MethodHandle constructorMethodHandle = getConstructorMethodHandle(constructor);
        if (isGenericInformationLost(constructor)) {
            return new ConstructorHandleAndTypes(constructorMethodHandle, getFields(constructor)
                .map(Field::getGenericType)
                .toList());
        }
        return new ConstructorHandleAndTypes(constructorMethodHandle, defaultSupplier.get());
    }

    private static <T> MethodHandle getConstructorMethodHandle(Constructor<T> constructor) {
        try {
            return LOOKUP.unreflectConstructor(constructor).asFixedArity();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> Stream<Field> getFields(Constructor<T> constructor) {
        return Arrays.stream(constructor.getDeclaringClass().getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()));
    }

    private static class ConstructorHandleAndTypes {
        private final MethodHandle constructorHandle;
        private final List<Type> types;

        ConstructorHandleAndTypes(MethodHandle constructorHandle, List<Type> types) {
            this.constructorHandle = requireNonNull(constructorHandle, "constructorHandle is null");
            this.types = requireNonNull(types, "types is null");
        }

        public MethodHandle getConstructorHandle() {
            return constructorHandle;
        }

        public List<Type> getTypes() {
            return types;
        }
    }
}
