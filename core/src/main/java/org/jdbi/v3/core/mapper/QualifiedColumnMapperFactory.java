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

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;

import static org.jdbi.v3.core.qualifier.Qualifiers.getQualifiers;

@FunctionalInterface
interface QualifiedColumnMapperFactory {
    <T> Optional<ColumnMapper<T>> build(QualifiedType<T> type, ConfigRegistry config);

    static QualifiedColumnMapperFactory adapt(ColumnMapperFactory factory) {
        Set<Annotation> qualifiers = getQualifiers(factory.getClass());

        return new QualifiedColumnMapperFactory() {
            @Override
            public <T> Optional<ColumnMapper<T>> build(QualifiedType<T> type, ConfigRegistry config) {
                return type.getQualifiers().equals(qualifiers)
                    ? factory.build(type.getType(), config).map(m -> (ColumnMapper<T>) m)
                    : Optional.empty();
            }
        };
    }

    static <T> QualifiedColumnMapperFactory of(QualifiedType<T> type, ColumnMapper<T> mapper) {
        return new QualifiedColumnMapperFactory() {
            @Override
            public <X> Optional<ColumnMapper<X>> build(QualifiedType<X> t, ConfigRegistry config) {
                return t.equals(type) ? Optional.of((ColumnMapper<X>) mapper) : Optional.empty();
            }
        };
    }
}
