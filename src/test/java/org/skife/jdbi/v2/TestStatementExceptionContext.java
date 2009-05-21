package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.StatementException;

/**
 *
 */
public class TestStatementExceptionContext extends DBITestCase
{
    public void testFoo() throws Exception {
        Handle h = openHandle();
        try {
            h.insert("WOOF", 7, "Tom");
        }
        catch (StatementException e) {
           assertEquals(e.getStatementContext().getRawSql(), "WOOF");
        }
    }
}
