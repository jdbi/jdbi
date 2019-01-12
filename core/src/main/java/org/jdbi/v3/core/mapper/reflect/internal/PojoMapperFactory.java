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
package org.jdbi.v3.core.mapper.reflect.internal;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;

/**
 * Row mapper that inspects an {@code immutables}-style Immutable or Modifiable value class for properties
 * and binds them in the style of {@link org.jdbi.v3.core.mapper.reflect.BeanMapper}.
 */
public class PojoMapperFactory implements RowMapperFactory {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        return config.get(PojoTypes.class).findFor(type)
                .map(p -> new PojoMapper(GenericTypes.getErasedType(type), p, ""));
    }
}
