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

}
