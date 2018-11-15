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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.anyColumnsStartWithPrefix;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.getColumnNames;
import static org.jdbi.v3.core.qualifier.Qualifiers.getQualifiers;

/**
 * A row mapper which maps the columns in a statement into a JavaBean. The default
 * implementation will perform a case insensitive mapping between the bean property
 * names and the column labels, also considering camel-case to underscores conversion.
 * This uses the JDK's built in bean mapping facilities, so it does not support nested
 * properties.
 *
 * The mapped class must have a default constructor.
 */
public class BeanMapper<T> implements RowMapper<T> {
    private static final String DEFAULT_PREFIX = "";

    private static final String NO_MATCHING_COLUMNS =
        "Mapping bean type %s didn't find any matching columns in result set";

    private static final String UNMATCHED_COLUMNS_STRICT =
        "Mapping bean type %s could not match properties for columns: %s";

    private static final String TYPE_NOT_INSTANTIABLE =
        "A bean, %s, was mapped which was not instantiable";

    private static final String MISSING_SETTER =
        "No appropriate method to write property %s";

    private static final String SETTER_NOT_ACCESSIBLE =
        "Unable to access setter for property, %s";

    private static final String INVOCATION_TARGET_EXCEPTION =
        "Invocation target exception trying to invoker setter for the %s property";

    private static final String REFLECTION_ILLEGAL_ARGUMENT_EXCEPTION =
        "Write method of %s for property %s is not compatible with the value passed";

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory factory(Class<?> type) {
        return RowMapperFactory.of(type, BeanMapper.of(type));
    }

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped bean property
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory factory(Class<?> type, String prefix) {
        return RowMapperFactory.of(type, BeanMapper.of(type, prefix));
    }

    /**
     * Returns a mapper for the given bean class
     *
     * @param <T> the type to find the mapper for
     * @param type the mapped class
     * @return a mapper for the given bean class
     */
    public static <T> RowMapper<T> of(Class<T> type) {
        return BeanMapper.of(type, DEFAULT_PREFIX);
    }

    /**
     * Returns a mapper for the given bean class
     *
     * @param <T> the type to find the mapper for
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped bean property
     * @return a mapper for the given bean class
     */
    public static <T> RowMapper<T> of(Class<T> type, String prefix) {
        return new BeanMapper<>(type, prefix);
    }

    private final Class<T> type;
    private final String prefix;
    private final BeanInfo info;
    private final Map<PropertyDescriptor, BeanMapper<?>> nestedMappers = new ConcurrentHashMap<>();

    private BeanMapper(Class<T> type, String prefix) {
        this.type = type;
        this.prefix = prefix.toLowerCase();
        try {
            info = Introspector.getBeanInfo(type);
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        final List<String> columnNames = getColumnNames(rs);
        final List<ColumnNameMatcher> columnNameMatchers =
                ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();
        final List<String> unmatchedColumns = new ArrayList<>(columnNames);

        RowMapper<T> result = specialize0(ctx, columnNames, columnNameMatchers, unmatchedColumns)
            .orElseThrow(() -> new IllegalArgumentException(String.format(NO_MATCHING_COLUMNS, type)));

        if (ctx.getConfig(ReflectionMappers.class).isStrictMatching()
            && anyColumnsStartWithPrefix(unmatchedColumns, prefix, columnNameMatchers)) {

            throw new IllegalArgumentException(
                String.format(UNMATCHED_COLUMNS_STRICT, type.getSimpleName(), unmatchedColumns));
        }

        return result;
    }

    private Optional<RowMapper<T>> specialize0(StatementContext ctx,
                                               List<String> columnNames,
                                               List<ColumnNameMatcher> columnNameMatchers,
                                               List<String> unmatchedColumns) {
        final List<RowMapper<?>> mappers = new ArrayList<>();
        final List<PropertyDescriptor> properties = new ArrayList<>();

        for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
            Method getter = descriptor.getReadMethod();
            Method setter = descriptor.getWriteMethod();
            Nested anno = Stream.of(getter, setter)
                .filter(Objects::nonNull)
                .map(m -> m.getAnnotation(Nested.class))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

            if (anno == null) {
                String paramName = prefix + paramName(descriptor);

                findColumnIndex(paramName, columnNames, columnNameMatchers, () -> debugName(descriptor))
                    .ifPresent(index -> {
                        Parameter setterParam = Optional.ofNullable(setter)
                            .map(m -> m.getParameterCount() > 0 ? m.getParameters()[0] : null)
                            .orElse(null);

                        QualifiedType type = QualifiedType.of(
                            propertyType(descriptor),
                            getQualifiers(getter, setter, setterParam));
                        ColumnMapper<?> mapper = ctx.findColumnMapperFor(type)
                            .orElse((r, n, c) -> r.getObject(n));

                        mappers.add(new SingleColumnMapper<>(mapper, index + 1));
                        properties.add(descriptor);

                        unmatchedColumns.remove(columnNames.get(index));
                    });
            } else {
                String nestedPrefix = prefix + anno.value();
                if (anyColumnsStartWithPrefix(columnNames, nestedPrefix, columnNameMatchers)) {
                    nestedMappers
                        .computeIfAbsent(descriptor, d -> new BeanMapper<>(d.getPropertyType(), nestedPrefix))
                        .specialize0(ctx, columnNames, columnNameMatchers, unmatchedColumns)
                        .ifPresent(nestedMapper -> {
                            mappers.add(nestedMapper);
                            properties.add(descriptor);
                        });
                }
            }
        }

        if (mappers.isEmpty() && !columnNames.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((r, c) -> {
            T bean = construct();

            for (int i = 0; i < mappers.size(); i++) {
                RowMapper<?> mapper = mappers.get(i);
                PropertyDescriptor property = properties.get(i);

                Object value = mapper.map(r, ctx);

                writeProperty(bean, property, value);
            }

            return bean;
        });
    }

    private static String paramName(PropertyDescriptor descriptor) {
        return Stream.of(descriptor.getReadMethod(), descriptor.getWriteMethod())
                .filter(Objects::nonNull)
                .map(method -> method.getAnnotation(ColumnName.class))
                .filter(Objects::nonNull)
                .map(ColumnName::value)
                .findFirst()
                .orElseGet(descriptor::getName);
    }

    private static Type propertyType(PropertyDescriptor descriptor) {
        return Optional.ofNullable(descriptor.getReadMethod()).map(Method::getGenericReturnType)
                .orElseGet(() -> descriptor.getWriteMethod().getGenericParameterTypes()[0]);
    }

    private String debugName(PropertyDescriptor descriptor) {
        return String.format("%s.%s", type.getSimpleName(), descriptor.getName());
    }

    private T construct() {
        try {
            return type.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format(TYPE_NOT_INSTANTIABLE, type.getName()), e);
        }
    }

    private static void writeProperty(Object bean, PropertyDescriptor property, Object value) {
        try {
            Method writeMethod = property.getWriteMethod();
            if (writeMethod == null) {
                throw new IllegalArgumentException(String.format(MISSING_SETTER, property.getName()));
            }
            writeMethod.invoke(bean, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format(SETTER_NOT_ACCESSIBLE, property.getName()), e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(String.format(INVOCATION_TARGET_EXCEPTION, property.getName()), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format(REFLECTION_ILLEGAL_ARGUMENT_EXCEPTION,
                property.getPropertyType(), property.getName()), e);
        }
    }
}
