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
package org.jdbi.v3.vavr;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vavr.Lazy;
import io.vavr.Value;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * supports several vavr value classes ({@link Option}, {@link Lazy}, {@link Either}, {@link Try} and {@link Validation}) with the underlying "nested" value
 * being resolved via "get"
 * <p>
 * if there is no such value (Try-Failed, Either-Left...) a "null" value will be applied as argument value
 */
class VavrValueArgumentFactory implements ArgumentFactory.Preparable {
    private static final Class<?>[] VALUE_CLASSES = {Option.class, Lazy.class, Try.class, Either.class, Validation.class};

    private static final Supplier<Type> OBJECT_SUPPLIER = () -> Object.class;

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        if (acceptType(type)) {
            Object nestedValue = unwrapValue((Value<?>) value);
            Type nestedType = findGenericType(type, nestedValue);
            return config.get(Arguments.class).findFor(nestedType, nestedValue);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        if (acceptType(type)) {
            return config.get(Arguments.class)
                .prepareFor(findGenericType(type, null))
                .map(argumentFunction ->
                    value -> argumentFunction.apply(unwrapValue((Value<?>) value)));
        }
        return Optional.empty();
    }

    private static boolean acceptType(Type type) {
        Class<?> rawType = getErasedType(type);
        for (Class<?> valueClass : VALUE_CLASSES) {
            if (valueClass.isAssignableFrom(rawType)) {
                return true;
            }
        }
        return false;
    }
    private static Type findGenericType(Type wrapperType, Object nestedValue) {
        Optional<Type> nestedType = findGenericParameter(wrapperType, Value.class);
        return nestedType.orElseGet(typeFromValue(nestedValue));
    }

    private static Supplier<Type> typeFromValue(Object object) {
        return object == null ? OBJECT_SUPPLIER : object::getClass;
    }

    private static Object unwrapValue(Value<?> value) {
        return value == null ? null : value.getOrNull();
    }
}
