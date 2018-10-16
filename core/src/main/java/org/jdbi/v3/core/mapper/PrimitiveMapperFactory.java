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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map java primitives.
 */
class PrimitiveMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = new HashMap<>();

    // TODO allow user to configure if nulls should return defaults (current) or throw exception

    static {
        MAPPERS.put(boolean.class, primitiveMapper(ResultSet::getBoolean));
        MAPPERS.put(byte.class, primitiveMapper(ResultSet::getByte));
        MAPPERS.put(char.class, primitiveMapper(PrimitiveMapperFactory::getChar));
        MAPPERS.put(short.class, primitiveMapper(ResultSet::getShort));
        MAPPERS.put(int.class, primitiveMapper(ResultSet::getInt));
        MAPPERS.put(long.class, primitiveMapper(ResultSet::getLong));
        MAPPERS.put(float.class, primitiveMapper(ResultSet::getFloat));
        MAPPERS.put(double.class, primitiveMapper(ResultSet::getDouble));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);

        return Optional.ofNullable(MAPPERS.get(rawType));
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
