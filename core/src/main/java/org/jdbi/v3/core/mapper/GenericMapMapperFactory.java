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
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.meta.Beta;

/**
 * Factory for a RowMapper that can map resultset rows to column name/generic value {@link Map}s.
 *
 * Each row in the resultset becomes a distinct {@link Map}, in which the keys are all distinct column names and the values are the corresponding cell contents.
 * All values are mapped to the same generic type {@code T} (e.g. {@link java.math.BigDecimal}) by a {@link ColumnMapper} from the {@link ConfigRegistry}.
 *
 * This differs from {@link MapMapper} by supporting a concrete type instead of only {@link Object}, and from {@code collecting} into a {@link Map}
 * in that the latter maps an entire resultset to a single {@link Map} and can only keep 1 key and 1 value from each row.
 *
 * Use cases for this are mainly single-row results like numeric reports (e.g. the price components,
 * taxes, etc of a product for sale, or a set of possible labeled values for a user setting), and matrices.
 *
 * @see ResultBearing#mapToMap(GenericType)
 *
 * @see MapMapper
 * @see ResultBearing#mapToMap()
 *
 * @see ResultBearing#collectInto(GenericType)
 */
@Beta
// TODO jdbi4: integrate with MapMapper? check if registering by default is compatible with other Map-mapping features
public class GenericMapMapperFactory implements RowMapperFactory {
    // invoked by sqlobject
    @Override
    public Optional<RowMapper<?>> build(Type mapType, ConfigRegistry config) {
        return Optional.of(mapType)
            .filter(ParameterizedType.class::isInstance)
            .map(ParameterizedType.class::cast)
            .filter(maybeMap -> Map.class.equals(maybeMap.getRawType()))
            .filter(map -> String.class.equals(GenericTypes.findGenericParameter(map, Map.class, 0).orElse(null)))
            .flatMap(map -> GenericTypes.findGenericParameter(map, Map.class, 1))
            .filter(value -> !Object.class.equals(value))
            .flatMap(config.get(ColumnMappers.class)::findFor)
            .map(GenericMapMapper::new);
    }

    // invoked manually or by fluent api
    @Beta
    public static <T> RowMapper<Map<String, T>> getMapperForValueType(Class<T> valueType, ConfigRegistry config) {
        return config.get(ColumnMappers.class)
            .findFor(valueType)
            .map(GenericMapMapper::new)
            .orElseThrow(() -> new RuntimeException("no column mapper found for type " + valueType));
    }

    // invoked manually or by fluent api
    @Beta
    public static <T> RowMapper<Map<String, T>> getMapperForValueType(GenericType<T> valueType, ConfigRegistry config) {
        return config.get(ColumnMappers.class)
            .findFor(valueType)
            .map(GenericMapMapper::new)
            .orElseThrow(() -> new RuntimeException("no column mapper found for type " + valueType));
    }

    private static class GenericMapMapper<T> implements RowMapper<Map<String, T>> {
        private final ColumnMapper<T> mapper;

        private GenericMapMapper(ColumnMapper<T> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Map<String, T> map(ResultSet rs, StatementContext ctx) throws SQLException {
            return specialize(rs, ctx).map(rs, ctx);
        }

        @Override
        public RowMapper<Map<String, T>> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
            List<String> keyNames = getMapKeys(rs.getMetaData(), ctx.getConfig(MapMappers.class).getCaseChange());

            return (r, c) -> {
                Map<String, T> row = new HashMap<>();

                for (int i = 0; i < keyNames.size(); i++) {
                    T value = mapper.map(r, i + 1, ctx);
                    row.put(keyNames.get(i), value);
                }

                return row;
            };
        }

        private static List<String> getMapKeys(ResultSetMetaData meta, UnaryOperator<String> caseChange) throws SQLException {
            // important: ordered, not sorted, and unique
            Set<String> names = new LinkedHashSet<>();
            int columnCount = meta.getColumnCount();

            for (int i = 0; i < columnCount; i++) {
                String columnName = meta.getColumnName(i + 1);
                String columnLabel = meta.getColumnLabel(i + 1);

                String key = columnLabel == null ? columnName : columnLabel;
                String renamedKey = caseChange.apply(key);

                boolean added = names.add(renamedKey);
                if (!added) {
                    throw new RuntimeException(String.format("map key \"%s\" (from column \"%s\") appears twice in this resultset!", renamedKey, key));
                }
            }

            return new ArrayList<>(names);
        }
    }
}
