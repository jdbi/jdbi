package org.skife.jdbi.v2;

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

import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


/**
 * A result set mapper which maps the fields in a statement into a JavaBean. This uses
 * the reflection to set the fields on the bean including its super class fields, it does not support nested properties.
 */
public class ReflectionBeanMapper<T> implements ResultSetMapper<T>
{
    private final Class<T> type;
    private final Map<String, Field> properties = new HashMap<String, Field>();

    public ReflectionBeanMapper(Class<T> type)
    {
        this.type = type;
        cacheAllFieldsIncludingSuperClass(type);
    }

    private void cacheAllFieldsIncludingSuperClass(Class<T> type) {
        Class aClass = type;
        while(aClass != null) {
            for (Field field : aClass.getDeclaredFields()) {
                properties.put(field.getName().toLowerCase(), field);
            }
            aClass = aClass.getSuperclass();
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public T map(int row, ResultSet rs, StatementContext ctx)
            throws SQLException
    {
        T bean;
        try {
            bean = type.newInstance();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(String.format("A bean, %s, was mapped " +
                    "which was not instantiable", type.getName()), e);
        }

        ResultSetMetaData metadata = rs.getMetaData();

        for (int i = 1; i <= metadata.getColumnCount(); ++i) {
            String name = metadata.getColumnLabel(i).toLowerCase();

            Field field = properties.get(name);

            if (field != null) {
                Class type = field.getType();

                Object value;
                ResultColumnMapper mapper = ctx.columnMapperFor(type);
                if (mapper != null) {
                    value = mapper.mapColumn(rs, i, ctx);
                }
                else {
                    value = rs.getObject(i);
                }

                try
                {
                    field.setAccessible(true);
                    field.set(bean, value);
                }
                catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(String.format("Unable to access " +
                            "property, %s", name), e);
                }
            }
        }

        return bean;
    }
}

