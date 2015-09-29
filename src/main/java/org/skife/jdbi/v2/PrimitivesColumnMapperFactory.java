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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.util.BigDecimalColumnMapper;
import org.skife.jdbi.v2.util.BooleanColumnMapper;
import org.skife.jdbi.v2.util.ByteArrayColumnMapper;
import org.skife.jdbi.v2.util.ByteColumnMapper;
import org.skife.jdbi.v2.util.DoubleColumnMapper;
import org.skife.jdbi.v2.util.EnumColumnMapper;
import org.skife.jdbi.v2.util.FloatColumnMapper;
import org.skife.jdbi.v2.util.IntegerColumnMapper;
import org.skife.jdbi.v2.util.LongColumnMapper;
import org.skife.jdbi.v2.util.ShortColumnMapper;
import org.skife.jdbi.v2.util.StringColumnMapper;
import org.skife.jdbi.v2.util.TimestampColumnMapper;
import org.skife.jdbi.v2.util.URLColumnMapper;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Result column mapper factory which knows how to map standard JDBC-recognized types.
 */
public class PrimitivesColumnMapperFactory implements ResultColumnMapperFactory {
    private static final Map<Class, ResultColumnMapper> mappers = new HashMap<Class, ResultColumnMapper>();

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
    public boolean accepts(Class type, StatementContext ctx) {
        return type.isEnum() || mappers.containsKey(type);
    }

    @Override
    public ResultColumnMapper columnMapperFor(Class type, StatementContext ctx) {
        if (type.isEnum()) {
            return EnumColumnMapper.byName(type);
        }
        return mappers.get(type);
    }
}
