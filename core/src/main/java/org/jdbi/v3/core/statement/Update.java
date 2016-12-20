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
package org.jdbi.v3.core.statement;

import static org.jdbi.v3.core.result.ResultProducers.returningGeneratedKeys;
import static org.jdbi.v3.core.result.ResultProducers.returningUpdateCount;

import java.sql.SQLException;
import java.sql.Statement;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.ResultProducer;
import org.jdbi.v3.core.result.ResultSetIterable;
import org.jdbi.v3.core.result.UnableToProduceResultException;

/**
 * Used for INSERT, UPDATE, and DELETE statements
 */
public class Update extends SqlStatement<Update> implements ResultBearing {
    public Update(Handle handle, String sql) {
        super(handle, sql);
    }

    /**
     * Execute the statement
     *
     * @return the number of rows modified
     */
    public int execute() {
        return execute(returningUpdateCount());
    }

    @Override
    public <R> R execute(ResultProducer<R> producer) {
        try {
            return producer.produce(this::internalExecute, getContext());
        } catch (SQLException e) {
            throw new UnableToProduceResultException("Could not produce statement result", e, getContext());
        }
    }

    /**
     * Execute the statement and returns any auto-generated keys. This requires the JDBC driver to support
     * the {@link Statement#getGeneratedKeys()} method.
     *
     * @param generatedKeyColumnNames optional list of generated key column names.
     * @return ResultSetIterable of generated keys
     */
    public ResultSetIterable executeAndReturnGeneratedKeys(String... generatedKeyColumnNames) {
        return execute(returningGeneratedKeys(generatedKeyColumnNames));
    }
}
