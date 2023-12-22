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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Row mapper factory which knows how to map row elements wrapped in an Optional.
 */
class OptionalRowMapperFactory implements RowMapperFactory {

    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        return Optional.of(type)
            .filter(t -> getErasedType(t) == Optional.class)
            .flatMap(t -> create(t, config));
    }

    private static Optional<RowMapper<?>> create(Type type, ConfigRegistry config) {
        return config.get(RowMappers.class).findFor(
                GenericTypes.findGenericParameter(type, Optional.class)
                    .orElseThrow(() -> new NoSuchMapperException("No mapper for raw Optional type")))
            .map(mapper -> (r, ctx) -> (Optional<?>) Optional.ofNullable(mapper.map(r, ctx)));
    }
}
