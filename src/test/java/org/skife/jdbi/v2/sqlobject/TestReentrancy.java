package org.skife.jdbi.v2.sqlobject;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;
import org.skife.jdbi.v2.tweak.HandleCallback;
import java.util.Random;

public class TestReentrancy extends TestCase
{
    private DBI    dbi;
    private Handle handle;

    interface TheBasics extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something something);
    }

    @Test
    public void testHandleReentrant() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);

        dao.withHandle(new HandleCallback<Void>() {
            @Override
            public Void withHandle(Handle handle) throws Exception
            {
                dao.insert(new Something(7, "Martin"));

                handle.createQuery("SELECT 1").list();

                return null;
            }
        });
    }

    @Test
    public void testTxnReentrant() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);

        dao.withHandle(new HandleCallback<Void>() {
            @Override
            public Void withHandle(Handle handle) throws Exception
            {
                handle.inTransaction(new TransactionCallback<Void>() {
                    @Override
                    public Void inTransaction(Handle conn, TransactionStatus status) throws Exception
                    {
                        dao.insert(new Something(1, "x"));

                        conn.createQuery("SELECT 1").list();
                        return null;
                    }
                });

                return null;
            }
        });
    }


    @Override
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        // in MVCC mode h2 doesn't shut down immediately on all connections closed, so need random db name
        ds.setURL("jdbc:h2:mem:test" + new Random().nextInt() +";MVCC=TRUE");

        dbi = new DBI(ds);

        dbi.registerMapper(new SomethingMapper());

        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @Override
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }
}
