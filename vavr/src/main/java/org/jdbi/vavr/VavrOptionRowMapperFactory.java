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
package org.jdbi.vavr;

import java.lang.reflect.Type;
import java.util.Optional;

import io.vavr.control.Option;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.mapper.NoSuchMapperException;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.RowMapperFactory;
import org.jdbi.core.mapper.RowMappers;

import static org.jdbi.core.generic.GenericTypes.getErasedType;

class VavrOptionRowMapperFactory implements RowMapperFactory {

    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        return Optional.of(type)
            .filter(t -> getErasedType(t) == Option.class)
            .flatMap(t -> create(t, config));
    }

    private static Optional<RowMapper<?>> create(Type type, ConfigRegistry config) {
        return config.get(RowMappers.class).findFor(
                GenericTypes.findGenericParameter(type, Option.class)
                    .orElseThrow(() -> new NoSuchMapperException("No mapper for raw vavr Option type")))
            .map(mapper -> (r, ctx) -> Option.of(mapper.map(r, ctx)));
    }
}
