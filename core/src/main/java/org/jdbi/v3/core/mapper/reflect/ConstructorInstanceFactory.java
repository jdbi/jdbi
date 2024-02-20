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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jdbi.v3.core.internal.exceptions.Unchecked;

import static java.util.Objects.requireNonNull;

class ConstructorInstanceFactory<T> extends InstanceFactory<T> {
    private final Constructor<T> constructor;

    private final Supplier<Type[]> typeSupplier;

    ConstructorInstanceFactory(Constructor<T> constructor) {
        super(constructor);
        this.constructor = requireNonNull(constructor, "constructor is null");
        this.typeSupplier = getTypeSupplier(constructor, super::getTypes);
    }

    @Override
    Type[] getTypes() {
        return typeSupplier.get();
    }

    @Override
    T newInstance(Object... params) {
        return Unchecked.<Object[], T>function(constructor::newInstance).apply(params);
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

    private static <T> Supplier<Type[]> getTypeSupplier(Constructor<T> constructor, Supplier<Type[]> defaultSupplier) {
        if (isGenericInformationLost(constructor)) {
            return () -> getFields(constructor)
                .map(Field::getGenericType)
                .toArray(Type[]::new);
        }
        return defaultSupplier;
    }

    private static <T> Stream<Field> getFields(Constructor<T> constructor) {
        return Arrays.stream(constructor.getDeclaringClass().getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()));
    }
}
