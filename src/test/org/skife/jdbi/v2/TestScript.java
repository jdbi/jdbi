package org.skife.jdbi.v2;

/**
 * 
 */
public class TestScript extends DBITestCase
{
    public void testScriptStuff() throws Exception
    {
        Handle h = openHandle();
        Script s = h.createScript("default-data");
        s.execute();

        assertEquals(2, h.select("select * from something").size());
    }
}
