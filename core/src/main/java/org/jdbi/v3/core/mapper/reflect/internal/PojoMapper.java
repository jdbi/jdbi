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

import java.lang.reflect.ParameterizedType;
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
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jdbi.v3.core.annotation.internal.JdbiAnnotations;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.PrefixedRowMapper;
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
public class PojoMapper<T> implements PrefixedRowMapper<T> {

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
        final UnaryOperator<String> caseChange = ctx.getConfig(ReflectionMappers.class).getCaseChange();
        final List<String> columnNames = getColumnNames(rs, caseChange);
        final List<ColumnNameMatcher> columnNameMatchers =
            ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();
        final List<String> unmatchedColumns = new ArrayList<>(columnNames);

        RowMapper<T> result = createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns, Function.identity())
            .orElseThrow(() -> new IllegalArgumentException(format("Mapping bean %s didn't find any matching columns in result set", type)));

        if (ctx.getConfig(ReflectionMappers.class).isStrictMatching()
            && anyColumnsStartWithPrefix(unmatchedColumns, prefix, columnNameMatchers)) {

            throw new IllegalArgumentException(
                format("Mapping bean %s could not match properties for columns: %s", type, unmatchedColumns));
        }

        return result;
    }

    private <R> Optional<RowMapper<R>> createSpecializedRowMapper(StatementContext ctx,
                                                                  List<String> columnNames,
                                                                  List<ColumnNameMatcher> columnNameMatchers,
                                                                  List<String> unmatchedColumns,
                                                                  Function<T, R> postProcessor) {
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

                    Optional<? extends RowMapper<?>> nestedMapper;
                    Type propertyType = property.getQualifiedType().getType();
                    if (propertyType instanceof ParameterizedType pt && pt.getRawType().equals(Optional.class)) {
                        Class<?> rawType = GenericTypes.findGenericParameter(propertyType, Optional.class)
                            .map(GenericTypes::getErasedType)
                            .orElseThrow(() -> new IllegalArgumentException(
                                format("Could not determine the type of Optional property %s", property.getName())));
                        nestedMapper = nestedMappers
                            .computeIfAbsent(property, d -> createNestedMapper(ctx, rawType, nestedPrefix))
                            .createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns, Optional::ofNullable);
                    } else {
                        nestedMapper = nestedMappers
                            .computeIfAbsent(property, d -> createNestedMapper(ctx, GenericTypes.getErasedType(propertyType), nestedPrefix))
                            .createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns, Function.identity());
                    }

                    nestedMapper
                        .ifPresent(mapper ->
                            propList.add(new PropertyData<>(property, mapper)));
                }
            }
        }

        if (propList.isEmpty() && !columnNames.isEmpty()) {
            return Optional.empty();
        }

        propList.sort(Comparator.comparing(p -> p.propagateNull ? 1 : 0));

        RowMapper<R> boundMapper = new BoundPojoMapper<>(propList, postProcessor);
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

    protected PojoMapper<?> createNestedMapper(StatementContext ctx, Class<?> rawType, String nestedPrefix) {
        return new PojoMapper<>(rawType, nestedPrefix);
    }

	@Override
	public String getPrefix() {
        return this.prefix;
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

    class BoundPojoMapper<R> implements RowMapper<R> {

        private final List<PropertyData<T>> propList;
        private final Function<T, R> postProcessor;

        BoundPojoMapper(List<PropertyData<T>> propList, Function<T, R> postProcessor) {
            this.propList = propList;
            this.postProcessor = postProcessor;
        }

        @Override
        public R map(ResultSet rs, StatementContext ctx) throws SQLException {
            final PojoBuilder<T> pojo = getProperties(ctx.getConfig()).create();
            for (PropertyData<T> p : propList) {
                Object value = p.mapper.map(rs, ctx);
                boolean wasNull = (value == null || (p.isPrimitive && rs.wasNull()));
                if (p.propagateNull && wasNull) {
                    return postProcessor.apply(null);
                }

                if (value != null) {
                    pojo.set(p.property, value);
                }
            }

            return postProcessor.apply(pojo.build());
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
