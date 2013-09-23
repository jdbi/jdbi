package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TestObjectMethods
{

    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new SomethingMapper());
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
    public void testToString() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        assertThat(dao.toString(), containsString(DAO.class.getName()));
    }

    @Test
    public void testEquals() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        assertThat(dao, equalTo(dao));
    }

    @Test
    public void testNotEquals() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        DAO oad = handle.attach(DAO.class);
        assertThat(dao, not(equalTo(oad)));
    }

    @Test
    public void testHashCodeDiff() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        DAO oad = handle.attach(DAO.class);
        assertThat(dao.hashCode(), not(equalTo(oad.hashCode())));
    }

    @Test
    public void testHashCodeMatch() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        assertThat(dao.hashCode(), equalTo(dao.hashCode()));
    }


    public static interface DAO
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id")long id, @Bind("name") String name);
    }
}
