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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.mapper.reflect.internal.BeanPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoBuilder;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.meta.Beta;

import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.anyColumnsStartWithPrefix;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.getColumnNames;

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
    protected static final String DEFAULT_PREFIX = "";

    private static final String NO_MATCHING_COLUMNS =
        "Mapping bean type %s didn't find any matching columns in result set";

    private static final String UNMATCHED_COLUMNS_STRICT =
        "Mapping bean type %s could not match properties for columns: %s";

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

    protected final Class<T> type;
    protected final String prefix;
    private final PojoProperties<T> beanInfo;
    private final Map<PojoProperty<T>, BeanMapper<?>> nestedMappers = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    protected BeanMapper(Class<T> type, String prefix) {
        this(type, (PojoProperties<T>) BeanPropertiesFactory.propertiesFor(type), prefix);
    }

    BeanMapper(Class<T> type, PojoProperties<T> properties, String prefix) {
        this.type = type;
        this.beanInfo = properties;
        this.prefix = prefix.toLowerCase();
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
        final List<PojoProperty<T>> propList = new ArrayList<>();

        for (PojoProperty<T> property : beanInfo.getProperties().values()) {
            Nested anno = property.getAnnotation(Nested.class).orElse(null);

            if (anno == null) {
                String paramName = prefix + getName(property);

                findColumnIndex(paramName, columnNames, columnNameMatchers, () -> debugName(property))
                    .ifPresent(index -> {
                        ColumnMapper<?> mapper = ctx.findColumnMapperFor(property.getQualifiedType())
                            .orElseGet(() -> defaultColumnMapper(property));

                        mappers.add(new SingleColumnMapper<>(mapper, index + 1));
                        propList.add(property);

                        unmatchedColumns.remove(columnNames.get(index));
                    });
            } else {
                String nestedPrefix = prefix + anno.value();
                if (anyColumnsStartWithPrefix(columnNames, nestedPrefix, columnNameMatchers)) {
                    nestedMappers
                        .computeIfAbsent(property, d -> new BeanMapper<>(GenericTypes.getErasedType(d.getQualifiedType().getType()), nestedPrefix))
                        .specialize0(ctx, columnNames, columnNameMatchers, unmatchedColumns)
                        .ifPresent(nestedMapper -> {
                            mappers.add(nestedMapper);
                            propList.add(property);
                        });
                }
            }
        }

        if (mappers.isEmpty() && !columnNames.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((r, c) -> {
            final PojoBuilder<T> pojo = beanInfo.create();

            for (int i = 0; i < mappers.size(); i++) {
                RowMapper<?> mapper = mappers.get(i);
                PojoProperty<T> property = propList.get(i);

                Object value = mapper.map(r, ctx);

                pojo.set(property, value);
            }

            return pojo.build();
        });
    }

    protected ColumnMapper<?> defaultColumnMapper(PojoProperty<T> property) {
        return (r, n, c) -> r.getObject(n);
    }

    @Beta
    public PojoProperties<T> getBeanInfo() {
        return beanInfo;
    }

    private String getName(PojoProperty<T> property) {
        return property.getAnnotation(ColumnName.class)
                .map(ColumnName::value)
                .orElseGet(property::getName);
    }

    private String debugName(PojoProperty<T> p) {
        return String.format("%s.%s", type.getSimpleName(), p.getName());
    }
}
