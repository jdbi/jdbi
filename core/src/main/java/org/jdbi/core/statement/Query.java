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
package org.jdbi.core.statement;

import java.sql.SQLException;

import org.jdbi.core.Handle;
import org.jdbi.core.result.ResultBearing;
import org.jdbi.core.result.ResultProducer;
import org.jdbi.core.result.ResultProducers;
import org.jdbi.core.result.ResultSetScanner;
import org.jdbi.core.result.UnableToProduceResultException;

/**
 * Statement providing convenience result handling for SQL queries.
 */
public class Query extends SqlStatement<Query> implements ResultBearing, QueryCustomizerMixin<Query>, QueryExecute {
    public Query(final Handle handle, final CharSequence sql) {
        super(handle, sql);
    }

    /**
     * Backwards compatible constructor that takes an explicit string argument.
     *
     * @see Query#Query(Handle, CharSequence)
     */
    public Query(final Handle handle, final String sql) {
        super(handle, sql);
    }

    @Override
    public <R> R execute(final ResultProducer<R> producer) {
        try {
            return producer.produce(this::internalExecute, getContext());
        } catch (final SQLException e) {
            cleanUpForException(e);
            throw new UnableToProduceResultException(e, getContext());
        }
    }

    @Override
    public <R> R scanResultSet(final ResultSetScanner<R> resultSetScanner) {
        return execute(ResultProducers.returningResults()).scanResultSet(resultSetScanner);
    }

    @Override
    public Query concurrentUpdatable() {
        getContext().setConcurrentUpdatable(true);
        return this;
    }
}
