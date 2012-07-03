package org.skife.jdbi.v2.sqlobject;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

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
    public void testGetHandleProvidesSeperateHandle() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);
        Handle h = dao.getHandle();

        try {
            h.execute("insert into something (id, name) values (1, 'Stephen')");
            fail("should have raised exception, connection will be closed at this point");
        }
        catch (UnableToCreateStatementException e) {
            // happy path
        }
    }

    @Test
    public void testHandleReentrant() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);

        dao.withHandle(new HandleCallback<Void>()
        {
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

        dao.withHandle(new HandleCallback<Void>()
        {
            @Override
            public Void withHandle(Handle handle) throws Exception
            {
                handle.inTransaction(new TransactionCallback<Void>()
                {
                    @Override
                    public Void inTransaction(Handle conn, TransactionStatus status) throws Exception
                    {
                        dao.insert(new Something(1, "x"));

                        List<String> rs = conn.createQuery("select name from something where id = 1")
                                              .map(StringMapper.FIRST)
                                              .list();
                        assertThat(rs.size(), equalTo(1));

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
        ds.setURL("jdbc:h2:mem:test" + new Random().nextInt() + ";MVCC=TRUE");

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
