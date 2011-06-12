package org.skife.jdbi.v2.docs;

import org.h2.jdbcx.JdbcConnectionPool;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Folder2;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.StringMapper;

import javax.sql.DataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");

        h.execute("insert into something (id, name) values (?, ?)", 1, "Brian");

        String name = h.createQuery("select name from something where id = :id")
                       .bind("id", 1)
                       .map(StringMapper.FIRST)
                       .first();
        assertThat(name, equalTo("Brian"));

        h.close();
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
        DBI dbi = new DBI("jdbc:h2:mem:test");

        MyDAO dao = dbi.open(MyDAO.class);

        dao.createSomethingTable();

        dao.insert(2, "Aaron");

        String name = dao.findNameById(2);

        assertThat(name, equalTo("Aaron"));

        dao.close();
    }


    @Test
    public void testObtainHandleViaOpen() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle handle = dbi.open();

        // make sure to close it!
        handle.close();
    }

    @Test
    public void testObtainHandleInCallback() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        dbi.withHandle(new HandleCallback<Void>()
        {
            public Void withHandle(Handle handle) throws Exception
            {
                handle.execute("create table silly (id int)");
                return null;
            }
        });
    }

    @Test
    public void testExecuteSomeStatements() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();

        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (?, ?)", 3, "Patrick");

        List<Map<String, Object>> rs = h.select("select id, name from something");
        assertThat(rs.size(), equalTo(1));

        Map<String, Object> row = rs.get(0);
        assertThat((Integer) row.get("id"), equalTo(3));
        assertThat((String) row.get("name"), equalTo("Patrick"));

        h.close();
    }

    @Test
    public void testFluentUpdate() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");

        h.createStatement("insert into something(id, name) values (:id, :name)")
         .bind("id", 4)
         .bind("name", "Martin")
         .execute();

        h.close();
    }

    @Test
    public void testFold() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (7, 'Mark')");
        h.execute("insert into something (id, name) values (8, 'Tatu')");


        StringBuilder rs = h.createQuery("select name from something order by id")
                            .map(StringMapper.FIRST)
                            .fold(new StringBuilder(), new Folder2<StringBuilder>()
                            {
                                public StringBuilder fold(StringBuilder acc, ResultSet rs, StatementContext ctx) throws SQLException
                                {
                                    acc.append(rs.getString(1)).append(", ");
                                    return acc;
                                }
                            });
        rs.delete(rs.length() - 2, rs.length()); // trim the extra ", "
        assertThat(rs.toString(), equalTo("Mark, Tatu"));
        h.close();
    }
}
