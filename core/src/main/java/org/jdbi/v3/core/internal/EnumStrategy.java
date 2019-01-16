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
import java.lang.reflect.Type;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.jdbi.v3.core.EnumByName;
import org.jdbi.v3.core.EnumByOrdinal;
import org.jdbi.v3.core.EnumConfig;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

public interface EnumStrategy extends ArgumentFactory, ColumnMapperFactory {
    @SuppressWarnings("unchecked")
    static <E extends Enum<E>> Optional<Class<E>> enumType(Type type) {
        return Optional.of(type)
                .map(GenericTypes::getErasedType)
                .filter(c -> c.isEnum())
                .map(t -> (Class<E>) t); // You'd like to do Enum.class::asSubclass but that doesn't work???
    }

    int nullType();

    Optional<Argument> bind(Arguments args, Enum<?> value);

    <E extends Enum<E>> ColumnMapper<E> columnMapper(Class<E> enumClass);

    @Override
    default Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        if (value == null) {
            return Optional.of(new NullArgument(nullType()));
        }
        return bind(config.get(Arguments.class), (Enum<?>) value);
    }

    @Override
    default Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        return enumType(type).map(this::columnMapper);
    }

    @EnumByName
    class ByName implements EnumStrategy {
        public static final EnumStrategy INSTANCE = new ByName();

        @Override
        public int nullType() {
            return Types.VARCHAR;
        }

        @Override
        public Optional<Argument> bind(Arguments args, Enum<?> value) {
            return args.findFor(String.class, value.name());
        }

        @Override
        public <E extends Enum<E>> ColumnMapper<E> columnMapper(Class<E> enumClass) {
            final ConcurrentMap<String, Enum<?>> cache = new ConcurrentHashMap<>();
            return (rs, col, ctx) -> {
                String name = ctx.findColumnMapperFor(String.class)
                        .orElseThrow(() -> new UnableToProduceResultException("a String column mapper is required to map Enums from names", ctx))
                        .map(rs, col, ctx);

                return name == null ? null : enumClass.cast(cache.computeIfAbsent(name.toLowerCase(), lowercased -> matchByName(enumClass, name, ctx)));
            };
        }

        private static <E extends Enum<E>> E matchByName(Class<E> type, String name, StatementContext ctx) {
            try {
                return Enum.valueOf(type, name);
            } catch (IllegalArgumentException ignored) {
                return Arrays.stream(type.getEnumConstants())
                    .filter(e -> e.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new UnableToProduceResultException(String.format(
                        "no %s value could be matched to the name %s", type.getSimpleName(), name
                    ), ctx));
            }
        }
    }

    @EnumByOrdinal
    class ByOrdinal implements EnumStrategy {
        public static final EnumStrategy INSTANCE = new ByOrdinal();

        @Override
        public int nullType() {
            return Types.INTEGER;
        }

        @Override
        public Optional<Argument> bind(Arguments args, Enum<?> value) {
            return args.findFor(Integer.class, value.ordinal());
        }

        @Override
        public <E extends Enum<E>> ColumnMapper<E> columnMapper(Class<E> enumClass) {
            final Enum<?>[] constants = enumClass.getEnumConstants();
            return (rs, col, ctx) -> {
                Integer ordinal = ctx.findColumnMapperFor(Integer.class)
                    .orElseThrow(() -> new UnableToProduceResultException("an Integer column mapper is required to map Enums from ordinals", ctx))
                    .map(rs, col, ctx);

                try {
                    return ordinal == null ? null : enumClass.cast(constants[ordinal]);
                } catch (ArrayIndexOutOfBoundsException oob) {
                    throw new UnableToProduceResultException(String.format(
                        "no %s value could be matched to the ordinal %s", enumClass.getSimpleName(), ordinal
                    ), oob, ctx);
                }
            };
        }
    }

    class Unqualified implements ArgumentFactory, ColumnMapperFactory {
        public static final Unqualified INSTANCE = new Unqualified();

        @SuppressWarnings("unchecked")
        @Override
        public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
            return enumType(type).flatMap(et ->
                config.get(ColumnMappers.class).findFor(defaultQualify(type, config)));
        }

        @Override
        public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
            return enumType(type).flatMap(et ->
                config.get(Arguments.class).findFor(defaultQualify(type, config), value));
        }

        @SuppressWarnings("rawtypes")
        private QualifiedType defaultQualify(Type type, ConfigRegistry config) { // XXX: should be QT<?> but causes type inference fail on java8
            Set<Class<? extends Annotation>> qualifiers = Qualifiers.getQualifiers(GenericTypes.getErasedType(type))
                    .stream()
                    .map(Annotation::annotationType)
                    .collect(Collectors.toSet());
            if (qualifiers.isEmpty()) {
                qualifiers = Collections.singleton(config.get(EnumConfig.class).getDefaultQualifier());
            }
            return QualifiedType.of(type).with(qualifiers.stream().map(AnnotationFactory::create).collect(Collectors.toList()));
        }
    }
}
