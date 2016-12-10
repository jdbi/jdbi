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
package org.jdbi.v3.core.mapper.reflect;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;

/**
 * A row mapper which maps the columns in a statement into a JavaBean. The default
 * implementation will perform a case insensitive mapping between the bean property
 * names and the column labels, also considering camel-case to underscores conversion.
 * This uses the JDK's built in bean mapping facilities, so it does not support nested
 * properties.
 *
 * The mapped class must have a default constructor.
 */
public class BeanMapper<T> implements RowMapper<T>
{
    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory of(Class<?> type) {
        return RowMapperFactory.of(type, new BeanMapper<>(type));
    }

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped bean property
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory of(Class<?> type, String prefix) {
        return RowMapperFactory.of(type, new BeanMapper<>(type, prefix));
    }

    static final String DEFAULT_PREFIX = "";

    private final Class<T> type;
    private final String prefix;
    private final BeanInfo info;
    private final ConcurrentMap<String, Optional<PropertyDescriptor>> descriptorByColumnCache = new ConcurrentHashMap<>();

    public BeanMapper(Class<T> type)
    {
        this(type, DEFAULT_PREFIX);
    }

    public BeanMapper(Class<T> type, String prefix)
    {
        this.type = type;
        this.prefix = prefix;
        try
        {
            info = Introspector.getBeanInfo(type);
        }
        catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        List<Integer> columnNumbers = new ArrayList<>();
        List<ColumnMapper<?>> mappers = new ArrayList<>();
        List<PropertyDescriptor> properties = new ArrayList<>();

        ResultSetMetaData metadata = rs.getMetaData();
        List<ColumnNameMatcher> columnNameMatchers = ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();

        for (int i = 1; i <= metadata.getColumnCount(); ++i) {
            String name = metadata.getColumnLabel(i);

            if (prefix.length() > 0) {
                if (name.length() > prefix.length() &&
                        name.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    name = name.substring(prefix.length());
                }
                else {
                    continue;
                }
            }

            final Optional<PropertyDescriptor> maybeDescriptor =
                    descriptorByColumnCache.computeIfAbsent(name, n -> descriptorForColumn(n, columnNameMatchers));

            if (!maybeDescriptor.isPresent()) {
                continue;
            }

            final PropertyDescriptor descriptor = maybeDescriptor.get();
            final Type type = descriptor.getReadMethod().getGenericReturnType();
            final ColumnMapper<?> mapper = ctx.findColumnMapperFor(type)
                    .orElse((r, n, c) -> r.getObject(n));

            columnNumbers.add(i);
            mappers.add(mapper);
            properties.add(descriptor);
        }

        return (r, c) -> {
            T bean;
            try {
                bean = type.newInstance();
            }
            catch (Exception e) {
                throw new IllegalArgumentException(String.format("A bean, %s, was mapped " +
                        "which was not instantiable", type.getName()), e);
            }

            for (int i = 0; i < columnNumbers.size(); i++) {
                int columnNumber = columnNumbers.get(i);
                ColumnMapper<?> mapper = mappers.get(i);
                PropertyDescriptor property = properties.get(i);

                Object value = mapper.map(r, columnNumber, ctx);
                try {
                    property.getWriteMethod().invoke(bean, value);
                }
                catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(String.format("Unable to access setter for " +
                            "property, %s", property.getName()), e);
                }
                catch (InvocationTargetException e) {
                    throw new IllegalArgumentException(String.format("Invocation target exception trying to " +
                            "invoker setter for the %s property", property.getName()), e);
                }
                catch (NullPointerException e) {
                    throw new IllegalArgumentException(String.format("No appropriate method to " +
                            "write property %s", property.getName()), e);
                }
            }

            return bean;
        };
    }

    private Optional<PropertyDescriptor> descriptorForColumn(String columnName,
                                                             List<ColumnNameMatcher> columnNameMatchers)
    {
        for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
            String paramName = paramName(descriptor);
            for (ColumnNameMatcher strategy : columnNameMatchers) {
                if (strategy.columnNameMatches(columnName, paramName)) {
                    return Optional.of(descriptor);
                }
            }
        }
        return Optional.empty();
    }

    private String paramName(PropertyDescriptor descriptor)
    {
        return Stream.of(descriptor.getReadMethod(), descriptor.getWriteMethod())
                .filter(Objects::nonNull)
                .map(method -> method.getAnnotation(ColumnName.class))
                .filter(Objects::nonNull)
                .map(ColumnName::value)
                .findFirst()
                .orElseGet(descriptor::getName);
    }
}
