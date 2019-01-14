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
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map java primitives:
 * <ul>
 *     <li>{@code boolean}</li>
 *     <li>{@code byte}</li>
 *     <li>{@code char}</li>
 *     <li>{@code short}</li>
 *     <li>{@code int}</li>
 *     <li>{@code long}</li>
 *     <li>{@code float}</li>
 *     <li>{@code double}</li>
 * </ul>
 */
class PrimitiveMapperFactory implements ColumnMapperFactory {
    private final Map<Class<?>, ColumnMapper<?>> mappers = new IdentityHashMap<>();

    PrimitiveMapperFactory() {
        mappers.put(boolean.class, primitiveMapper(ResultSet::getBoolean));
        mappers.put(byte.class, primitiveMapper(ResultSet::getByte));
        mappers.put(char.class, primitiveMapper(PrimitiveMapperFactory::getChar));
        mappers.put(short.class, primitiveMapper(ResultSet::getShort));
        mappers.put(int.class, primitiveMapper(ResultSet::getInt));
        mappers.put(long.class, primitiveMapper(ResultSet::getLong));
        mappers.put(float.class, primitiveMapper(ResultSet::getFloat));
        mappers.put(double.class, primitiveMapper(ResultSet::getDouble));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);

        return Optional.ofNullable(mappers.get(rawType));
    }

    private static <T> ColumnMapper<T> primitiveMapper(ColumnGetter<T> getter) {
        return (r, i, ctx) -> getter.get(r, i);
    }

    private static char getChar(ResultSet r, int i) throws SQLException {
        Character character = getCharacter(r, i);
        return character == null ? '\000' : character;
    }

    private static Character getCharacter(ResultSet r, int i) throws SQLException {
        String s = r.getString(i);
        if (s != null && !s.isEmpty()) {
            return s.charAt(0);
        }
        return null;
    }
}
