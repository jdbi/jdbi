package org.skife.jdbi.v2;

/**
 * 
 */
public class TestNamedParams extends DBITestCase
{
    public void testSomething() throws Exception
    {
        Handle h = openHandle();
        SQLStatement insert = h.createStatement("insert into something (id, name) values (:id, :name)");
        insert.setInteger("id", 1);
        insert.setString("name", "Brian");
        int count = insert.execute();
        assertEquals(1, count);
    }
}
