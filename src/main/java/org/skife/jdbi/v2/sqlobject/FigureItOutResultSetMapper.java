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
package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.PrimitivesMapperFactory;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.BigDecimalMapper;
import org.skife.jdbi.v2.util.BooleanMapper;
import org.skife.jdbi.v2.util.ByteArrayMapper;
import org.skife.jdbi.v2.util.ShortMapper;
import org.skife.jdbi.v2.util.FloatMapper;
import org.skife.jdbi.v2.util.DoubleMapper;
import org.skife.jdbi.v2.util.ByteMapper;
import org.skife.jdbi.v2.util.URLMapper;
import org.skife.jdbi.v2.util.IntegerMapper;
import org.skife.jdbi.v2.util.LongMapper;
import org.skife.jdbi.v2.util.TimestampMapper;
import org.skife.jdbi.v2.util.StringMapper;


import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

class FigureItOutResultSetMapper implements ResultSetMapper<Object>
{
    private static final PrimitivesMapperFactory factory = new PrimitivesMapperFactory();

    @Override
    public Object map(int index, ResultSet r, StatementContext ctx) throws SQLException
    {
        Method m = ctx.getSqlObjectMethod();
        Class<?> rt = m.getReturnType();
        GetGeneratedKeys ggk = m.getAnnotation(GetGeneratedKeys.class);
        String keyColumn = ggk.columnName();

        ResultSetMapper f;
        if (!"".equals(keyColumn)) {
            f = figureOutMapper(rt, keyColumn);
        } else {
            f = factory.mapperFor(rt, ctx);
        }
        return f.map(index, r, ctx);
    }

    ResultSetMapper figureOutMapper(Class keyType, String keyColumn) {
        ResultSetMapper f;
        if (keyType.isAssignableFrom(BigDecimal.class)) {
            f = new BigDecimalMapper(keyColumn);
        } else if (keyType.isAssignableFrom(Boolean.class) || keyType.isAssignableFrom(boolean.class)) {
            f = new BooleanMapper(keyColumn);
        } else if (keyType.isAssignableFrom(byte[].class)) {
            f = new ByteArrayMapper(keyColumn);
        } else if (keyType.isAssignableFrom(Short.class) || keyType.isAssignableFrom(short.class)) {
            f = new ShortMapper(keyColumn);
        } else if (keyType.isAssignableFrom(float.class) || keyType.isAssignableFrom(Float.class)) {
            f = new FloatMapper(keyColumn);
        } else if (keyType.isAssignableFrom(Double.class) || keyType.isAssignableFrom(double.class)) {
            f = new DoubleMapper(keyColumn);
        } else if (keyType.isAssignableFrom(Byte.class) || keyType.isAssignableFrom(byte.class)) {
            f = new ByteMapper(keyColumn);
        } else if (keyType.isAssignableFrom(URL.class)) {
            f = new URLMapper(keyColumn);
        } else if (keyType.isAssignableFrom(Integer.class) || keyType.isAssignableFrom(int.class)) {
            f = new IntegerMapper(keyColumn);
        } else if (keyType.isAssignableFrom(Long.class) || keyType.isAssignableFrom(long.class)) {
            f = new LongMapper(keyColumn);
        } else if (keyType.isAssignableFrom(Timestamp.class)) {
            f = new TimestampMapper(keyColumn);
        } else if (keyType.isAssignableFrom(String.class)) {
            f = new StringMapper(keyColumn);
        } else {
            f = new LongMapper(keyColumn);
        }
        return f;
    }
}
