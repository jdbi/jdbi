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
package org.jdbi.v3.core.mapper.reflect.internal;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.leangen.geantyref.GenericTypeReflector;
import org.jdbi.v3.core.annotation.Unmappable;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jdbi.v3.core.mapper.reflect.ColumnNameMatcher;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoBuilder;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.anyColumnsStartWithPrefix;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.getColumnNames;

/** This class is the future home of BeanMapper functionality. */
public class PojoMapper<T> implements RowMapper<T> {

    private static final String NO_MATCHING_COLUMNS =
        "Mapping bean %s didn't find any matching columns in result set";

    private static final String UNMATCHED_COLUMNS_STRICT =
        "Mapping bean %s could not match properties for columns: %s";

    protected boolean strictColumnTypeMapping = true; // this should be default (only?) behavior but that's a breaking change
    protected final Type type;
    protected final String prefix;
    private final Map<PojoProperty<T>, PojoMapper<?>> nestedMappers = new ConcurrentHashMap<>();

    public PojoMapper(Type type, String prefix) {
        this.type = type;
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
                String.format(UNMATCHED_COLUMNS_STRICT, type, unmatchedColumns));
        }

        return result;
    }

    private Optional<RowMapper<T>> specialize0(StatementContext ctx,
                                               List<String> columnNames,
                                               List<ColumnNameMatcher> columnNameMatchers,
                                               List<String> unmatchedColumns) {
        final List<RowMapper<?>> mappers = new ArrayList<>();
        final List<PojoProperty<T>> propList = new ArrayList<>();

        for (PojoProperty<T> property : getProperties(ctx.getConfig()).getProperties().values()) {
            Nested anno = property.getAnnotation(Nested.class).orElse(null);
            if (property.getAnnotation(Unmappable.class).map(Unmappable::value).orElse(false)) {
                continue;
            }

            if (anno == null) {
                String paramName = prefix + getName(property);

                findColumnIndex(paramName, columnNames, columnNameMatchers, () -> debugName(property))
                    .ifPresent(index -> {
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        ColumnMapper<?> mapper = ctx.findColumnMapperFor(box(property.getQualifiedType()))
                            .orElseGet(() -> (ColumnMapper) defaultColumnMapper(property));

                        mappers.add(new SingleColumnMapper<>(mapper, index + 1));
                        propList.add(property);
                        unmatchedColumns.remove(columnNames.get(index));
                    });
            } else {
                String nestedPrefix = prefix + anno.value();
                if (anyColumnsStartWithPrefix(columnNames, nestedPrefix, columnNameMatchers)) {
                    nestedMappers
                        .computeIfAbsent(property, d -> createNestedMapper(ctx, d, nestedPrefix))
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
            final PojoBuilder<T> pojo = getProperties(c.getConfig()).create();

            for (int i = 0; i < mappers.size(); i++) {
                RowMapper<?> mapper = mappers.get(i);
                PojoProperty<T> property = propList.get(i);

                Object value = mapper.map(r, ctx);

                if (value != null) {
                    pojo.set(property, value);
                }
            }

            return pojo.build();
        });
    }

    private static QualifiedType<?> box(QualifiedType<?> qualifiedType) {
        return qualifiedType.mapType(t -> Optional.of(GenericTypeReflector.box(t))).get();
    }

    @SuppressWarnings("unchecked")
    protected PojoProperties<T> getProperties(ConfigRegistry config) {
        return (PojoProperties<T>) config.get(PojoTypes.class).findFor(type)
                .orElseThrow(() -> new UnableToProduceResultException("Couldn't find properties for " + type));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) // Type loses <T>
    protected PojoMapper<?> createNestedMapper(StatementContext ctx, PojoProperty<T> property, String nestedPrefix) {
        final Type propertyType = property.getQualifiedType().getType();
        return new PojoMapper(
                GenericTypes.getErasedType(propertyType),
                nestedPrefix);
    }

    private ColumnMapper<?> defaultColumnMapper(PojoProperty<T> property) {
        if (strictColumnTypeMapping) {
            throw new NoSuchMapperException(String.format(
                    "Couldn't find mapper for property '%s' of type '%s' from %s", property.getName(), property.getQualifiedType(), type));
        }
        return (r, n, c) -> r.getObject(n);
    }

    private String getName(PojoProperty<T> property) {
        return property.getAnnotation(ColumnName.class)
                .map(ColumnName::value)
                .orElseGet(property::getName);
    }

    private String debugName(PojoProperty<T> p) {
        return String.format("%s.%s", type, p.getName());
    }
}
