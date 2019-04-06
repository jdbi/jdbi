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

import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.meta.Beta;

/**
 * Factory interface used to produce column mappers.
 */
@FunctionalInterface
@Beta
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
        return (type, config) -> type.getQualifiers().equals(
                config.get(Qualifiers.class).findFor(factory.getClass()))
            ? factory.build(type.getType(), config)
            : Optional.empty();
    }

    /**
     * Create a QualifiedColumnMapperFactory from a given {@link ColumnMapper} that matches
     * a single {@link QualifiedType} exactly.
     *
     * @param type the mapped type
     * @param mapper the mapper
     * @param <T> the mapped type
     * @return
     */
    static <T> QualifiedColumnMapperFactory of(QualifiedType<T> type, ColumnMapper<T> mapper) {
        return (t, config) -> t.equals(type) ? Optional.of(mapper) : Optional.empty();
    }
}
