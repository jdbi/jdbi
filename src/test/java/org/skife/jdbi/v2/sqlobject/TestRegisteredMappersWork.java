package org.skife.jdbi.v2.sqlobject;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.DBIException;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TestRegisteredMappersWork
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testRegistered() throws Exception
    {
        handle.registerMapper(new SomethingMapper());

        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);

        s.insert(1, "Tatu");

        Something t = s.byId(1);
        assertEquals(1, t.getId());
        assertEquals("Tatu", t.getName());
    }

    @Test
    public void testBuiltIn() throws Exception
    {

        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);

        s.insert(1, "Tatu");

        assertEquals("Tatu", s.findNameBy(1));
    }

    @Test
    public void testRegisterMapperAnnotationWorks() throws Exception
    {
        Kabob bob = dbi.onDemand(Kabob.class);

        bob.insert(1, "Henning");
        Something henning = bob.find(1);

        assertThat(henning, equalTo(new Something(1, "Henning")));
    }

    @Test(expected = DBIException.class)
    public void testNoRootRegistrations() throws Exception
    {
        Handle h = dbi.open();
        h.insert("insert into something (id, name) values (1, 'Henning')");
        try {
            Something henning = h.createQuery("select id, name from something where id = 1")
                .mapTo(Something.class)
                .first();
            fail("should have raised an exception");
        }
        finally {
            h.close();
        }
    }

    public static interface Spiffy extends CloseMe
    {

        @SqlQuery("select id, name from something where id = :id")
        public Something byId(@Bind("id") long id);

        @SqlQuery("select name from something where id = :id")
        public String findNameBy(@Bind("id") long id);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);
    }


    @RegisterMapper(MySomethingMapper.class)
    public static interface Kabob
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        public Something find(@Bind("id") int id);
    }

    public static class MySomethingMapper implements ResultSetMapper<Something>
    {
        public Something map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            return new Something(r.getInt("id"), r.getString("name"));
        }
    }

}
