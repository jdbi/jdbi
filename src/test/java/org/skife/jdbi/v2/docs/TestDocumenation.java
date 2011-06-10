package org.skife.jdbi.v2.docs;

import org.h2.jdbcx.JdbcConnectionPool;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.util.StringMapper;

import javax.sql.DataSource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestDocumenation
{

    @Before
    public void setUp() throws Exception
    {

    }

    @Test
    public void testFiveMinuteFluentApi() throws Exception
    {
        // using in-memory H2 database via a pooled DataSource
        JdbcConnectionPool ds = JdbcConnectionPool.create("jdbc:h2:mem:test",
                                                          "username",
                                                          "password");
        DBI dbi = new DBI(ds);
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");

        h.execute("insert into something (id, name) values (?, ?)", 1, "Brian");

        String name = h.createQuery("select name from something where id = :id")
                       .bind("id", 1)
                       .map(StringMapper.FIRST)
                       .first();
        assertThat(name, equalTo("Brian"));

        h.close();
        ds.dispose();
    }

    public interface MyDAO
    {
        @SqlUpdate("create table something (id int primary key, name varchar(100))")
        void createSomethingTable();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);

        /**
         * close with no args is used to close the connection
         */
        void close();
    }

    @Test
    public void testFiveMinuteSqlObjectExample() throws Exception
    {
        // using in-memory H2 database via a pooled DataSource
        JdbcConnectionPool ds = JdbcConnectionPool.create("jdbc:h2:mem:test2",
                                                          "username",
                                                          "password");
        DBI dbi = new DBI(ds);

        MyDAO dao = dbi.open(MyDAO.class);

        dao.createSomethingTable();

        dao.insert(2, "Aaron");

        String name = dao.findNameById(2);

        assertThat(name, equalTo("Aaron"));

        dao.close();
        ds.dispose();
    }

}
