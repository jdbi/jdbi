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
package org.jdbi.v3.core.mapper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.meta.Beta;

@Beta
// TODO jdbi4: consolidate with MapMapper and include by default
public class GenericMapMapperFactory implements RowMapperFactory {
    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        // it's a non-raw Map...
        if (!(type instanceof ParameterizedType) || GenericTypes.getErasedType(type) != Map.class) {
            return Optional.empty();
        }
        Optional<Type> maybeKeyType = GenericTypes.findGenericParameter(type, Map.class, 0);

        // ... with a concrete K type...
        if (!maybeKeyType.isPresent()) {
            return Optional.empty();
        }
        Type keyType = maybeKeyType.get();

        // ... which is a String...
        if (keyType != String.class) {
            return Optional.empty();
        }
        Optional<Type> maybeValueType = GenericTypes.findGenericParameter(type, Map.class, 1);

        // ... and a concrete V type...
        if (!maybeValueType.isPresent()) {
            return Optional.empty();
        }
        Type valueType = maybeValueType.get();

        // ... that is not Object...
        if (valueType == Object.class) {
            return Optional.empty();
        }
        Optional<ColumnMapper<?>> maybeColumnMapper = config.get(ColumnMappers.class).findFor(valueType);

        // ... for which there is a ColumnMapper
        return maybeColumnMapper.map(t -> forValueType(valueType));
    }

    @Beta
    public static <X> RowMapper<Map<String, X>> forValueType(Type valueType) {
        return new GenericMapMapper<>(valueType);
    }

    private static class GenericMapMapper<X> implements RowMapper<Map<String, X>> {
        private final Type type;

        private GenericMapMapper(Type type) {
            this.type = type;
        }

        @Override
        public Map<String, X> map(ResultSet rs, StatementContext ctx) throws SQLException {
            return specialize(rs, ctx).map(rs, ctx);
        }

        @Override
        public RowMapper<Map<String, X>> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
            Optional<ColumnMapper<?>> maybeColumnMapper = ctx.getConfig(ColumnMappers.class).findFor(type);

            // unfortunately this cannot be checked sooner
            if (maybeColumnMapper.isPresent()) {
                ColumnMapper<?> columnMapper = maybeColumnMapper.get();
                List<String> columnNames = getColumnNames(rs.getMetaData(), ctx.getConfig(MapMappers.class).getCaseChange());

                return (r, c) -> {
                    Map<String, X> row = new HashMap<>();

                    for (int i = 0; i < columnNames.size(); i++) {
                        @SuppressWarnings("unchecked")
                        X value = (X) columnMapper.map(r, i + 1, ctx);
                        row.put(columnNames.get(i), value);
                    }

                    return row;
                };
            } else {
                throw new RuntimeException("no mapper found for type " + type.getTypeName());
            }
        }

        private static List<String> getColumnNames(ResultSetMetaData meta, UnaryOperator<String> caseChange) throws SQLException {
            // important: ordered, not sorted, and unique
            Set<String> names = new LinkedHashSet<>();
            int columnCount = meta.getColumnCount();

            for (int i = 0; i < columnCount; i++) {
                String columnName = meta.getColumnName(i + 1);
                String columnLabel = meta.getColumnLabel(i + 1);

                String name = columnLabel == null ? columnName : columnLabel;
                name = caseChange.apply(name);

                boolean added = names.add(name);
                if (!added) {
                    throw new RuntimeException("column \"" + name + "\" appears twice in this resultset!");
                }
            }

            return new ArrayList<>(names);
        }
    }
}
