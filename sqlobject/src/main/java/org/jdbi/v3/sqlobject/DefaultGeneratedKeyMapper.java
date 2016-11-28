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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.SingleColumnMapper;

class DefaultGeneratedKeyMapper implements RowMapper<Object> {
    private final Type returnType;
    private final String columnName;

    DefaultGeneratedKeyMapper(Type returnType, String columnName) {
        this.returnType = returnType;
        this.columnName = columnName;
    }

    @Override
    public Object map(ResultSet rs, StatementContext ctx) throws SQLException {
        return rowMapperFor(ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<Object> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        return rowMapperFor(ctx).specialize(rs, ctx);
    }

    @SuppressWarnings("unchecked")
    private RowMapper<Object> rowMapperFor(StatementContext ctx) {
        ColumnMapper<Object> columnMapper = (ColumnMapper<Object>) ctx.findColumnMapperFor(returnType).orElse(null);
        if (columnMapper != null) {
            return "".equals(columnName)
                    ? new SingleColumnMapper<>(columnMapper, 1)
                    : new SingleColumnMapper<>(columnMapper, columnName);
        }
        return (RowMapper<Object>) ctx.findRowMapperFor(returnType)
                .orElseThrow(() -> new IllegalStateException("No column or row mapper for " + returnType));
    }
}
