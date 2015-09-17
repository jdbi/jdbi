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
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * A result set mapper which maps the fields in a statement into a JavaBean. This uses
 * the JDK's built in bean mapping facilities, so it does not support nested properties.
 */
public class BeanMapper<T> implements ResultSetMapper<T>
{
    private final Class<T> type;
    private final Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor>();

    public BeanMapper(Class<T> type)
    {
        this.type = type;
        try {
            BeanInfo info = Introspector.getBeanInfo(type);

            for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
                properties.put(descriptor.getName().toLowerCase(), descriptor);
            }
        }
        catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
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

            PropertyDescriptor descriptor = properties.get(name);

            if (descriptor != null) {
                Class type = descriptor.getPropertyType();

                Object value;
                ResultColumnMapper mapper = ctx.columnMapperFor(type);
                if (mapper != null) {
                    value = mapper.mapColumn(rs, i, ctx);
                }
                else if (type.isEnum()) {
                    String str = rs.getString(i);
                    value = str == null ? null : Enum.valueOf(type, str);
                }
                else {
                    value = rs.getObject(i);
                }

                try
                {
                    descriptor.getWriteMethod().invoke(bean, value);
                }
                catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(String.format("Unable to access setter for " +
                                                                     "property, %s", name), e);
                }
                catch (InvocationTargetException e) {
                    throw new IllegalArgumentException(String.format("Invocation target exception trying to " +
                                                                     "invoker setter for the %s property", name), e);
                }
                catch (NullPointerException e) {
                    throw new IllegalArgumentException(String.format("No appropriate method to " +
                                                                     "write property %s", name), e);
                }

            }
        }

        return bean;
    }

}

