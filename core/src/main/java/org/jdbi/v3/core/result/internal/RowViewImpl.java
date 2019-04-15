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
package org.jdbi.v3.core.result.internal;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.StatementContext;

public class RowViewImpl extends RowView {
    private final StatementContext ctx;
    private final ResultSet rs;

    private final Map<Type, RowMapper<?>> rowMappers = new ConcurrentHashMap<>();
    private final Map<QualifiedType<?>, ColumnMapper<?>> columnMappers = new ConcurrentHashMap<>();

    public RowViewImpl(ResultSet rs, StatementContext ctx) {
        this.rs = rs;
        this.ctx = ctx;
    }

    /**
     * Use a row mapper to extract a type from the current ResultSet row.
     * @param type the type to map
     * @return the materialized object
     */
    @Override
    public Object getRow(Type type) {
        try {
            return rowMapperFor(type).map(rs, ctx);
        } catch (SQLException e) {
            throw new MappingException(e);
        }
    }

    private RowMapper<?> rowMapperFor(Type type) throws SQLException {
        if (rowMappers.containsKey(type)) {
            return rowMappers.get(type);
        }

        RowMapper<?> mapper = ctx.findRowMapperFor(type)
                .orElseThrow(() -> new NoSuchMapperException("No row mapper registered for " + type))
                .specialize(rs, ctx);
        rowMappers.put(type, mapper);

        return mapper;
    }

    /**
     * Use a qualified column mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param column the column index
     * @param type the QualifiedType of the type
     * @return the materialized T
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getColumn(int column, QualifiedType<T> type) {
        try {
            return (T) columnMapperFor(type).map(rs, column, ctx);
        } catch (SQLException e) {
            throw new MappingException(e);
        }
    }

    /**
     * Use a qualified column mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param column the column name
     * @param type the QualifiedType of the type
     * @return the materialized T
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getColumn(String column, QualifiedType<T> type) {
        try {
            return (T) columnMapperFor(type).map(rs, column, ctx);
        } catch (SQLException e) {
            throw new MappingException(e);
        }
    }

    private ColumnMapper<?> columnMapperFor(QualifiedType<?> type) {
        return columnMappers.computeIfAbsent(type, t ->
                ctx.findColumnMapperFor(t)
                        .orElseThrow(() -> new NoSuchMapperException("No column mapper registered for " + t)));
    }
}
