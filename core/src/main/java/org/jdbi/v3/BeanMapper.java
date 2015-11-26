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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.reflect.TypeToken;
import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.tweak.ResultSetMapper;
import org.jdbi.v3.util.bean.ColumnNameMappingStrategy;
import org.jdbi.v3.util.bean.CaseInsensitiveColumnNameStrategy;
import org.jdbi.v3.util.bean.SnakeCaseColumnNameStrategy;

/**
 * A result set mapper which maps the fields in a statement into a JavaBean. The default implementation will perform a
 * case insensitive mapping between the bean property names and the column labels, also considering camel-case to
 * underscores conversion. This uses the JDK's built in bean mapping facilities, so it does not support nested
 * properties.
 */
public class BeanMapper<T> implements ResultSetMapper<T>
{
    static final Collection<ColumnNameMappingStrategy> DEFAULT_STRATEGIES =
            Collections.unmodifiableList(Arrays.asList(
                    CaseInsensitiveColumnNameStrategy.INSTANCE,
                    SnakeCaseColumnNameStrategy.INSTANCE
            ));

    private final Class<T> type;
    private final BeanInfo info;
    private final ConcurrentMap<String, Optional<PropertyDescriptor>> descriptorByColumnCache = new ConcurrentHashMap<>();
    private final Collection<ColumnNameMappingStrategy> nameMappingStrategies;

    public BeanMapper(Class<T> type)
    {
        this(type, DEFAULT_STRATEGIES);
    }

    public BeanMapper(Class<T> type, Collection<ColumnNameMappingStrategy> nameMappingStrategies)
    {
        this.type = type;
        this.nameMappingStrategies = Collections.unmodifiableList(new ArrayList<>(nameMappingStrategies));
        try
        {
            info = Introspector.getBeanInfo(type);
        }
        catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
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
            String name = metadata.getColumnLabel(i);

            final Optional<PropertyDescriptor> maybeDescriptor =
                    descriptorByColumnCache.computeIfAbsent(name, this::descriptorForColumn);

            if (!maybeDescriptor.isPresent()) {
                continue;
            }

            final PropertyDescriptor descriptor = maybeDescriptor.get();
            final TypeToken<?> type = TypeToken.of(descriptor.getReadMethod().getGenericReturnType());
            final Object value;
            final ResultColumnMapper<?> mapper = ctx.columnMapperFor(type);

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

        return bean;
    }

    private Optional<PropertyDescriptor> descriptorForColumn(String columnName)
    {
        for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
            for (ColumnNameMappingStrategy strategy : nameMappingStrategies) {
                if (strategy.nameMatches(descriptor.getName(), columnName)) {
                    return Optional.of(descriptor);
                }
            }
        }
        return Optional.empty();
    }
}

