package org.skife.jdbi.v2.sqlobject;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;

public class TestVariousOddities extends TestCase
{
    private DBI dbi;
    private Handle handle;


    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem");
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    public void testAttach() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(new Something(14, "Tom"));

        Something tom = s.byId(14);
        assertEquals("Tom", tom.getName());
    }

    public void testRegisteredMappersWork() throws Exception
    {

    }

    public void testEquals()
    {
        Spiffy s1 = SqlObjectBuilder.attach(handle, Spiffy.class);
        Spiffy s2 = SqlObjectBuilder.attach(handle, Spiffy.class);
        assertEquals(s1, s1);
        assertNotSame(s1, s2);
        assertFalse(s1.equals(s2));
    }

    public void testToString()
    {
        Spiffy s1 = SqlObjectBuilder.attach(handle, Spiffy.class);
        Spiffy s2 = SqlObjectBuilder.attach(handle, Spiffy.class);
        assertNotNull(s1.toString());
        assertNotNull(s2.toString());
        assertTrue(s1.toString() != s2.toString());
    }

    public void testHashCode()
    {
        Spiffy s1 = SqlObjectBuilder.attach(handle, Spiffy.class);
        Spiffy s2 = SqlObjectBuilder.attach(handle, Spiffy.class);
        assertFalse(0 == s1.hashCode());
        assertFalse(0 == s2.hashCode());
        assertTrue(s1.hashCode() != s2.hashCode());
    }

    public static interface Spiffy extends CloseMe
    {

        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        public Something byId(@Bind("id") long id);

        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        public void insert(@Bind(value = "it", binder = SomethingBinderAgainstBind.class) Something it);
    }

}
