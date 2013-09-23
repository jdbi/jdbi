package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.tweak.HandleCallback;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestGetGeneratedKeys
{
    private JdbcConnectionPool ds;
    private DBI                dbi;

    @Before
    public void setUp() throws Exception
    {
        ds = JdbcConnectionPool.create("jdbc:h2:mem:" + UUID.randomUUID(),
                                       "username",
                                       "password");
        dbi = new DBI(ds);
        dbi.withHandle(new HandleCallback<Object>()
        {
            public Object withHandle(Handle handle) throws Exception
            {
                handle.execute("create table something (id identity primary key, name varchar(32))");
                return null;
            }
        });
    }

    @After
    public void tearDown() throws Exception
    {
        ds.dispose();
    }

    public static interface DAO extends CloseMe
    {
        @SqlUpdate("insert into something (name) values (:it)")
        @GetGeneratedKeys
        public long insert(@Bind String name);

        @SqlQuery("select name from something where id = :it")
        public String findNameById(@Bind long id);
    }

    @Test
    public void testFoo() throws Exception
    {
        DAO dao = dbi.open(DAO.class);

        long brian_id = dao.insert("Brian");
        long keith_id = dao.insert("Keith");

        assertThat(dao.findNameById(brian_id), equalTo("Brian"));
        assertThat(dao.findNameById(keith_id), equalTo("Keith"));

        dao.close();
    }

}
