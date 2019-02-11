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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
import org.jdbi.v3.core.generic.GenericTypes;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;

/**
 * supports several vavr value classes ({@link Option}, {@link Lazy}, {@link Either}, {@link Try} and {@link Validation}) with the underlying "nested" value being resolved via "get"
 * <p>
 * if there is no such value (Try-Failed, Either-Left...) a "null" value will be applied as argument value
 */
class VavrValueArgumentFactory implements ArgumentFactory {
    private static final Set<Class<?>> VALUE_CLASSES = new HashSet<>(Arrays.asList(Option.class, Lazy.class, Try.class, Either.class, Validation.class));

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        Class<?> rawType = GenericTypes.getErasedType(type);

        if (VALUE_CLASSES.stream().anyMatch(vc -> vc.isAssignableFrom(rawType))) {
            return buildValueArgument(type, config, (Value) value);
        }

        return Optional.empty();
    }

    private static Optional<Argument> buildValueArgument(Type type, ConfigRegistry config, Value<?> value) {
        Type nestedType = findGenericParameter(type, Value.class).orElseGet(() -> extractTypeOfValue(value));
        Object nestedValue = value == null ? null : value.getOrNull();
        return config.get(Arguments.class).findFor(nestedType, nestedValue);
    }

    private static Type extractTypeOfValue(Value<?> value) {
        Value<Class<?>> classOfValue = value.map(Object::getClass);
        return classOfValue.getOrElse(Object.class);
    }
}
