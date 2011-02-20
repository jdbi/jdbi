package org.skife.jdbi.v2.unstable.eod;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.util.StringMapper;

public class TestOnDemandSqlObject extends TestCase
{
    private DBI    dbi;
    private     Handle handle;


    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }


    public void testGetHandle() throws Exception
    {
        Spiffy s = EOD.onDemand(dbi, Spiffy.class);

        s.insert(7, "Bill");

        String bill = handle.createQuery("select name from something where id = 7").map(StringMapper.FIRST).first();

        assertEquals("Bill", bill);
    }

    public static interface Spiffy {
        @SqlUpdate("insert into something (id, name) values (:id, :name")
        public void insert(@Bind("id") long id, @Bind("name") String name);
    }

}
