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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.result.internal.RowViewImpl;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.meta.Beta;

/**
 * Higher level cousin of {@link RowMapper} that operates over {@link RowView}s rather than
 * the bare {@link ResultSet}.
 * @param <T>
 */
@FunctionalInterface
@Beta
public interface RowViewMapper<T> extends RowMapper<T> {
    @Override
    default T map(ResultSet rs, StatementContext ctx) throws SQLException {
        return map(new RowViewImpl(rs, ctx));
    }

    @Override
    default RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        RowView row = new RowViewImpl(rs, ctx);
        return (x, y) -> map(row);
    }

    /**
     * Produce a single result item from the current row of a {@link RowView}.
     * @param rowView the view into the current row of the ResultSet
     * @return the produced result
     * @throws SQLException something went wrong
     */
    T map(RowView rowView) throws SQLException;
}
