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
package org.jdbi.v3;

import static org.jdbi.v3.Types.getErasedType;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.tweak.ColumnMapper;
import org.jdbi.v3.util.EnumMapper;

/**
 * Column mapper factory which knows how to map JDBC-recognized types, along with some other well-known types
 * from the JDK.
 */
public class BuiltInMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> mappers = new HashMap<>();

    static {
        mappers.put(boolean.class, primitiveMapper(ResultSet::getBoolean));
        mappers.put(byte.class, primitiveMapper(ResultSet::getByte));
        mappers.put(char.class, primitiveMapper(BuiltInMapperFactory::getChar));
        mappers.put(short.class, primitiveMapper(ResultSet::getShort));
        mappers.put(int.class, primitiveMapper(ResultSet::getInt));
        mappers.put(long.class, primitiveMapper(ResultSet::getLong));
        mappers.put(float.class, primitiveMapper(ResultSet::getFloat));
        mappers.put(double.class, primitiveMapper(ResultSet::getDouble));

        mappers.put(Boolean.class, referenceMapper(ResultSet::getBoolean));
        mappers.put(Byte.class, referenceMapper(ResultSet::getByte));
        mappers.put(Character.class, referenceMapper(BuiltInMapperFactory::getCharacter));
        mappers.put(Short.class, referenceMapper(ResultSet::getShort));
        mappers.put(Integer.class, referenceMapper(ResultSet::getInt));
        mappers.put(Long.class, referenceMapper(ResultSet::getLong));
        mappers.put(Float.class, referenceMapper(ResultSet::getFloat));
        mappers.put(Double.class, referenceMapper(ResultSet::getDouble));

        mappers.put(BigDecimal.class, referenceMapper(ResultSet::getBigDecimal));

        mappers.put(String.class, referenceMapper(ResultSet::getString));

        mappers.put(byte[].class, referenceMapper(ResultSet::getBytes));

        mappers.put(Timestamp.class, referenceMapper(ResultSet::getTimestamp));

        mappers.put(URL.class, referenceMapper(ResultSet::getURL));
        mappers.put(URI.class, referenceMapper(BuiltInMapperFactory::getURI));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ColumnMapper<?>> build(Type type, StatementContext ctx) {
        Class<?> rawType = getErasedType(type);
        if (rawType.isEnum()) {
            return Optional.of(EnumMapper.byName((Class<? extends Enum>) rawType));
        }

        return Optional.ofNullable(mappers.get(rawType));
    }

    @FunctionalInterface
    interface ColumnGetter<T> {
        T get(ResultSet rs, int i) throws SQLException;
    }

    private static <T> ColumnMapper<T> primitiveMapper(ColumnGetter<T> getter) {
        return (r, i, ctx) -> getter.get(r, i);
    }

    private static <T> ColumnMapper<T> referenceMapper(ColumnGetter<T> getter) {
        return (r, i, ctx) -> {
            T value = getter.get(r, i);
            return r.wasNull() ? null : value;
        };
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

    private static URI getURI(ResultSet r, int i) throws SQLException {
        String s = r.getString(i);
        try {
            return (s != null) ? (new URI(s)) : null;
        } catch(URISyntaxException e) {
            throw new SQLException("Failed to convert data to URI", e);
        }
    }
}
