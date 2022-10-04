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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.annotation.internal.JdbiAnnotations;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.PropagateNull;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jdbi.v3.core.mapper.reflect.ColumnNameMatcher;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoBuilder;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

import static java.lang.String.format;

import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.addPropertyNamePrefix;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.anyColumnsStartWithPrefix;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.getColumnNames;

/** This class is the future home of BeanMapper functionality. */
public class PojoMapper<T> implements RowMapper<T> {

    protected boolean strictColumnTypeMapping = true; // this should be default (only?) behavior but that's a breaking change
    protected final Type type;
    protected final String prefix;
    private final Map<PojoProperty<T>, PojoMapper<?>> nestedMappers = new ConcurrentHashMap<>();

    public PojoMapper(Type type, String prefix) {
        this.type = type;
        this.prefix = prefix;
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

        RowMapper<T> result = createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns)
            .orElseThrow(() -> new IllegalArgumentException(format("Mapping bean %s didn't find any matching columns in result set", type)));

        if (ctx.getConfig(ReflectionMappers.class).isStrictMatching()
            && anyColumnsStartWithPrefix(unmatchedColumns, prefix, columnNameMatchers)) {

            throw new IllegalArgumentException(
                format("Mapping bean %s could not match properties for columns: %s", type, unmatchedColumns));
        }

        return result;
    }

    private Optional<RowMapper<T>> createSpecializedRowMapper(StatementContext ctx,
        List<String> columnNames,
        List<ColumnNameMatcher> columnNameMatchers,
        List<String> unmatchedColumns) {
        final List<PropertyData<T>> propList = new ArrayList<>();

        for (PojoProperty<T> property : getProperties(ctx.getConfig()).getProperties().values()) {
            Nested nested = property.getAnnotation(Nested.class).orElse(null);
            if (!JdbiAnnotations.isMapped(property)) {
                continue;
            }

            if (nested == null) {
                String paramName = addPropertyNamePrefix(prefix, getName(property));

                findColumnIndex(paramName, columnNames, columnNameMatchers, () -> debugName(property))
                    .ifPresent(index -> {
                        ColumnMapper<?> mapper = ctx.findColumnMapperFor(property.getQualifiedType().mapType(GenericTypes::box))
                            .orElseGet(() -> {
                                if (strictColumnTypeMapping) {
                                    throw new NoSuchMapperException(format(
                                        "Couldn't find mapper for property '%s' of type '%s' from %s", property.getName(), property.getQualifiedType(), type));
                                }
                                return ColumnMapper.getDefaultColumnMapper();
                            });

                        propList.add(new PropertyData<>(property, new SingleColumnMapper<>(mapper, index + 1)));
                        unmatchedColumns.remove(columnNames.get(index));
                    });
            } else {
                String nestedPrefix = addPropertyNamePrefix(prefix, nested.value());
                if (anyColumnsStartWithPrefix(columnNames, nestedPrefix, columnNameMatchers)) {
                    nestedMappers
                        .computeIfAbsent(property, d -> createNestedMapper(ctx, d, nestedPrefix))
                        .createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns)
                        .ifPresent(nestedMapper ->
                            propList.add(new PropertyData<>(property, nestedMapper)));
                }
            }
        }

        if (propList.isEmpty() && !columnNames.isEmpty()) {
            return Optional.empty();
        }

        propList.sort(Comparator.comparing(p -> p.propagateNull ? 1 : 0));

        RowMapper<T> boundMapper = new BoundPojoMapper(propList);
        OptionalInt propagateNullColumnIndex = locatePropagateNullColumnIndex(columnNames, columnNameMatchers);

        if (propagateNullColumnIndex.isPresent()) {
            return Optional.of(new NullDelegatingMapper<>(propagateNullColumnIndex.getAsInt() + 1, boundMapper));
        } else {
            return Optional.of(boundMapper);
        }
    }

    private OptionalInt locatePropagateNullColumnIndex(List<String> columnNames, List<ColumnNameMatcher> columnNameMatchers) {
        Optional<String> propagateNullColumn =
            Optional.ofNullable(GenericTypes.getErasedType(type).getAnnotation(PropagateNull.class))
                .map(PropagateNull::value)
                .map(name -> addPropertyNamePrefix(prefix, name));

        if (!propagateNullColumn.isPresent()) {
            return OptionalInt.empty();
        }

        return findColumnIndex(propagateNullColumn.get(), columnNames, columnNameMatchers, propagateNullColumn::get);
    }

    @SuppressWarnings("unchecked")
    protected PojoProperties<T> getProperties(ConfigRegistry config) {
        return (PojoProperties<T>) config.get(PojoTypes.class).findFor(type)
            .orElseThrow(() -> new UnableToProduceResultException("Couldn't find properties for " + type));
    }

    @SuppressWarnings("rawtypes") // Type loses <T>
    protected PojoMapper<?> createNestedMapper(StatementContext ctx, PojoProperty<T> property, String nestedPrefix) {
        final Type propertyType = property.getQualifiedType().getType();
        return new PojoMapper(
            GenericTypes.getErasedType(propertyType),
            nestedPrefix);
    }

    private String getName(PojoProperty<T> property) {
        return property.getAnnotation(ColumnName.class)
            .map(ColumnName::value)
            .orElseGet(property::getName);
    }

    private String debugName(PojoProperty<T> p) {
        return format("%s.%s", type, p.getName());
    }

    private static class PropertyData<T> {

        PropertyData(PojoProperty<T> property, RowMapper<?> mapper) {
            this.property = property;
            this.mapper = mapper;
            propagateNull = checkPropagateNullAnnotation(property);
            isPrimitive = GenericTypes.getErasedType(property.getQualifiedType().getType()).isPrimitive();
        }

        private static boolean checkPropagateNullAnnotation(PojoProperty<?> property) {
            final Optional<String> propagateNullValue = property.getAnnotation(PropagateNull.class).map(PropagateNull::value);
            propagateNullValue.ifPresent(v -> {
                if (!v.isEmpty()) {
                    throw new IllegalArgumentException(format("@PropagateNull does not support a value (%s) on a property (%s)", v, property.getName()));
                }
            });

            return propagateNullValue.isPresent();
        }

        final PojoProperty<T> property;
        final RowMapper<?> mapper;
        final boolean propagateNull;
        final boolean isPrimitive;
    }

    class BoundPojoMapper implements RowMapper<T> {

        private final List<PropertyData<T>> propList;

        BoundPojoMapper(List<PropertyData<T>> propList) {
            this.propList = propList;
        }

        @Override
        public T map(ResultSet rs, StatementContext ctx) throws SQLException {
            final PojoBuilder<T> pojo = getProperties(ctx.getConfig()).create();

            for (PropertyData<T> p : propList) {
                Object value = p.mapper.map(rs, ctx);
                if (p.propagateNull && (value == null || (p.isPrimitive && rs.wasNull()))) {
                    return null;
                }

                if (value != null) {
                    pojo.set(p.property, value);
                }
            }

            return pojo.build();
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", BoundPojoMapper.class.getSimpleName() + "[", "]")
                .add("type=" + type.getTypeName())
                .add("prefix=" + prefix)
                .toString();
        }
    }
}
