package org.skife.jdbi.v2.docs;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.ResultIterator;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.skife.jdbi.v2.ExtraMatchers.equalsOneOf;

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

    @Test
    public void testMappingExample() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (1, 'Brian')");
        h.execute("insert into something (id, name) values (2, 'Keith')");


        Query<Map<String, Object>> q =
            h.createQuery("select name from something order by id");
        Query<String> q2 = q.map(StringMapper.FIRST);
        List<String> rs = q2.list();

        assertThat(rs, equalTo(asList("Brian", "Keith")));

        h.close();
    }

    @Test
    public void testMappingExampleChained() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (1, 'Brian')");
        h.execute("insert into something (id, name) values (2, 'Keith')");


        List<String> rs = h.createQuery("select name from something order by id")
            .map(StringMapper.FIRST)
            .list();

        assertThat(rs, equalTo(asList("Brian", "Keith")));

        h.close();
    }

    @Test
    public void testMappingExampleChainedFirst() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (1, 'Brian')");
        h.execute("insert into something (id, name) values (2, 'Keith')");


        String rs = h.createQuery("select name from something order by id")
            .map(StringMapper.FIRST)
            .first();

        assertThat(rs, equalTo("Brian"));

        h.close();
    }

    @Test
    public void testMappingExampleChainedIterator() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (1, 'Brian')");
        h.execute("insert into something (id, name) values (2, 'Keith')");


        ResultIterator<String> rs = h.createQuery("select name from something order by id")
            .map(StringMapper.FIRST)
            .iterator();

        assertThat(rs.next(), equalTo("Brian"));
        assertThat(rs.next(), equalTo("Keith"));
        assertThat(rs.hasNext(), equalTo(false));

        rs.close();

        h.close();
    }


    @Test
    public void testMappingExampleChainedIterator2() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (1, 'Brian')");
        h.execute("insert into something (id, name) values (2, 'Keith')");


        Iterator<String> rs = h.createQuery("select name from something order by id")
            .map(StringMapper.FIRST)
            .iterator();

        assertThat(rs.next(), equalTo("Brian"));
        assertThat(rs.next(), equalTo("Keith"));
        assertThat(rs.hasNext(), equalTo(false));

        h.close();
    }

    @Test
    public void testMappingExampleChainedIterator3() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (1, 'Brian')");
        h.execute("insert into something (id, name) values (2, 'Keith')");

        for (String name : h.createQuery("select name from something order by id").map(StringMapper.FIRST))
        {
            assertThat(name, equalsOneOf("Brian", "Keith"));
        }

        h.close();
    }


}
