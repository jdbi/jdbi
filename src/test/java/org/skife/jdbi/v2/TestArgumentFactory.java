package org.skife.jdbi.v2;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.sqlobject.SomethingMapper;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestArgumentFactory
{
    private DBI                dbi;
    private JdbcConnectionPool ds;
    private Handle             h;

    @Before
    public void setUp() throws Exception
    {
        ds = JdbcConnectionPool.create("jdbc:h2:mem:test",
                                       "username",
                                       "password");
        dbi = new DBI(ds);
        this.h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
    }

    @After
    public void tearDown() throws Exception
    {
        h.close();
        ds.dispose();
    }

    @Test
    public void testRegisterOnDBI() throws Exception
    {
        dbi.registerArgumentFactory(new NameAF());
        Handle h2 = dbi.open();
        h2.createStatement("insert into something (id, name) values (:id, :name)")
          .bind("id", 7)
          .bind("name", new Name("Brian", "McCallister"))
          .execute();

        String full_name = h.createQuery("select name from something where id = 7").map(StringMapper.FIRST).first();

        assertThat(full_name, equalTo("Brian McCallister"));
        h2.close();
    }

    @Test
    public void testRegisterOnHandle() throws Exception
    {
        h.registerArgumentFactory(new NameAF());
        h.createStatement("insert into something (id, name) values (:id, :name)")
         .bind("id", 7)
         .bind("name", new Name("Brian", "McCallister"))
         .execute();

        String full_name = h.createQuery("select name from something where id = 7").map(StringMapper.FIRST).first();

        assertThat(full_name, equalTo("Brian McCallister"));
    }

    @Test
    public void testRegisterOnStatement() throws Exception
    {
        h.createStatement("insert into something (id, name) values (:id, :name)")
         .registerArgumentFactory(new NameAF())
         .bind("id", 1)
         .bind("name", new Name("Brian", "McCallister"))
         .execute();
    }

    @Test
    public void testOnPreparedBatch() throws Exception
    {
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        batch.registerArgumentFactory(new NameAF());

        batch.add().bind("id", 1).bind("name", new Name("Brian", "McCallister"));
        batch.add().bind("id", 2).bind("name", new Name("Henning", "S"));
        batch.execute();

        List<String> rs = h.createQuery("select name from something order by id")
                           .map(StringMapper.FIRST)
                           .list();

        assertThat(rs.get(0), equalTo("Brian McCallister"));
        assertThat(rs.get(1), equalTo("Henning S"));
    }

    public static class NameAF implements ArgumentFactory<Name>
    {
        public boolean accepts(Class<? super Name> expectedType, Object value, StatementContext ctx)
        {
            return expectedType == Object.class && value instanceof Name;
        }

        public Argument build(Class<? super Name> expectedType, Name value, StatementContext ctx)
        {
            return new StringArgument(value.getFullName());
        }
    }

    public static class Name
    {
        private final String first;
        private final String last;

        public Name(String first, String last)
        {

            this.first = first;
            this.last = last;
        }

        public String getFullName()
        {
            return first + " " + last;
        }

        public String toString()
        {
            return "<Name first=" + first + " last=" + last + " >";
        }
    }

}
