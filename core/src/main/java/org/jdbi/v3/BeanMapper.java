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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.tweak.ResultSetMapper;

/**
 * A result set mapper which maps the fields in a statement into a JavaBean. The default implementation will perform a
 * case insensitive mapping between the bean property names and the column labels, also considering camel-case to
 * underscores conversion. This uses the JDK's built in bean mapping facilities, so it does not support nested
 * properties.
 */
public class BeanMapper<T> implements ResultSetMapper<T>
{
    private final Class<T> type;
    private final Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor>();

    public BeanMapper(Class<T> type)
    {
        this.type = type;

        try
        {
            BeanInfo info = Introspector.getBeanInfo(type);

            Locale locale = getLocale();

            for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
                    Collection<String> columnNames = mapToColumnNames(descriptor.getName());
                    if (columnNames == null) {
                        continue;
                    }
                    for (String columnName : columnNames) {
                        properties.put(columnName.toLowerCase(locale), descriptor);
                    }
            }
        }
        catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Maps a property name to possible column names candidates. This default implementation will map the property name
     * as-is, and converted from the camel-case syntax to an underscore separated name.
     * <p>
     * Processing of the returned mappings will be case insensitive. The mappings are not expected to change across
     * invocations.
     *
     * @param propertyName the bean property name
     *
     * @return a collection holding the possible column names (mutable in the default implementation)
     */
    protected Collection<String> mapToColumnNames(String propertyName)
    {
        Locale locale = getLocale();

        List<String> columnName = new ArrayList<String>(5);

        // Add the bean property name as-is.
        columnName.add(propertyName);

        // Convert the property name from camel-case to underscores syntax. Freely adapted from Spring
        // BeanPropertyRowMapper.
        StringBuilder propertyNameWithUnderscores = new StringBuilder();
        propertyNameWithUnderscores.append(propertyName.substring(0, 1));
        for (int i = 1; i < propertyName.length(); i++) {
            // Do case comparison using strings rather than chars (avoid to deal with non-BMP char handling).
            String s = propertyName.substring(i, i + 1);
            String slc = s.toLowerCase(locale);
            if (!s.equals(slc)) {
                // Different cases: tokenize.
                propertyNameWithUnderscores.append("_").append(slc);
            }
            else {
                propertyNameWithUnderscores.append(s);
            }
        }
        columnName.add(propertyNameWithUnderscores.toString());

        return columnName;
    }

    /**
     * Gets the locale used to manipulate the Bean properties name. This locale is useful, for example, when doing
     * case conversion. The locale is expected to be constant across method invocations. By default the {@code US}
     * locale will be used.
     *
     * @return the target locale, never {@code null}
     */
    protected Locale getLocale() {
        return Locale.US;
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

