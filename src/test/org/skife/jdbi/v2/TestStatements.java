package org.skife.jdbi.v2;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;

/**
 * 
 */
public class TestStatements extends TestCase
{
    private BasicHandle h;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        h = new BasicHandle(Tools.getConnection());
    }

    public void tearDown() throws Exception
    {
        if (h != null) h.close();
        Tools.stop();
    }

    public void testStatement() throws Exception
    {
        int rows = h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        assertEquals(1, rows);
    }

    public void testSimpleInsert() throws Exception
    {
        int c = h.insert("insert into something (id, name) values (1, 'eric')");
        assertEquals(1, c);
    }

    public void testUpdate() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.createStatement("update something set name = 'ERIC' where id = 1").execute();
        Something eric = h.createQuery("select * from something where id = 1").map(Something.class).list().get(0);
        assertEquals("ERIC", eric.getName());
    }

    public void testSimpleUpdate() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.update("update something set name = 'cire' where id = 1");
        Something eric = h.createQuery("select * from something where id = 1").map(Something.class).list().get(0);
        assertEquals("cire", eric.getName());
    }

}
