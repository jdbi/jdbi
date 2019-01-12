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

/**
 * supports several vavr value classes ({@link Option}, {@link Lazy}, {@link Either}, {@link Try} and {@link Validation}) with the underlying "nested" value being resolved via "get"
 * <p>
 * if there is no such value (Try-Failed, Either-Left...) a "null" value will be applied as argument value
 */
class VavrValueArgumentFactory implements ArgumentFactory {

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        if (value instanceof Option || value instanceof Lazy || value instanceof Try || value instanceof Either || value instanceof Validation) {
            return buildValueArgument(type, config, (Value) value);
        }

        return Optional.empty();
    }

    private Optional<Argument> buildValueArgument(Type type, ConfigRegistry config, Value<?> value) {
        Type nestedType = findGenericParameter(type, Value.class).orElseGet(() -> extractTypeOfValue(value));
        Object nestedValue = value.getOrNull();
        return resolveNestedFromConfigured(config, nestedType, nestedValue);
    }

    Optional<Argument> resolveNestedFromConfigured(ConfigRegistry config, Type nestedType, Object nestedValue) {
        return config.get(Arguments.class).findFor(nestedType, nestedValue);
    }

    private Type extractTypeOfValue(Value<?> value) {
        Value<Class<?>> classOfValue = value.map(Object::getClass);
        return classOfValue.getOrElse(Object.class);
    }
}
