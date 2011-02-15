package org.skife.jdbi.v2.unstable.eod;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestReturningQuery extends TestCase
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

    public void testWithRegisteredMapper() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        SqlObjectBuilder b = new SqlObjectBuilder(dbi);
        b.addMapper(new SomethingMapper());

        Spiffy spiffy = b.open(Spiffy.class);


        Something s = spiffy.findById(7)
                            .first();
        assertEquals("Tim", s.getName());
    }

    public void testWithExplicitMapper() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        SqlObjectBuilder b = new SqlObjectBuilder(dbi);
        Spiffy2 spiffy = b.open(Spiffy2.class);

        Something s = spiffy.findByIdWithExplicitMapper(7)
                            .first();
        assertEquals("Tim", s.getName());
    }

    public static interface Spiffy extends Closeable
    {
        @Sql("select id, name from something where id = :id")
        public Query<Something> findById(@Bind("id") int id);
    }

    public static interface Spiffy2 extends Closeable
    {
        @Sql("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        public Query<Something> findByIdWithExplicitMapper(@Bind("id") int id);


    }

    public static class SomethingMapper implements ResultSetMapper<Something>
    {
        public Something map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            return new Something(r.getInt("id"), r.getString("name"));
        }
    }
}
