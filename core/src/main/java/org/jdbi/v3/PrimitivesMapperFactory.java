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

import org.jdbi.v3.tweak.ResultSetMapper;
import org.jdbi.v3.util.BigDecimalMapper;
import org.jdbi.v3.util.BooleanMapper;
import org.jdbi.v3.util.ByteArrayMapper;
import org.jdbi.v3.util.ByteMapper;
import org.jdbi.v3.util.DoubleMapper;
import org.jdbi.v3.util.FloatMapper;
import org.jdbi.v3.util.IntegerMapper;
import org.jdbi.v3.util.LongMapper;
import org.jdbi.v3.util.ShortMapper;
import org.jdbi.v3.util.StringMapper;
import org.jdbi.v3.util.TimestampMapper;
import org.jdbi.v3.util.URLMapper;

/**
 * Result set mapper factory which knows how to construct java primitive types.
 *
 * @deprecated Use {@link PrimitivesColumnMapperFactory} instead.
 */
@Deprecated
public class PrimitivesMapperFactory implements ResultSetMapperFactory
{
    private static final Map<Class<?>, ResultSetMapper<?>> mappers = new HashMap<>();

    static {

        mappers.put(BigDecimal.class, BigDecimalMapper.FIRST);

        mappers.put(Boolean.class, BooleanMapper.FIRST);
        mappers.put(boolean.class, BooleanMapper.FIRST);

        mappers.put(byte[].class, ByteArrayMapper.FIRST);

        mappers.put(short.class, ShortMapper.FIRST);
        mappers.put(Short.class, ShortMapper.FIRST);

        mappers.put(Float.class, FloatMapper.FIRST);
        mappers.put(float.class, FloatMapper.FIRST);

        mappers.put(Double.class, DoubleMapper.FIRST);
        mappers.put(double.class, DoubleMapper.FIRST);

        mappers.put(Byte.class, ByteMapper.FIRST);
        mappers.put(byte.class, ByteMapper.FIRST);

        mappers.put(URL.class, URLMapper.FIRST);

        mappers.put(int.class, IntegerMapper.FIRST);
        mappers.put(Integer.class, IntegerMapper.FIRST);

        mappers.put(long.class, LongMapper.FIRST);
        mappers.put(Long.class, LongMapper.FIRST);

        mappers.put(Timestamp.class, TimestampMapper.FIRST);

        mappers.put(String.class, StringMapper.FIRST);
    }

    @Override
    public boolean accepts(Class<?> type, StatementContext ctx)
    {
        return mappers.containsKey(type) || type.isEnum();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public ResultSetMapper<?> mapperFor(Class<?> type, StatementContext ctx)
    {
        if (type.isEnum()) {
            return new EnumMapper(1, type);
        }
        return mappers.get(type);
    }
}
