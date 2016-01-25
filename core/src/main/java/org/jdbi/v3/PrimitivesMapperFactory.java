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
import java.net.URL;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.util.BigDecimalMapper;
import org.jdbi.v3.util.BooleanMapper;
import org.jdbi.v3.util.ByteArrayMapper;
import org.jdbi.v3.util.ByteMapper;
import org.jdbi.v3.util.CharMapper;
import org.jdbi.v3.util.DoubleMapper;
import org.jdbi.v3.util.EnumMapper;
import org.jdbi.v3.util.FloatMapper;
import org.jdbi.v3.util.IntegerMapper;
import org.jdbi.v3.util.LongMapper;
import org.jdbi.v3.util.ShortMapper;
import org.jdbi.v3.util.StringMapper;
import org.jdbi.v3.util.TimestampMapper;
import org.jdbi.v3.util.URIMapper;
import org.jdbi.v3.util.URLMapper;

/**
 * Result column mapper factory which knows how to map standard JDBC-recognized types.
 */
public class PrimitivesMapperFactory implements ResultColumnMapperFactory {
    private static final Map<Class<?>, ResultColumnMapper<?>> mappers = new HashMap<>();

    static {
        mappers.put(boolean.class, BooleanMapper.PRIMITIVE);
        mappers.put(byte.class, ByteMapper.PRIMITIVE);
        mappers.put(char.class, CharMapper.PRIMITIVE);
        mappers.put(short.class, ShortMapper.PRIMITIVE);
        mappers.put(int.class, IntegerMapper.PRIMITIVE);
        mappers.put(long.class, LongMapper.PRIMITIVE);
        mappers.put(float.class, FloatMapper.PRIMITIVE);
        mappers.put(double.class, DoubleMapper.PRIMITIVE);

        mappers.put(Boolean.class, BooleanMapper.WRAPPER);
        mappers.put(Byte.class, ByteMapper.WRAPPER);
        mappers.put(Character.class, CharMapper.WRAPPER);
        mappers.put(Short.class, ShortMapper.WRAPPER);
        mappers.put(Integer.class, IntegerMapper.WRAPPER);
        mappers.put(Long.class, LongMapper.WRAPPER);
        mappers.put(Float.class, FloatMapper.WRAPPER);
        mappers.put(Double.class, DoubleMapper.WRAPPER);

        mappers.put(BigDecimal.class, BigDecimalMapper.INSTANCE);

        mappers.put(String.class, StringMapper.INSTANCE);

        mappers.put(byte[].class, ByteArrayMapper.INSTANCE);

        mappers.put(Timestamp.class, TimestampMapper.INSTANCE);

        mappers.put(URL.class, URLMapper.INSTANCE);
        mappers.put(URI.class, URIMapper.INSTANCE);
    }

    @Override
    public boolean accepts(Type type, StatementContext ctx) {
        Class<?> rawType = getErasedType(type);
        return rawType.isEnum() || mappers.containsKey(rawType);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ResultColumnMapper<?> columnMapperFor(Type type, StatementContext ctx) {
        Class<?> rawType = getErasedType(type);
        if (rawType.isEnum()) {
            return EnumMapper.byName(
                    (Class<? extends Enum>) rawType.asSubclass(Enum.class));
        }
        return mappers.get(rawType);
    }
}
