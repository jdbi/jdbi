package org.skife.jdbi.v2;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

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

        Something eric = h.createQuery("select * from something where name = ?")
                .setString(0, "eric")
                .map(Something.class)
                .list().get(0);
        assertEquals(1, eric.getId());
    }

    public void testSetPositionalInteger() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Something eric = h.createQuery("select * from something where id = ?")
                .setInteger(0, 1)
                .map(Something.class)
                .list().get(0);
        assertEquals(1, eric.getId());
    }

    public void testBehaviorOnBadBinding1() throws Exception
    {
        Query<Something> q = h.createQuery("select * from something where id = ? and name = ?")
                .setInteger(0, 1)
                .map(Something.class);

        try
        {
            q.list();
            fail("should have thrown exception");
        }
        catch (UnableToExecuteStatementException e)
        {
            assertTrue("Execution goes through here", true);
        }
        catch (Exception e)
        {
            fail("Threw an incorrect exception type");
        }
    }

     public void testBehaviorOnBadBinding2() throws Exception
    {
        Query<Something> q = h.createQuery("select * from something where id = ?")
                .setInteger(1, 1)
                .setString(2, "Hi")
                .map(Something.class);

        try
        {
            q.list();
            fail("should have thrown exception");
        }
        catch (UnableToExecuteStatementException e)
        {
            assertTrue("Execution goes through here", true);
        }
        catch (Exception e)
        {
            fail("Threw an incorrect exception type");
        }
    }

    public void testNamedParameterBinding() throws Exception
    {

    }
}
