package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;

import java.util.UUID;

import junit.framework.TestCase;

public class TestReturningQuery extends TestCase
{
    private DBI    dbi;
    private Handle handle;


    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    public void testWithRegisteredMapper() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        dbi.registerMapper(new SomethingMapper());

        Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class);

        Something s = spiffy.findById(7)
                            .first();

        assertEquals("Tim", s.getName());
    }

    public void testWithExplicitMapper() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        Spiffy2 spiffy = SqlObjectBuilder.open(dbi, Spiffy2.class);

        Something s = spiffy.findByIdWithExplicitMapper(7)
                            .first();

        assertEquals("Tim", s.getName());
    }

    public static interface Spiffy extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :id")
        public Query<Something> findById(@Bind("id") int id);
    }

    public static interface Spiffy2 extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        public Query<Something> findByIdWithExplicitMapper(@Bind("id") int id);
    }
}
