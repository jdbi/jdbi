package org.skife.jdbi.v2;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;

/**
 * 
 */
public class TestParameterBinding extends TestCase
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

    public void testSetPositionalString() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Something eric  = h.createQuery("select * from something where name = ?")
                .setString(0, "eric")
                .map(Something.class)
                .list().get(0);
        assertEquals(1, eric.getId());
    }

    public void testSetPositionalInteger() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Something eric  = h.createQuery("select * from something where id = ?")
                .setInteger(0, 1)
                .map(Something.class)
                .list().get(0);
        assertEquals(1, eric.getId());
    }
}
