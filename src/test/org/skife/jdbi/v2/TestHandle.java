package org.skife.jdbi.v2;

import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public class TestHandle extends DBITestCase
{
    public void testInTransaction() throws Exception
    {
        Handle h = this.openHandle();

        String value = h.inTransaction(new TransactionCallback<String>()
        {
            public String inTransaction(Handle handle, TransactionStatus status) throws Exception
            {
                handle.insert("insert into something (id, name) values (1, 'Brian')");
                return handle.createQuery("select name from something where id = 1").map(Something.class).first().getName();
            }
        });
        assertEquals("Brian", value);
    }

    public void testSillyNumberOfCallbacks() throws Exception
    {
        Handle h = openHandle();
        h.insert("insert into something (id, name) values (1, 'Keith')");
        h.close();


        String value = new DBI(Tools.CONN_STRING).withHandle(new HandleCallback<String>()
        {
            public String withHandle(Handle handle) throws Exception
            {
                return handle.inTransaction(new TransactionCallback<String>()
                {
                    public String inTransaction(Handle handle, TransactionStatus status) throws Exception
                    {
                        return handle.createQuery("select name from something where id = 1").map(new ResultSetMapper<String>()
                        {
                            public String map(int index, ResultSet r) throws SQLException
                            {
                                return r.getString(1);
                            }
                        }).first();
                    }
                });
            }
        });

        assertEquals("Keith", value);
    }
}
