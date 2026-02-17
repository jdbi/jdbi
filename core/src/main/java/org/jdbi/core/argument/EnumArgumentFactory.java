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
package org.jdbi.core.argument;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.enums.DatabaseValue;
import org.jdbi.core.enums.EnumStrategy;
import org.jdbi.core.internal.EnumStrategies;
import org.jdbi.core.internal.exceptions.Unchecked;
import org.jdbi.core.qualifier.QualifiedType;

class EnumArgumentFactory implements QualifiedArgumentFactory {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<Argument> build(QualifiedType<?> givenType, Object value, ConfigRegistry config) {
        return ifEnum(givenType.getType())
            .flatMap(clazz -> makeEnumArgument((QualifiedType<Enum>) givenType, (Enum) value, config));
    }

    @SuppressWarnings("unchecked")
    static <E extends Enum<E>> Optional<Class<E>> ifEnum(Type type) {
        if (type instanceof Class<?> clazz && Enum.class.isAssignableFrom(clazz)) {
            return Optional.of((Class<E>) clazz);
        }
        return Optional.empty();
    }

    private static <E extends Enum<E>> Optional<Argument> makeEnumArgument(QualifiedType<E> givenType, E value, ConfigRegistry config) {
        boolean byName = EnumStrategy.BY_NAME == config.get(EnumStrategies.class).findStrategy(givenType);

        return byName
            ? byName(value, config)
            : byOrdinal(value, config);
    }

    private static <E extends Enum<E>> Optional<Argument> byName(E value, ConfigRegistry config) {
        return makeArgument(Types.VARCHAR, String.class, value, EnumArgumentFactory::annotatedValue, config);
    }

    private static <E extends Enum<E>> String annotatedValue(E e) {
        return Optional.of(e.getDeclaringClass())
                .map(Unchecked.function(type -> type.getField(e.name())))
                .map(field -> field.getAnnotation(DatabaseValue.class))
                .map(DatabaseValue::value)
                .orElse(e.name());
    }

    private static <E extends Enum<E>> Optional<Argument> byOrdinal(E value, ConfigRegistry config) {
        return makeArgument(Types.INTEGER, Integer.class, value, E::ordinal, config);
    }

    private static <A, E extends Enum<E>> Optional<Argument> makeArgument(int nullType,
                                                                          Class<A> attributeType,
                                                                          E value,
                                                                          Function<E, A> transform,
                                                                          ConfigRegistry config) {
        if (value == null) {
            return Optional.of(new NullArgument(nullType));
        }

        return config.get(Arguments.class).findFor(attributeType, transform.apply(value));
    }
}
