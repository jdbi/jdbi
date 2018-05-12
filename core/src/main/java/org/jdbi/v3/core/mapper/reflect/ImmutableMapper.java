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

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.getColumnNames;

/**
 * Maps to an Interface with googles Immutable libary.
 */
public class ImmutableMapper<T> implements RowMapper<T> {

    static final String DEFAULT_PREFIX = "";

    /**
     * Returns a mapper factory that maps to the given immutable class
     *
     * @param type the mapped class
     * @return a mapper factory that maps to the given immutable class
     */
    public static RowMapperFactory factory(Class<?> type) {
        return RowMapperFactory.of(type, ImmutableMapper.of(type));
    }

    /**
     * Returns a mapper factory that maps to the given immutable class
     *
     * @param type   the mapped class
     * @param prefix the column name prefix for each mapped immutable property
     * @return a mapper factory that maps to the given immutable class
     */
    public static RowMapperFactory factory(Class<?> type, String prefix) {
        return RowMapperFactory.of(type, ImmutableMapper.of(type, prefix));
    }

    /**
     * Returns a mapper for the given immutable class
     *
     * @param <T>  the type to find the mapper for
     * @param type the mapped class
     * @return a mapper for the given immutable class
     */
    public static <T> RowMapper<T> of(Class<T> type) {
        return ImmutableMapper.of(type, DEFAULT_PREFIX);
    }

    /**
     * Returns a mapper for the given immutable class
     *
     * @param <T>    the type to find the mapper for
     * @param type   the mapped class
     * @param prefix the column name prefix for each mapped immutable property
     * @return a mapper for the given immutable class
     */
    public static <T> RowMapper<T> of(Class<T> type, String prefix) {
        return new ImmutableMapper<>(type, prefix);
    }

    private final Class<? extends T> type;
    private final String prefix;
    private final List<PropertyDescriptor> info;
    private final Map<PropertyDescriptor, ImmutableMapper<?>> nestedMappers = new ConcurrentHashMap<>();

    private ImmutableMapper(Class<T> type, String prefix) {

        this.type = type;
        this.prefix = prefix;

        info = getInformation(type);
    }

    @Override
    public T map(final ResultSet rs, final StatementContext ctx) throws SQLException {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<T> specialize(final ResultSet rs, final StatementContext ctx) throws SQLException {
        final List<String> columnNames = getColumnNames(rs);
        final List<ColumnNameMatcher> columnNameMatchers =
            ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();
        final List<String> unmatchedColumns = new ArrayList<>(columnNames);

        RowMapper<T> result = specialize0(rs, ctx, columnNames, columnNameMatchers, unmatchedColumns);

        if (ctx.getConfig(ReflectionMappers.class).isStrictMatching() &&
            unmatchedColumns.stream().anyMatch(col -> col.startsWith(prefix))) {

            throw new MappingException(String.format(
                "Mapping immutable type %s could not match properties for columns: %s",
                type.getSimpleName(),
                unmatchedColumns));
        }
        return result;
    }

    private RowMapper<T> specialize0(ResultSet rs,
                                     StatementContext ctx,
                                     List<String> columnNames,
                                     List<ColumnNameMatcher> columnNameMatchers,
                                     List<String> unmatchedColumns) {
        final List<RowMapper<?>> mappers = new ArrayList<>();
        final List<PropertyDescriptor> properties = new ArrayList<>();

        for (PropertyDescriptor descriptor : info) {
            Nested anno = Stream.of(descriptor.getReadMethod(), descriptor.getWriteMethod())
                .filter(Objects::nonNull)
                .map(m -> m.getAnnotation(Nested.class))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

            if (anno == null) {
                String paramName = prefix + paramName(descriptor);

                findColumnIndex(paramName, columnNames, columnNameMatchers, () -> debugName(descriptor))
                    .ifPresent(index -> {
                        Type type = descriptor.getReadMethod().getGenericReturnType();
                        ColumnMapper<?> mapper = ctx.findColumnMapperFor(type)
                            .orElse((r, n, c) -> r.getObject(n));

                        mappers.add(new SingleColumnMapper<>(mapper, index + 1));
                        properties.add(descriptor);

                        unmatchedColumns.remove(columnNames.get(index));
                    });
            } else {
                String nestedPrefix = prefix + anno.value();

                RowMapper<?> nestedMapper = nestedMappers
                    .computeIfAbsent(descriptor, d -> new ImmutableMapper(d.getPropertyType(), nestedPrefix))
                    .specialize0(rs, ctx, columnNames, columnNameMatchers, unmatchedColumns);

                mappers.add(nestedMapper);
                properties.add(descriptor);
            }
        }

        if (mappers.isEmpty() && columnNames.size() > 0) {
            throw new MappingException(String.format("Mapping immutable type %s " +
                "didn't find any matching columns in result set", type));
        }

        Constructor<MethodHandles.Lookup> constructorTemp = null;
        try {
            constructorTemp = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructorTemp.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        final Constructor<MethodHandles.Lookup> constructor = constructorTemp;

        return (r, c) -> {
            final Map<Method, Object> invocations = new HashMap<>();
            for (int i = 0; i < mappers.size(); i++) {
                RowMapper<?> mapper = mappers.get(i);
                PropertyDescriptor property = properties.get(i);
                Object value = mapper.map(r, ctx);
                invocations.put(property.getReadMethod(), value);
            }

            return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{type}, new InvocationHandler() {
                private final Map<Method, Object> invocationMap = invocations;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Object returnObj = invocationMap.get(method);
                    if (returnObj == null) {
                        if (method.isDefault() && constructor != null) {
                            final Class<?> declaringClass = method.getDeclaringClass();
                            return constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
                                .unreflectSpecial(method, declaringClass)
                                .bindTo(proxy)
                                .invokeWithArguments(args);
                        } else {
                            Class<?> returnType = method.getReturnType();
                            if (returnType.equals(Map.class)) {
                                return new HashMap<>(); // TODO IMMUTABLE MAP
                            }
                            if (returnType.equals(List.class)) {
                                return new ArrayList<>(); // TODO IMMUTABLE LIST
                            }
                            if (returnType.equals(Set.class)) {
                                return new HashSet<>(); // TODO IMMUTABLE SET
                            }
                            throw new UnsupportedOperationException(method.getName());
                        }
                    }
                    return invocationMap.get(method);
                }
            });
        };
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

    private String debugName(PropertyDescriptor descriptor) {
        return String.format("%s.%s", type.getSimpleName(), descriptor.getName());
    }

    private List<PropertyDescriptor> getInformation(Class<T> immutable) {
        List<Method> getter = Arrays.asList(immutable.getMethods());

        return getter.stream().map(g -> {
            String rawName = getRawName(g.getName());
            try {
                return new PropertyDescriptor(rawName,
                    g,
                    null);
            } catch (IntrospectionException e) {
                throw new MappingException(e);
            }
        }).collect(Collectors.toList());
    }

    private String getRawName(String name) {
        if (name.startsWith("get")) {
            if (Character.isUpperCase(name.charAt(3))) {
                return Character.toLowerCase(name.charAt(3)) + name.substring(4);
            }
        }
        if (name.startsWith("is")) {
            if (Character.isUpperCase(name.charAt(2))) {
                return Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }
        }
        return name;
    }
}
