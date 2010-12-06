package org.skife.jdbi.v2.unstable.eod;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;

import java.io.Closeable;
import java.util.List;

public class TestApiDesign extends TestCase
{

    private DBI dbi;
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


    public void testFoo() throws Exception
    {
        SqlObjectBuilder b = new SqlObjectBuilder(dbi);

        MyDAO db = b.open(MyDAO.class);

        QueryObject<Something> ranger = db.findByIdRange(0, 10);
        ranger.setTimeout(10);
        ranger.setFetchSize(10);
        List<Something> rs = ranger.list();

        List<Something> rs2 = db.findByPattern("%ia%");

        db.close();

    }

    public interface MyDAO extends Closeable {

        @Sql("select id, name from something where name like :pattern")
        public List<Something> findByPattern(@Bind("pattern") String pattern);

        @Sql("select id, name from something where id > :from and id < :to")
        public QueryObject<Something> findByIdRange(@Bind("from") int from, @Bind("to") int to);
    }


}
