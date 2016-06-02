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
package org.jdbi.v3.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.ColumnName;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.Types;
import org.jdbi.v3.util.bean.ColumnNameMappingStrategy;

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
    private final Constructor<T> constructor;
    private final ConcurrentMap<List<String>, ObjectCreator<T>> creatorCache = new ConcurrentHashMap<>();
    private final Collection<ColumnNameMappingStrategy> nameMappingStrategies;

    private ConstructorMapper(Constructor<T> constructor, Collection<ColumnNameMappingStrategy> nameMappingStrategies)
    {
        this.constructor = constructor;
        this.nameMappingStrategies = Collections.unmodifiableList(new ArrayList<>(nameMappingStrategies));
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

        ObjectCreator<T> creator = creatorCache.computeIfAbsent(columnNames, c -> createCreator(c, metadata, ctx));
        return creator.create(rs);
    }

    private ObjectCreator<T> createCreator(List<String> columnNames, ResultSetMetaData metadata, StatementContext ctx) {
        final int length = columnNames.size();

        if (length != constructor.getParameterCount()) {
            throw new IllegalStateException(length + " columns in result set, but constructor takes " + constructor.getParameterCount());
        }

        final int[] columnMap = new int[length];
        final ColumnMapper<?>[] mappers = new ColumnMapper<?>[length];

        for (int i = 0; i < length; i++) {
            final Type type = constructor.getGenericParameterTypes()[i];
            final int paramIndex = parameterIndexForColumn(columnNames.get(i));

            if (mappers[paramIndex] != null) {
                throw new IllegalArgumentException("Column named " + columnNames.get(i) + " maps to multiple parameters");
            }

            mappers[paramIndex] = ctx.findColumnMapperFor(type).orElseThrow(() ->
                new IllegalArgumentException("Could not find column mapper for type '" + type + "' of parameter for constructor " + constructor));
            columnMap[i] = paramIndex;
        }

        return rs -> {
            final Object[] params = new Object[length];
            for (int i = 0; i < length; i++) {
                params[columnMap[i]] = mappers[i].map(rs, i + 1, ctx);
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

    private int parameterIndexForColumn(String columnName)
    {
        for (int i = 0; i < constructor.getParameterCount(); i++) {
            for (ColumnNameMappingStrategy strategy : nameMappingStrategies) {
                if (strategy.nameMatches(paramName(constructor.getParameters()[i]), columnName)) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Constructor " + constructor + " has no parameter for '" + columnName +
                "'. Are parameter names compiled into your class?");
    }

    private static String paramName(Parameter parameter) {
        ColumnName dbName = parameter.getAnnotation(ColumnName.class);
        if (dbName != null) {
            return dbName.value();
        }
        return parameter.getName();
    }

    interface ObjectCreator<T> {
        T create(ResultSet rs) throws SQLException;
    }


    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> guessConstructor(Class<T> type) {
        final Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length != 1) {
            throw new IllegalArgumentException(type + " must have exactly one constructor, or specify it explicitly");
        }
        return (Constructor<T>) constructors[0];
    }

    public static RowMapperFactory factoryFor(Constructor<?> constructor) {
        return factoryFor(constructor, BeanMapper.DEFAULT_STRATEGIES);
    }

    public static RowMapperFactory factoryFor(Class<?> clazz) {
        return factoryFor(clazz, BeanMapper.DEFAULT_STRATEGIES);
    }

    public static RowMapperFactory factoryFor(Class<?> clazz, Collection<ColumnNameMappingStrategy> nameMappingStrategies) {
        return factoryFor(guessConstructor(clazz), nameMappingStrategies);
    }

    public static RowMapperFactory factoryFor(Constructor<?> constructor, Collection<ColumnNameMappingStrategy> nameMappingStrategies) {
        final ConstructorMapper<?> mapper = new ConstructorMapper<>(constructor, nameMappingStrategies);
        return new RowMapperFactory() {
            @Override
            public Optional<RowMapper<?>> build(Type type, StatementContext ctx) {
                return Types.getErasedType(type) == constructor.getDeclaringClass() ? Optional.of(mapper) : Optional.empty();
            }
        };
    }
}
