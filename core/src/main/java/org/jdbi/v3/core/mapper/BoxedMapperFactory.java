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
 * Column mapper factory which knows how to map JDBC-recognized types, along with some other well-known types
 * from the JDK.
 */
public class BoxedMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = new HashMap<>();

    static {
        MAPPERS.put(Boolean.class, referenceMapper(ResultSet::getBoolean));
        MAPPERS.put(Byte.class, referenceMapper(ResultSet::getByte));
        MAPPERS.put(Character.class, referenceMapper(BoxedMapperFactory::getCharacter));
        MAPPERS.put(Short.class, referenceMapper(ResultSet::getShort));
        MAPPERS.put(Integer.class, referenceMapper(ResultSet::getInt));
        MAPPERS.put(Long.class, referenceMapper(ResultSet::getLong));
        MAPPERS.put(Float.class, referenceMapper(ResultSet::getFloat));
        MAPPERS.put(Double.class, referenceMapper(ResultSet::getDouble));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);

        return Optional.ofNullable(MAPPERS.get(rawType));
    }

    private static <T> ColumnMapper<T> referenceMapper(ColumnGetter<T> getter) {
        return (r, i, ctx) -> {
            T value = getter.get(r, i);
            return r.wasNull() ? null : value;
        };
    }

    private static Character getCharacter(ResultSet r, int i) throws SQLException {
        String s = r.getString(i);
        if (s != null && !s.isEmpty()) {
            return s.charAt(0);
        }
        return null;
    }
}
