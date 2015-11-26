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

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.util.BigDecimalColumnMapper;
import org.jdbi.v3.util.BooleanColumnMapper;
import org.jdbi.v3.util.ByteArrayColumnMapper;
import org.jdbi.v3.util.ByteColumnMapper;
import org.jdbi.v3.util.DoubleColumnMapper;
import org.jdbi.v3.util.EnumColumnMapper;
import org.jdbi.v3.util.FloatColumnMapper;
import org.jdbi.v3.util.IntegerColumnMapper;
import org.jdbi.v3.util.LongColumnMapper;
import org.jdbi.v3.util.ShortColumnMapper;
import org.jdbi.v3.util.StringColumnMapper;
import org.jdbi.v3.util.TimestampColumnMapper;
import org.jdbi.v3.util.URLColumnMapper;

/**
 * Result column mapper factory which knows how to map standard JDBC-recognized types.
 */
public class PrimitivesColumnMapperFactory implements ResultColumnMapperFactory {
    private static final Map<Class<?>, ResultColumnMapper<?>> mappers = new HashMap<>();

    static {
        mappers.put(boolean.class, BooleanColumnMapper.PRIMITIVE);
        mappers.put(byte.class, ByteColumnMapper.PRIMITIVE);
        mappers.put(short.class, ShortColumnMapper.PRIMITIVE);
        mappers.put(int.class, IntegerColumnMapper.PRIMITIVE);
        mappers.put(long.class, LongColumnMapper.PRIMITIVE);
        mappers.put(float.class, FloatColumnMapper.PRIMITIVE);
        mappers.put(double.class, DoubleColumnMapper.PRIMITIVE);

        mappers.put(Boolean.class, BooleanColumnMapper.WRAPPER);
        mappers.put(Byte.class, ByteColumnMapper.WRAPPER);
        mappers.put(Short.class, ShortColumnMapper.WRAPPER);
        mappers.put(Integer.class, IntegerColumnMapper.WRAPPER);
        mappers.put(Long.class, LongColumnMapper.WRAPPER);
        mappers.put(Float.class, FloatColumnMapper.WRAPPER);
        mappers.put(Double.class, DoubleColumnMapper.WRAPPER);

        mappers.put(BigDecimal.class, BigDecimalColumnMapper.INSTANCE);

        mappers.put(String.class, StringColumnMapper.INSTANCE);

        mappers.put(byte[].class, ByteArrayColumnMapper.INSTANCE);

        mappers.put(Timestamp.class, TimestampColumnMapper.INSTANCE);

        mappers.put(URL.class, URLColumnMapper.INSTANCE);
    }

    @Override
    public boolean accepts(TypeToken<?> type, StatementContext ctx) {
        Class<?> rawType = type.getRawType();
        return rawType.isEnum() || mappers.containsKey(rawType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ResultColumnMapper<? extends T> columnMapperFor(TypeToken<T> type, StatementContext ctx) {
        Class<? super T> rawType = type.getRawType();
        if (rawType.isEnum()) {
            return (ResultColumnMapper<? extends T>) EnumColumnMapper.byName(
                    (Class<? extends Enum>) rawType.asSubclass(Enum.class));
        }
        return (ResultColumnMapper<? extends T>) mappers.get(rawType);
    }
}
