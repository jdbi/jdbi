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

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.qualifier.Qualifiers.getQualifiers;

import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;

/**
 * A generic QualifiedColumnMapperFactory that reflectively inspects a
 * {@code ColumnMapper<T>} and maps only to columns of type
 * {@code T}, with type qualifiers equal to the qualifiers present on
 * the mapper class. The type parameter T must be accessible
 * via reflection or an {@link UnsupportedOperationException}
 * will be thrown.
 */
class InferredColumnMapperFactory implements QualifiedColumnMapperFactory {
    private final QualifiedType<?> maps;
    private final ColumnMapper<?> mapper;

    InferredColumnMapperFactory(ColumnMapper<?> mapper) {
        this.maps = QualifiedType.of(
            findGenericParameter(mapper.getClass(), ColumnMapper.class)
                .orElseThrow(() -> new UnsupportedOperationException("Must use a concretely typed ColumnMapper here")))
            .with(getQualifiers(mapper.getClass()));
        this.mapper = mapper;
    }

    @Override
    public Optional<ColumnMapper<?>> build(QualifiedType<?> type, ConfigRegistry config) {
        return maps.equals(type)
                ? Optional.of(mapper)
                : Optional.empty();
    }
}
