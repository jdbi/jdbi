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
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.jdbi.v3.core.statement.StatementContextListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestResultSetResultIterator {
    AtomicBoolean closed;
    StatementContext context;

    @BeforeEach
    void setUp() {
        this.closed = new AtomicBoolean();
        this.context = StatementContextAccess.createContext();
        context.getConfig(SqlStatements.class).addContextListener(getCloseListener(closed));
    }


    @Test
    public void testNullClosesIterator() throws Exception {
        Supplier<ResultSet> nullSupplier = () -> null;
        RowMapper<Integer> mapper = context.findMapperFor(Integer.class).orElseGet(Assertions::fail);

        Iterator<Integer> it = new ResultSetResultIterator<>(nullSupplier, mapper, context);
        assertThat(it).isExhausted();

        // context was closed
        assertThat(closed).isTrue();
    }

    @Test
    public void testEmptyClosesIterator() throws Exception {
        Supplier<ResultSet> emptySupplier = EmptyResultSet::new;

        RowMapper<Integer> mapper = context.findMapperFor(Integer.class).orElseGet(Assertions::fail);

        Iterator<Integer> it = new ResultSetResultIterator<>(emptySupplier, mapper, context);
        assertThat(it).isExhausted();

        // calling hasNext() closes context
        assertThat(closed).isTrue();
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
