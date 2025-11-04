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
package org.jdbi.v3.core.argument;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.meta.Beta;

/**
 * Inspect a value with optional static qualified type information and produce an {@link Argument}
 * that binds the value to a prepared statement.
 *
 * <p>Make sure to override {@link Object#toString} in your {@link Argument} instances if you want
 * to be able to log their values with an {@link org.jdbi.v3.core.statement.SqlLogger}.
 *
 * <p>Note that {@code null} is handled specially in a few cases, and a few {@code Jdbi} features
 * assume you'll return an instance of {@link NullArgument} when you intend to bind null.
 */
@FunctionalInterface
public interface QualifiedArgumentFactory {
    /**
     * Returns an {@link Argument} for the given value if the factory supports it; empty otherwise.
     *
     * @param type the known qualified type of value. Depending on the situation this may be a full
     *     generic signature e.g. {@link java.lang.reflect.ParameterizedType}, a {@link Class}, or
     *     Object.class if no type information is known.
     * @param value the value to convert into an {@link Argument}
     * @param config the config registry, for composition
     * @return an argument for the given value if this factory supports it, or <code>Optional.empty()
     *     </code> otherwise.
     * @see org.jdbi.v3.core.statement.StatementContext#findArgumentFor(QualifiedType, Object)
     * @see Arguments#findFor(QualifiedType, Object)
     * @see QualifiedType
     */
    Optional<Argument> build(QualifiedType<?> type, Object value, ConfigRegistry config);

    /**
     * Adapts an {@link ArgumentFactory} into a QualifiedArgumentFactory. The returned factory only
     * matches qualified types with zero qualifiers.
     *
     * @param config the ConfigRegistry
     * @param factory the factory to adapt
     */
    static QualifiedArgumentFactory adapt(ConfigRegistry config, ArgumentFactory factory) {
        if (factory instanceof ArgumentFactory.Preparable preparable) {
            return adapt(config, preparable);
        }
        Set<Annotation> qualifiers = config.get(Qualifiers.class).findFor(factory.getClass());
        return (type, value, cfg) ->
            type.getQualifiers().equals(qualifiers)
                ? factory.build(type.getType(), value, cfg)
                : Optional.empty();
    }

    /**
     * Adapts an {@link ArgumentFactory.Preparable} into a QualifiedArgumentFactory.Preparable.
     * The returned factory only matches qualified types with zero qualifiers.
     *
     * @param factory the factory to adapt
     */
    static QualifiedArgumentFactory.Preparable adapt(ConfigRegistry config, ArgumentFactory.Preparable factory) {
        return QualifiedArgumentFactory.Preparable.adapt(config, factory);
    }

    /**
     * QualifiedArgumentFactory extension interface that allows preparing arguments for efficient batch binding.
     */
    @Beta
    interface Preparable extends QualifiedArgumentFactory {
        Optional<Function<Object, Argument>> prepare(QualifiedType<?> type, ConfigRegistry config);

        /**
         * @deprecated no longer used
         */
        @Deprecated(since = "3.15.0", forRemoval = true)
        default Collection<QualifiedType<?>> prePreparedTypes() {
            return Collections.emptyList();
        }

        /**
         * Adapts an {@link ArgumentFactory.Preparable} into a QualifiedArgumentFactory.Preparable
         * The returned factory only matches qualified types with zero qualifiers.
         *
         * @param factory the factory to adapt
         */
        static QualifiedArgumentFactory.Preparable adapt(ConfigRegistry config, ArgumentFactory.Preparable factory) {
            return new Preparable() {
                final Set<Annotation> qualifiers =
                        config.get(Qualifiers.class)
                        .findFor(factory.getClass());

                final Collection<QualifiedType<?>> prePreparedTypes = Collections.unmodifiableList(
                        factory.prePreparedTypes().stream()
                            .map(QualifiedType::of)
                            .map(qt -> qt.withAnnotations(qualifiers))
                            .collect(Collectors.toList()));

                @Override
                public Optional<Argument> build(QualifiedType<?> type, Object value, ConfigRegistry cfg) {
                    return type.getQualifiers().equals(qualifiers)
                            ? factory.build(type.getType(), value, cfg)
                            : Optional.empty();
                }

                @Override
                public Optional<Function<Object, Argument>> prepare(QualifiedType<?> type, ConfigRegistry cfg) {
                    return type.getQualifiers().equals(qualifiers)
                            ? factory.prepare(type.getType(), cfg)
                            : Optional.empty();
                }

                /**
                 * @deprecated no longer used
                 */
                @Deprecated(since = "3.39.0", forRemoval = true)
                @Override
                public Collection<QualifiedType<?>> prePreparedTypes() {
                    return prePreparedTypes;
                }

                @Override
                public String toString() {
                    return "Qualified[" + factory + "]";
                }
            };
        }
    }
}
