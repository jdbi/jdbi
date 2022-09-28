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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringJoiner;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Delegating mapper that implements the &#0064;PropagateNull semantics to check a specific column in the result set for null first. If that column is null,
 * return null as the value, otherwise executed the delegated mapper.
 *
 * @param <T>
 */
public final class NullDelegatingMapper<T> implements RowMapper<T> {

    private final int index;
    private final RowMapper<T> delegate;

    public NullDelegatingMapper(int index, RowMapper<T> delegate) {
        this.index = index;
        this.delegate = delegate;
    }

    @Override
    public void init(ConfigRegistry registry) {
        delegate.init(registry);
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        rs.getObject(index);
        if (rs.wasNull()) {
            return null;
        }
        return delegate.map(rs, ctx);
    }

    @Override
    public RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        final RowMapper<T> newDelegate = delegate.specialize(rs, ctx);
        if (newDelegate instanceof NullDelegatingMapper) {
            return newDelegate;
        } else {
            return new NullDelegatingMapper<>(index, newDelegate);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NullDelegatingMapper.class.getSimpleName() + "[", "]")
            .add("index=" + index)
            .add("delegate=" + delegate)
            .toString();
    }
}
