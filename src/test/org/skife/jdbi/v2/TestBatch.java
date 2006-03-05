package org.skife.jdbi.v2;

import java.util.List;

/**
 * 
 */
public class TestBatch extends DBITestCase
{
    public void testBasics() throws Exception
    {
        Handle h = this.openHandle();
        Batch b = h.createBatch();
        b.add("insert into something (id, name) values (0, 'Keith')");
        b.add("insert into something (id, name) values (1, 'Eric')");
        b.add("insert into something (id, name) values (2, 'Brian')");
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").map(Something.class).list();
        assertEquals(3, r.size());
    }
}
