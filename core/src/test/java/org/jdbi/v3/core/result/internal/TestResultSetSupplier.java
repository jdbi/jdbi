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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.jdbi.v3.core.statement.StatementContextListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestResultSetSupplier {
    AtomicBoolean closed;
    StatementContext statementContext;

    @BeforeEach
    void setUp() {
        this.closed = new AtomicBoolean();
        this.statementContext = StatementContextAccess.createContext();
        this.statementContext.getConfig(SqlStatements.class).addContextListener(getCloseListener(closed));
    }

    @Test
    void testCloseContext() throws Exception {
        Supplier<ResultSet> emptySupplier = EmptyResultSet::new;
        ResultSetSupplier closingSupplier = ResultSetSupplier.closingContext(emptySupplier, statementContext);

        closingSupplier.close();

        assertThat(closed.get()).isTrue();
    }

    @Test
    void testCloseContextResultSet() throws Exception {
        Supplier<ResultSet> emptySupplier = EmptyResultSet::new;
        ResultSetSupplier closingSupplier = ResultSetSupplier.closingContext(emptySupplier, statementContext);
        final ResultSet resultSet = closingSupplier.get();
        assertThat(resultSet).isNotNull();

        closingSupplier.close();

        assertThat(closed.get()).isTrue();
    }

    @Test
    void testCloseContextNullResultSet() throws Exception {
        Supplier<ResultSet> emptySupplier = () -> null;
        ResultSetSupplier closingSupplier = ResultSetSupplier.closingContext(emptySupplier, statementContext);
        final ResultSet resultSet = closingSupplier.get();
        assertThat(resultSet).isNull();

        closingSupplier.close();

        assertThat(closed.get()).isTrue();
    }

    @Test
    void testNonClosingContext() throws Exception {
        Supplier<ResultSet> emptySupplier = EmptyResultSet::new;
        ResultSetSupplier closingSupplier = ResultSetSupplier.notClosingContext(emptySupplier);

        closingSupplier.close();

        assertThat(closed.get()).isFalse();
    }

    @Test
    void testNonClosingContextResultSet() throws Exception {
        Supplier<ResultSet> emptySupplier = EmptyResultSet::new;
        ResultSetSupplier closingSupplier = ResultSetSupplier.notClosingContext(emptySupplier);
        final ResultSet resultSet = closingSupplier.get();
        assertThat(resultSet).isNotNull();

        closingSupplier.close();

        assertThat(closed.get()).isFalse();
    }

    @Test
    void testNonClosingContextNullResultSet() throws Exception {
        Supplier<ResultSet> emptySupplier = () -> null;
        ResultSetSupplier closingSupplier = ResultSetSupplier.notClosingContext(emptySupplier);
        final ResultSet resultSet = closingSupplier.get();
        assertThat(resultSet).isNull();

        closingSupplier.close();

        assertThat(closed.get()).isFalse();
    }

    private StatementContextListener getCloseListener(AtomicBoolean closed) {
        return new StatementContextListener() {
            @Override
            public void contextCleaned(StatementContext statementContext) {
                closed.set(true);
            }
        };
    }
}
