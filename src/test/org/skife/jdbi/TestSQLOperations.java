/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TestSQLOperations extends TestCase
{
    private Handle handle;
    private Connection conn;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        handle = DBI.open(Tools.CONN_STRING);
        conn = handle.getConnection();
    }

    public void tearDown() throws Exception
    {
        handle.close();
        Tools.stop();
    }

    public void testInsert() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'george')");
        final Statement stmt = conn.createStatement();

        final ResultSet rst = stmt.executeQuery("select * from something");

        assertTrue(rst.next());
        assertEquals(1, rst.getInt("id"));
        assertEquals("george", rst.getString("name"));

        rst.close();
        stmt.close();
    }

    public void testSelect() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'george')");
        final Collection results = handle.query("select id, name from something");
        assertTrue(results.iterator().hasNext());
        final Map row = (Map) results.iterator().next();
        assertNotNull(row);
        assertEquals(new Integer(1), row.get("id"));
        assertEquals("george", row.get("name"));
    }

    public void testSelectWithAliases() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'george')");
        final Collection results = handle.query("select id as kangaroo, name as frog from something");
        assertTrue(results.iterator().hasNext());
        final Map row = (Map) results.iterator().next();
        assertNotNull(row);
        assertEquals(new Integer(1), row.get("kangaroo"));
        assertEquals("george", row.get("frog"));
    }

    public void testExplicitNamedQuery() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'george')");
        handle.name("bob", "select id as kangaroo, name as frog from something");
        final Collection results = handle.query("bob");
        assertTrue(results.iterator().hasNext());
        final Map row = (Map) results.iterator().next();
        assertNotNull(row);
        assertEquals(new Integer(1), row.get("kangaroo"));
        assertEquals("george", row.get("frog"));
    }

    public void testExplicitlyLoadedQuery() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'george')");
        handle.load("all-something");
        final Collection results = handle.query("all-something");
        assertTrue(results.iterator().hasNext());
        final Map row = (Map) results.iterator().next();
        assertNotNull(row);
        assertEquals(new Integer(1), row.get("id"));
        assertEquals("george", row.get("name"));
    }

    public void testImplicitelyLoadedQuery() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'george')");
        final Collection results = handle.query("all-something");
        assertTrue(results.iterator().hasNext());
        final Map row = (Map) results.iterator().next();
        assertNotNull(row);
        assertEquals(new Integer(1), row.get("id"));
        assertEquals("george", row.get("name"));
    }

    public void testNamedParams() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'one')");
        handle.execute("insert into something (id, name) values (2, 'two')");

        final Map params = new HashMap();
        params.put("anId", new Integer(1));
        final Collection results = handle.query("select id, name from something where id = :anId", params);
        final Iterator itty = results.iterator();
        assertTrue(itty.hasNext());
        final Map row = (Map) itty.next();
        assertEquals(new Integer(1), row.get("id"));
        assertEquals("one", row.get("name"));
        assertFalse(itty.hasNext());
    }

    public void testSetToNull() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'one')");

        handle.execute("update something set name = :name", new Object[] {null});

        Map r = handle.first("select * from something where id = 1");
        assertEquals(null, r.get("name"));

        r = handle.first("select * from something where name is null");
        assertEquals(Integer.valueOf(1), r.get("id"));
    }

    public void testPositionalParams() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'one')");
        handle.execute("insert into something (id, name) values (2, 'two')");

        final Collection results = handle.query("select id, name from something where id = ?",
                                                new Object[] {new Integer(1)});
        final Iterator itty = results.iterator();
        assertTrue(itty.hasNext());
        final Map row = (Map) itty.next();
        assertEquals(new Integer(1), row.get("id"));
        assertEquals("one", row.get("name"));
    }

    public void testPositionalParamsFromCollection() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'one')");
        handle.execute("insert into something (id, name) values (2, 'two')");

        final Collection args = new ArrayList();
        args.add(new Integer(1));

        final Collection results = handle.query("select id, name from something where id = :id", args);
        final Iterator itty = results.iterator();
        assertTrue(itty.hasNext());
        final Map row = (Map) itty.next();
        assertEquals(new Integer(1), row.get("id"));
        assertEquals("one", row.get("name"));
    }

    public void testNamedArgs() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'one')");
        handle.execute("insert into something (id, name) values (2, 'two')");

        final Collection results = handle.query("select id, name from something where id = :id",
                                                Args.with("id", new Integer(1)));
        final Iterator itty = results.iterator();
        assertTrue(itty.hasNext());
        final Map row = (Map) itty.next();
        assertEquals(new Integer(1), row.get("id"));
        assertEquals("one", row.get("name"));
    }

    public void test2NamedArgs() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'one')");
        handle.execute("insert into something (id, name) values (2, 'two')");

        final Collection results = handle.query("select id, name from something where id = :id and name like :name",
                                                Args.with("id", new Integer(1)).and("name", "o%"));
        final Iterator itty = results.iterator();
        assertTrue(itty.hasNext());
        final Map row = (Map) itty.next();
        assertEquals(new Integer(1), row.get("id"));
        assertEquals("one", row.get("name"));
    }

    public void testScript() throws Exception
    {
        handle.script("sample-script");
        final Collection results = handle.query("select id, name from something order by id");
        final Iterator itty = results.iterator();
        assertTrue(itty.hasNext());
        final Map one = (Map) itty.next();
        assertEquals(new Integer(1), one.get("id"));
        assertTrue(itty.hasNext());
        final Map two = (Map) itty.next();
        assertEquals(new Integer(2), two.get("id"));
    }

    public void testScriptWithFunName() throws Exception
    {
        handle.execute("different-name-sql.txt");
        final Collection results = handle.query("select id, name from something");
        final Iterator i = results.iterator();
        assertTrue(i.hasNext());
        final Map row = (Map) i.next();
        assertEquals(new Integer(3), row.get("id"));
    }

    public void testFunction() throws Exception
    {
        handle.execute("create-function");
        final Collection results = handle.query("select count(*), do_it() as foo from something");
        final Iterator itty = results.iterator();
        assertTrue(itty.hasNext());
        final Map row = (Map) itty.next();
        assertEquals(Tools.doIt(), row.get("foo"));
    }

    public void testCallStoredProcedureAsStatement() throws Exception
    {
        handle.execute("create-procedure");
        handle.execute("call insertSomething()");
        final Collection results = handle.query("select id, name from something");
        final Iterator i = results.iterator();
        assertTrue(i.hasNext());
        final Map row = (Map) i.next();
        assertEquals("george", row.get("name"));
    }

    public void testReusingStatementWorksAlright() throws Exception
    {
        final String sql = "select * from something";
        handle.execute(sql);
        try
        {
            handle.execute(sql);
        }
        catch (DBIException e)
        {
            fail("re-using same statement caused exception");
        }
    }

    public void testQuotedQuestionMark() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'hi ? hi')");
        final Map row = handle.first("select * from something where name like '%?%'");
        assertEquals("hi ? hi", row.get("name"));
    }
}
