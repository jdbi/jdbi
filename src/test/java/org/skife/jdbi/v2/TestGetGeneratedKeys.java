package org.skife.jdbi.v2;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.tweak.HandleCallback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestGetGeneratedKeys
{

    private JdbcConnectionPool ds;
    private DBI                dbi;

    @Before
    public void setUp() throws Exception
    {
        ds = JdbcConnectionPool.create("jdbc:h2:mem:test",
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

    @Test
    public void testFoo() throws Exception
    {
        Connection conn = ds.getConnection();
        PreparedStatement stmt = conn.prepareStatement("insert into something (name) values ('Waldo')");
        stmt.execute();
        ResultSet rs = stmt.getGeneratedKeys();
        assertThat(rs.next(), equalTo(true));
        assertThat(rs.getLong(1), equalTo(1L));
        assertThat(rs.next(), equalTo(false));
        stmt.close();
        conn.close();
    }
}
