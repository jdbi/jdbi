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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.core.statement.StatementContext;

import static java.util.Objects.requireNonNull;

class ResultSetResultIterator<T> implements ResultIterator<T> {
    private final ResultSet resultSet;
    private final RowMapper<T> rowMapper;
    private final StatementContext context;

    private volatile boolean alreadyAdvanced = false;
    private volatile boolean hasNext = false;
    private volatile boolean closed = false;

    ResultSetResultIterator(ResultSet resultSet,
                            RowMapper<T> rowMapper,
                            StatementContext context) throws SQLException {
        this.resultSet = requireNonNull(resultSet);
        this.rowMapper = rowMapper.specialize(resultSet, context);
        this.context = context;
    }

    @Override
    public void close() {
        closed = true;
        context.close();
    }

    @Override
    public boolean hasNext() {
        if (closed) {
            return false;
        }

        if (alreadyAdvanced) {
            return hasNext;
        }

        hasNext = safeNext();

        if (hasNext) {
            alreadyAdvanced = true;
        } else {
            close();
        }

        return hasNext;
    }

    @Override
    public T next() {
        if (closed) {
            throw new IllegalStateException("iterator is closed");
        }

        if (!hasNext()) {
            close();
            throw new NoSuchElementException("No element to advance to");
        }

        try {
            return rowMapper.map(resultSet, context);
        } catch (SQLException e) {
            throw new ResultSetException("Exception thrown mapping result set into return type", e, context);
        } finally {
            alreadyAdvanced = safeNext();
            if (!alreadyAdvanced) {
                close();
            }
        }
    }

    @Override
    public StatementContext getContext() {
        return context;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Deleting from a result set iterator is not yet supported");
    }

    private boolean safeNext() {
        try {
            return resultSet.next();
        } catch (SQLException e) {
            throw new ResultSetException("Unable to advance result set", e, context);
        }
    }
}
