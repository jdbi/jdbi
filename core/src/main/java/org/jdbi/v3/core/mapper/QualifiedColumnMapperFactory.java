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
package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Predicate;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.RedundantWildcards;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

/**
 * Factory interface used to produce column mappers.
 */
@FunctionalInterface
public interface QualifiedColumnMapperFactory {
    /**
     * Supplies a column mapper which will map columns to type if the factory supports it; empty
     * otherwise.
     *
     * @param type   the target qualified type to map to
     * @param config the config registry, for composition
     * @return a column mapper for the given type if this factory supports it, or <code>Optional.empty()</code> otherwise.
     * @see ColumnMappers for composition
     * @see QualifiedType
     */
    Optional<ColumnMapper<?>> build(QualifiedType<?> type, ConfigRegistry config);

    /**
     * Adapts a {@link ColumnMapperFactory} into a QualifiedColumnMapperFactory. The returned
     * factory only matches qualified types with zero qualifiers.
     *
     * @param factory the factory to adapt
     */
    static QualifiedColumnMapperFactory adapt(ColumnMapperFactory factory) {
        return (type, config) -> type.hasQualifiers(config.get(Qualifiers.class).findFor(factory.getClass()))
            ? factory.build(type.getType(), config)
            : Optional.empty();
    }

    /**
     * Create a QualifiedColumnMapperFactory from a given {@link ColumnMapper} that matches
     * a single {@link QualifiedType}. A wildcard {@code ? extends B}, where no type other than {@code B} extends {@code B},
     * matches {@code B}, so {@code List<? extends String>} matches {@code List<String>}.
     *
     * @param type the mapped type
     * @param mapper the mapper
     * @param <T> the mapped type
     * @return A {@link QualifiedColumnMapperFactory}
     */
    static <T> QualifiedColumnMapperFactory of(QualifiedType<T> type, ColumnMapper<T> mapper) {
        Predicate<Type> matches = RedundantWildcards.matcher(type.getType());
        return (t, config) -> t.getQualifiers().equals(type.getQualifiers()) && matches.test(t.getType())
            ? Optional.of(mapper)
            : Optional.empty();
    }
}
