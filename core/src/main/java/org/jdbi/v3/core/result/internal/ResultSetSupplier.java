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
import java.util.function.Supplier;

import org.jdbi.v3.core.statement.Cleanable;
import org.jdbi.v3.core.statement.StatementContext;

public abstract class ResultSetSupplier implements Supplier<ResultSet>, Cleanable {

    public static ResultSetSupplier closingContext(Supplier<ResultSet> supplier, StatementContext context) {
        return new ResultSetSupplier(supplier) {
            @Override
            public void close() throws SQLException {
                try (context) {
                    cleanable.close();
                }
            }
        };
    }

    public static ResultSetSupplier notClosingContext(Supplier<ResultSet> supplier) {
        return new ResultSetSupplier(supplier) {
            @Override
            public void close() throws SQLException {
                cleanable.close();
            }
        };
    }

    private final Supplier<ResultSet> supplier;

    protected Cleanable cleanable = NO_OP;

    private ResultSetSupplier(Supplier<ResultSet> supplier) {
        this.supplier = supplier;
    }

    @Override
    public ResultSet get() {
        ResultSet resultSet = supplier.get();
        if (resultSet != null) {
            this.cleanable = resultSet::close;
        }
        return resultSet;
    }
}
