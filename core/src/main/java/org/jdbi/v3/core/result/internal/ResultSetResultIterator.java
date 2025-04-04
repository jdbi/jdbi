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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.core.statement.StatementContext;

class ResultSetResultIterator<T> implements ResultIterator<T> {
    private final ResultSet resultSet;
    private final RowMapper<T> rowMapper;

    private final ResultSetSupplier resultSetSupplier;
    private final StatementContext context;

    private final AtomicLong mappedRows = new AtomicLong();

    private volatile boolean alreadyAdvanced = false;
    private volatile boolean hasNext = false;
    private volatile boolean closed = false;

    ResultSetResultIterator(Supplier<ResultSet> resultSetSupplier,
        RowMapper<T> rowMapper,
        StatementContext context) throws SQLException {

        this.context = context;

        if (resultSetSupplier instanceof ResultSetSupplier) {
            this.resultSetSupplier = (ResultSetSupplier) resultSetSupplier;
        } else {
            this.resultSetSupplier = ResultSetSupplier.closingContext(resultSetSupplier, context);
        }

        this.resultSet = this.resultSetSupplier.get();

        if (resultSet != null) {
            context.addCleanable(resultSet::close);
            this.rowMapper = rowMapper.specialize(resultSet, context);
        } else {
            close();
            this.rowMapper = null;
        }
    }

    @Override
    public void close() {
        closed = true;
        context.setMappedRows(mappedRows.get());
        try {
            resultSetSupplier.close();
        } catch (SQLException e) {
            throw Sneaky.throwAnyway(e);
        }
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
        if (!hasNext()) {
            close();
            throw new NoSuchElementException("No element to advance to");
        }

        mappedRows.incrementAndGet();

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
