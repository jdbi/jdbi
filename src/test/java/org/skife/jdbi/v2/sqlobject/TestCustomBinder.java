package org.skife.jdbi.v2.sqlobject;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.binders.Bind;

public class TestCustomBinder extends TestCase
{
    private DBI    dbi;
    private Handle handle;


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

    public void testFoo() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'Martin')");
        Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class);

        Something s = spiffy.findSame(new Something(2, "Unknown"));

        assertEquals("Martin", s.getName());

        spiffy.close();
    }

    public void testCustomBindingAnnotation() throws Exception
    {
        Spiffy s = handle.attach(Spiffy.class);

        s.insert(new Something(2, "Keith"));

        assertEquals("Keith", s.findNameById(2));
    }


    public static interface Spiffy extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :it.id")
        @Mapper(SomethingMapper.class)
        public Something findSame(@Bind(value = "it", binder = SomethingBinderAgainstBind.class) Something something);

        @SqlUpdate("insert into something (id, name) values (:s.id, :s.name)")
        public int insert(@BindSomething("s") Something something);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int i);
    }
}
