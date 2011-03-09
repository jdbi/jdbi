package org.skife.jdbi.v2.unstable.eod;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.unstable.eod.customizers.FetchSize;
import org.skife.jdbi.v2.unstable.eod.customizers.MaxRows;
import org.skife.jdbi.v2.unstable.eod.customizers.QueryTimeOut;

import java.util.List;

public class TestModifiers extends TestCase
{
    private DBI    dbi;
    private Handle handle;


    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
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

    public void testFetchSizeAsArgOnlyUsefulWhenSteppingThroughDebuggerSadly() throws Exception
    {
        Spiffy s = EOD.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAll(1);
        assertEquals(3, things.size());
    }

    public void testFetchSizeOnMethodOnlyUsefulWhenSteppingThroughDebuggerSadly() throws Exception
    {
        Spiffy s = EOD.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAll();
        assertEquals(3, things.size());
    }

    public void testMaxSizeOnMethod() throws Exception
    {
        Spiffy s = EOD.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAllWithMaxRows();
        assertEquals(1, things.size());
    }

    public void testMaxSizeOnParam() throws Exception
    {
        Spiffy s = EOD.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAllWithMaxRows(2);
        assertEquals(2, things.size());
    }

    public void testQueryTimeOutOnMethodOnlyUsefulWhenSteppingThroughDebuggerSadly() throws Exception
    {
        Spiffy s = EOD.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAllWithQueryTimeOut();
        assertEquals(3, things.size());
    }

    public void testQueryTimeOutOnParamOnlyUsefulWhenSteppingThroughDebuggerSadly() throws Exception
    {
        Spiffy s = EOD.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAllWithQueryTimeOut(2);
        assertEquals(3, things.size());
    }

    public static interface Spiffy
    {
        @SqlQuery("select id, name from something where id = :id")
        public Something byId(@Bind("id") long id);

        @SqlQuery("select id, name from something")
        public List<Something> findAll(@FetchSize(1) int fetchSize);

        @SqlQuery("select id, name from something")
        @FetchSize(2)
        public List<Something> findAll();


        @SqlQuery("select id, name from something")
        public List<Something> findAllWithMaxRows(@MaxRows(1) int fetchSize);

        @SqlQuery("select id, name from something")
        @MaxRows(1)
        public List<Something> findAllWithMaxRows();

        @SqlQuery("select id, name from something")
        public List<Something> findAllWithQueryTimeOut(@QueryTimeOut(1) int timeOutInSeconds);

        @SqlQuery("select id, name from something")
        @QueryTimeOut(1)
        public List<Something> findAllWithQueryTimeOut();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);
    }


}
