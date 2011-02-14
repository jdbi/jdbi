package org.skife.jdbi.v2.unstable.eod;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.io.Closeable;
import java.util.List;

public class TestSqlObjectBuilder extends TestCase
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
        handle.close();
    }


    public void _testFoo() throws Exception
    {
        SqlObjectBuilder b = new SqlObjectBuilder(dbi);
        Mine m = b.open(Mine.class);

        List<String> names = m.selectAllNames();

        m.close();
    }

    public void testSanity() throws Exception
    {
        assertEquals(1 + 1, 2);
    }


    public static interface Mine extends Closeable
    {

        @Sql("select name from something")
        public List<String> selectAllNames();
    }
}
