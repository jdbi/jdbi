package org.skife.jdbi.v2;

import org.skife.jdbi.derby.Tools;

/**
 *
 */
public class TestNoOpStatementRewriter extends DBITestCase
{
    private DBI dbi;

    public void setUp() throws Exception
    {
        super.setUp();
        this.dbi = new DBI(Tools.getDataSource());
        dbi.setStatementRewriter(new NoOpStatementRewriter());
    }


    public void testFoo() throws Exception
    {
        Handle h = dbi.open();
        h.insert("insert into something (id, name) values (1, 'Keith')");

        String name = h.createQuery("select name from something where id = ?")
                .bind(0, 1)
                .map(Something.class)
                .first().getName();
        assertEquals("Keith", name);
    }

    public void tesBar() throws Exception
    {
        Handle h = dbi.open();
        h.insert("insert into something (id, name) values (1, 'Keith')");

        String name = h.createQuery("select name from something where id = ? and name = ?")
                .bind(0, 1)
                .bind(1, "Keith")
                .map(Something.class)
                .first().getName();
        assertEquals("Keith", name);
    }

    public void testBaz() throws Exception
    {
        Handle h = dbi.open();
        h.insert("insert into something (id, name) values (1, 'Keith')");
    }
}
