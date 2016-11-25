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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.util.bean.ColumnNameMappingStrategy;

/**
 * A row mapper which maps the fields in a result set into a constructor. The default implementation will perform a
 * case insensitive mapping between the constructor parameter names and the column labels,
 * also considering camel-case to underscores conversion.
 *
 * Currently the constructor must have exactly the same number of columns as the result set, and
 * the mapping must be one-to-one.  These restrictions may be reconsidered at a later time.
 */
public class ConstructorMapper<T> implements RowMapper<T>
{
    /**
     * Use the only declared constructor to map a class.
     */
    public static RowMapperFactory of(Class<?> clazz) {
        return of(clazz, DEFAULT_PREFIX);
    }

    /**
     * Use the only declared constructor to map a class.
     */
    public static RowMapperFactory of(Class<?> clazz, String prefix) {
        return of(findOnlyConstructor(clazz), prefix);
    }

    /**
     * Use a {@code Constructor<T>} to map its declaring type.
     */
    public static RowMapperFactory of(Constructor<?> constructor) {
        return of(constructor, DEFAULT_PREFIX);
    }

    /**
     * Use a {@code Constructor<T>} to map its declaring type.
     */
    public static RowMapperFactory of(Constructor<?> constructor, String prefix) {
        final Class<?> type = constructor.getDeclaringClass();
        final RowMapper<?> mapper = new ConstructorMapper<>(constructor, prefix);
        return (t, ctx) -> t == type ? Optional.of(mapper) : Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> findOnlyConstructor(Class<T> type) {
        final Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length != 1) {
            throw new IllegalArgumentException(type + " must have exactly one constructor, or specify it explicitly");
        }
        return (Constructor<T>) constructors[0];
    }

    static final String DEFAULT_PREFIX = "";

    private final Constructor<T> constructor;
    private final ConcurrentMap<List<String>, Factory<T>> factoryCache = new ConcurrentHashMap<>();
    private final String prefix;

    private ConstructorMapper(Constructor<T> constructor,
                              String prefix)
    {
        this.constructor = constructor;
        this.prefix = prefix;
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx)
        throws SQLException
    {
        final ResultSetMetaData metadata = rs.getMetaData();
        final List<String> columnNames = new ArrayList<>(metadata.getColumnCount());

        for (int i = 1; i <= metadata.getColumnCount(); ++i) {
            columnNames.add(metadata.getColumnLabel(i));
        }

        Factory<T> factory = factoryCache.computeIfAbsent(columnNames, c -> createFactory(c, ctx));
        return factory.create(rs);
    }

    /**
     * Compute a memoized mapping from ResultSet column labels to Constructor argument position.
     */
    private Factory<T> createFactory(List<String> columnNames, StatementContext ctx) {
        final int columns = constructor.getParameterCount();

        if (columns > columnNames.size()) {
            throw new IllegalStateException(columnNames.size() +
                    " columns in result set, but constructor takes " +
                    constructor.getParameterCount());
        }

        List<ColumnNameMappingStrategy> nameMappingStrategies =
                ctx.getConfig(ReflectionMappers.class).getColumnNameMappingStrategies();

        final int[] columnMap = new int[columns];
        final ColumnMapper<?>[] mappers = new ColumnMapper<?>[columns];

        for (int i = 0; i < columns; i++) {
            final Type type = constructor.getGenericParameterTypes()[i];
            final String paramName = paramName(constructor.getParameters()[i]);
            final int columnIndex = columnIndexForParameter(columnNames, paramName, nameMappingStrategies);

            mappers[i] = ctx.findColumnMapperFor(type)
                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "Could not find column mapper for type '%s' of parameter '%s' for constructor '%s'",
                            type, paramName, constructor)));
            columnMap[i] = columnIndex;
        }

        return rs -> {
            final Object[] params = new Object[columns];
            for (int i = 0; i < columns; i++) {
                params[i] = mappers[i].map(rs, columnMap[i] + 1, ctx);
            }
            try {
                return constructor.newInstance(params);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                if (e.getCause() instanceof Error) {
                    throw (Error) e.getCause();
                }
                throw new RuntimeException(e);
            }
        };
    }

    private int columnIndexForParameter(List<String> columnNames,
                                        String parameterName,
                                        List<ColumnNameMappingStrategy> nameMappingStrategies)
    {
        int result = -1;
        for (int i = 0; i < columnNames.size(); i++) {
            String name = columnNames.get(i);
            if (prefix.length() > 0) {
                if (name.length() > prefix.length() &&
                        name.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    name = name.substring(prefix.length());
                }
                else {
                    continue;
                }
            }

            for (ColumnNameMappingStrategy strategy : nameMappingStrategies) {
                if (strategy.nameMatches(parameterName, name)) {
                    if (result >= 0) {
                        throw new IllegalArgumentException(String.format(
                                "Constructor '%s' parameter '%s' matches multiple " +
                                "columns: '%s' (%d) and '%s' (%d)", constructor,
                                parameterName, columnNames.get(result), result,
                                columnNames.get(i), i));
                    }
                    result = i;
                    break;
                }
            }
        }
        if (result >= 0) {
            return result;
        }
        throw new IllegalArgumentException("Constructor '" + constructor + "' parameter '" +
                parameterName +
                "' has no column in the result set.  Verify that the Java " +
                "compiler is configured to emit parameter names, " +
                "that your result set has the columns expected, " +
                "or annotate the parameter names explicitly with @ColumnName");
    }

    private static String paramName(Parameter parameter) {
        ColumnName dbName = parameter.getAnnotation(ColumnName.class);
        if (dbName != null) {
            return dbName.value();
        }
        return parameter.getName();
    }

    interface Factory<T> {
        T create(ResultSet rs) throws SQLException;
    }
}
