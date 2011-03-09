package org.skife.jdbi.v2.unstable.eod;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.Iterator;

public class TestOnDemandSqlObject extends TestCase
{
    private DBI    dbi;
    private Handle handle;


    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;MVCC=TRUE");
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }


    public void testAPIWorks() throws Exception
    {
        Spiffy s = EOD.onDemand(dbi, Spiffy.class);

        s.insert(7, "Bill");

        String bill = handle.createQuery("select name from something where id = 7").map(StringMapper.FIRST).first();

        assertEquals("Bill", bill);
    }

    public void testTransactionBindsTheHandle() throws Exception
    {
        TransactionStuff txl = EOD.onDemand(dbi, TransactionStuff.class);
        TransactionStuff tx2 = EOD.onDemand(dbi, TransactionStuff.class);

        txl.insert(8, "Mike");

        txl.begin();

        assertSame(txl.getHandle(), txl.getHandle());

        txl.updateName(8, "Miker");
        assertEquals("Miker", txl.byId(8).getName());
        assertEquals("Mike", tx2.byId(8).getName());

        txl.commit();

        assertNotSame(txl.getHandle(), txl.getHandle());

        assertEquals("Miker", tx2.byId(8).getName());
    }

    public void testIteratorBindsTheHandle() throws Exception
    {
        Spiffy s = EOD.onDemand(dbi, Spiffy.class);

        s.insert(1, "Tom");
        s.insert(2, "Sam");

        assertNotSame(s.getHandle(), s.getHandle());

        Iterator<Something> all = s.findAll();
        assertSame(s.getHandle(), s.getHandle());

        all.next();
        assertSame(s.getHandle(), s.getHandle());
        all.next();
        assertFalse(all.hasNext());

        assertNotSame(s.getHandle(), s.getHandle());

    }

    public static interface Spiffy extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);

        @SqlQuery("select name, id from something")
        @Mapper(SomethingMapper.class)
        Iterator<Something> findAll();
    }

    public static interface TransactionStuff extends GetHandle, Transactional<TransactionStuff>
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        public Something byId(@Bind("id") long id);

        @SqlUpdate("update something set name = :name where id = :id")
        public void updateName(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);
    }
}
