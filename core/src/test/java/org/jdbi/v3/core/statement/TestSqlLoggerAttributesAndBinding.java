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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSqlLoggerAttributesAndBinding {
    private static final String CREATE = "create table <x>(bar int primary key not null)";

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    private Handle h;
    private TalkativeSqlLogger logger;

    @Before
    public void before() {
        logger = new TalkativeSqlLogger();
        dbRule.getJdbi().getConfig(SqlStatements.class).setSqlLogger(logger);
        h = dbRule.getJdbi().open();
    }

    @After
    public void after() {
        h.close();
    }

    @Test
    public void testStatement() {
        h.createUpdate(CREATE).define("x", "foo").execute();

        assertThat(logger.getAttributes())
            .hasSize(2)
            .allMatch(x -> x.get("x").equals("foo"))
            .allMatch(x -> x.size() == 1);
    }

    @Test
    public void testBatch() {
        h.createBatch().add(CREATE).define("x", "foo").execute();

        assertThat(logger.getAttributes())
            .hasSize(2)
            .allMatch(x -> x.get("x").equals("foo"))
            .allMatch(x -> x.size() == 1);
    }

    @Test
    public void testPreparedBatch() {
        h.configure(SqlStatements.class, c -> c.setSqlLogger(SqlLogger.NOP_SQL_LOGGER));
        h.createUpdate(CREATE).define("x", "foo").execute();
        h.configure(SqlStatements.class, c -> c.setSqlLogger(logger));

        int id = 0;

        h.prepareBatch("insert into <x>(bar) values(?)")
            .define("x", "foo")
            .bind(0, new Argument() {
                @Override
                public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
                    statement.setInt(1, id);
                }

                @Override
                public String toString() {
                    return Objects.toString(id);
                }
            })
            .execute();

        assertThat(logger.getAttributes())
            .hasSize(2)
            .allMatch(x -> x.get("x").equals("foo"))
            .allMatch(x -> x.size() == 1);
        assertThat(logger.getBindings()).containsExactly(String.valueOf(id), String.valueOf(id));
    }

    private static class TalkativeSqlLogger implements SqlLogger {
        private final List<Map<String, Object>> attributes = new ArrayList<>();
        private final List<String> bindings = new ArrayList<>();

        @Override
        public void logBeforeExecution(StatementContext context) {
            attributes.add(context.getAttributes());
            context.getBinding().findForPosition(0).ifPresent(a -> bindings.add(a.toString()));
        }

        @Override
        public void logAfterExecution(StatementContext context) {
            attributes.add(context.getAttributes());
            context.getBinding().findForPosition(0).ifPresent(a -> bindings.add(a.toString()));
        }

        @Override
        public void logException(StatementContext context, SQLException ex) {
            attributes.add(context.getAttributes());
            context.getBinding().findForPosition(0).ifPresent(a -> bindings.add(a.toString()));
        }

        public List<Map<String, Object>> getAttributes() {
            return attributes;
        }

        public List<String> getBindings() {
            return bindings;
        }
    }
}
