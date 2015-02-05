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
package org.jdbi.v3;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jdbi.v3.tweak.transactions.LocalTransactionHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestTimingCollector
{
    @Rule
    public MemoryDatabase db = new MemoryDatabase();

    private BasicHandle h;

    private TTC tc;

    protected BasicHandle openHandle() throws SQLException
    {
        tc = new TTC();

        Connection conn = db.openHandle().getConnection();
        BasicHandle h = new BasicHandle(new LocalTransactionHandler(),
                                        new ClasspathStatementLocator(),
                                        new DefaultStatementBuilder(),
                                        new ColonPrefixNamedParamStatementRewriter(),
                                        conn,
                                        new HashMap<String, Object>(),
                                        tc,
                                        new MappingRegistry(),
                                        new Foreman());
        return h;
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
        assertEquals(1, rows);
    }

    @Test
    public void testSimpleInsert() throws Exception
    {
        String statement = "insert into something (id, name) values (1, 'eric')";
        int c = h.insert(statement);
        assertEquals(1, c);

        final List<String> statements = tc.getStatements();
        assertEquals(1, statements.size());
        assertEquals(statement, statements.get(0));
    }

    @Test
    public void testUpdate() throws Exception
    {
        String stmt1 = "insert into something (id, name) values (1, 'eric')";
        String stmt2 = "update something set name = 'ERIC' where id = 1";
        String stmt3 = "select * from something where id = 1";

        h.insert(stmt1);
        h.createStatement(stmt2).execute();
        Something eric = h.createQuery(stmt3).map(Something.class).list().get(0);
        assertEquals("ERIC", eric.getName());

        final List<String> statements = tc.getStatements();
        assertEquals(3, statements.size());
        assertEquals(stmt1, statements.get(0));
        assertEquals(stmt2, statements.get(1));
        assertEquals(stmt3, statements.get(2));
    }

    @Test
    public void testSimpleUpdate() throws Exception
    {
        String stmt1 = "insert into something (id, name) values (1, 'eric')";
        String stmt2 = "update something set name = 'cire' where id = 1";
        String stmt3 = "select * from something where id = 1";

        h.insert(stmt1);
        h.update(stmt2);
        Something eric = h.createQuery(stmt3).map(Something.class).list().get(0);
        assertEquals("cire", eric.getName());

        final List<String> statements = tc.getStatements();
        assertEquals(3, statements.size());
        assertEquals(stmt1, statements.get(0));
        assertEquals(stmt2, statements.get(1));
        assertEquals(stmt3, statements.get(2));
    }

    private static class TTC implements TimingCollector
    {
        private final List<String> statements = new ArrayList<String>();

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
