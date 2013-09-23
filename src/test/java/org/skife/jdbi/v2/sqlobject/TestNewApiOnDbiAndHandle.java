package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;

import java.util.UUID;

import junit.framework.TestCase;

public class TestNewApiOnDbiAndHandle extends TestCase
{
    private DBI    dbi;
    private Handle handle;

    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new SomethingMapper());
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    public void testOpenNewSpiffy() throws Exception
    {
        Spiffy spiffy = dbi.open(Spiffy.class);
        try {
            spiffy.insert(new Something(1, "Tim"));
            spiffy.insert(new Something(2, "Diego"));

            assertEquals("Diego", spiffy.findNameById(2));
        }
        finally {
            dbi.close(spiffy);
        }
        assertTrue(spiffy.getHandle().getConnection().isClosed());
    }

    public void testOnDemandSpiffy() throws Exception
    {
        Spiffy spiffy = dbi.onDemand(Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertEquals("Diego", spiffy.findNameById(2));
    }

    public void testAttach() throws Exception
    {
        Spiffy spiffy = handle.attach(Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertEquals("Diego", spiffy.findNameById(2));
    }


    static interface Spiffy extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@Bind(value = "it", binder = SomethingBinderAgainstBind.class) Something s);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }

}
