package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.SimpleResultSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skife.jdbi.v2.CallableStatementMapper;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.OutParameters;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.jdbc.core.SqlTypeValue;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestSqlCall
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:test");

        dbi.setSQLLog(new PrintStreamLog(System.out));
        handle = dbi.open();
        handle.execute("create table something( id integer primary key, name varchar(100) )");
        handle.execute("CREATE ALIAS stored_insert FOR \"org.skife.jdbi.v2.sqlobject.TestSqlCall.insertSomething\";");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void testFoo() throws Exception
    {
        Dao dao = handle.attach(Dao.class);
//        OutParameters out = handle.createCall(":num = call stored_insert(:id, :name)")
//                                  .bind("id", 1)
//                                  .bind("name", "Jeff")
//                                  .registerOutParameter("num", Types.INTEGER)
//                                  .invoke();
        dao.insert(1, "Jeff");

        assertThat(handle.attach(Dao.class).findById(1), equalTo(new Something(1, "Jeff")));
    }

    public static interface Dao
    {
        @SqlCall("call stored_insert(:id, :name)")
        public void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        @RegisterMapper(SomethingMapper.class)
        Something findById(@Bind("id") int id);
    }


    public static int insertSomething(Connection conn, int id, String name) throws SQLException
    {

        PreparedStatement stmt = conn.prepareStatement("insert into something (id, name) values (?, ?)");
        stmt.setInt(1, id);
        stmt.setString(2, name);
        return stmt.executeUpdate();
    }
}
