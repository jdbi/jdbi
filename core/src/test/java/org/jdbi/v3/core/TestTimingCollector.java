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
package org.jdbi.v3.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.transaction.LocalTransactionHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestTimingCollector
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle h;

    private TTC tc;

    protected Handle openHandle() throws SQLException
    {
        tc = new TTC();

        Connection conn = db.openHandle().getConnection();
        JdbiConfig config = new JdbiConfig();
        config.timingCollector = tc;
        return new Handle(config, new LocalTransactionHandler(), new DefaultStatementBuilder(), conn);
    }


    @Before
    public void setUp() throws Exception
    {
        h = openHandle();
    }

    @After
    public void doTearDown() throws Exception
    {
        if (h != null) h.close();
    }

    @Test
    public void testStatement() throws Exception
    {
        int rows = h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        assertThat(rows).isEqualTo(1);
    }

    @Test
    public void testSimpleInsert() throws Exception
    {
        String statement = "insert into something (id, name) values (1, 'eric')";
        int c = h.insert(statement);
        assertThat(c).isEqualTo(1);

        final List<String> statements = tc.getStatements();
        assertThat(statements).containsExactly(statement);
    }

    @Test
    public void testUpdate() throws Exception
    {
        String stmt1 = "insert into something (id, name) values (1, 'eric')";
        String stmt2 = "update something set name = 'ERIC' where id = 1";
        String stmt3 = "select * from something where id = 1";

        h.insert(stmt1);
        h.createStatement(stmt2).execute();
        Something eric = h.createQuery(stmt3).mapToBean(Something.class).list().get(0);
        assertThat(eric.getName()).isEqualTo("ERIC");

        final List<String> statements = tc.getStatements();
        assertThat(statements).containsExactly(stmt1, stmt2, stmt3);
    }

    @Test
    public void testSimpleUpdate() throws Exception
    {
        String stmt1 = "insert into something (id, name) values (1, 'eric')";
        String stmt2 = "update something set name = 'cire' where id = 1";
        String stmt3 = "select * from something where id = 1";

        h.insert(stmt1);
        h.update(stmt2);
        Something eric = h.createQuery(stmt3).mapToBean(Something.class).list().get(0);
        assertThat(eric.getName()).isEqualTo("cire");

        final List<String> statements = tc.getStatements();
        assertThat(statements).containsExactly(stmt1, stmt2, stmt3);
    }

    private static class TTC implements TimingCollector
    {
        private final List<String> statements = new ArrayList<>();

        @Override
        public synchronized void collect(final long elapsedTime, final StatementContext ctx)
        {
            statements.add(ctx.getRawSql());
        }

        public synchronized List<String> getStatements()
        {
            return statements;
        }
    }
}
