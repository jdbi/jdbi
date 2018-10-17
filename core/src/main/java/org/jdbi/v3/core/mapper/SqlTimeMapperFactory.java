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
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map java.sql timekeeping objects:
 * <ul>
 *     <li>{@link Timestamp}</li>
 * </ul>
 */
class SqlTimeMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = new IdentityHashMap<>();

    static {
        MAPPERS.put(Timestamp.class, new ReferenceMapper<>(ResultSet::getTimestamp));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);

        return Optional.ofNullable(MAPPERS.get(rawType));
    }
}
