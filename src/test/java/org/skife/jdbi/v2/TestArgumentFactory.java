package org.skife.jdbi.v2;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;
import org.skife.jdbi.v2.util.IntegerMapper;
import org.skife.jdbi.v2.util.StringMapper;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestArgumentFactory
{
    private DBI    dbi;
    private Handle h;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:");
        h = dbi.open();

        h.execute("create table something (id int primary key, name varchar(100))");

    }

    @After
    public void tearDown() throws Exception
    {
        h.execute("drop table something");
        h.close();
    }


    @Test
    @Ignore("h2 does not implement jdbc4 so this fails")
    public void testSQLArrayStuff() throws Exception
    {
        h.registerArgumentFactory(new ArgumentFactory<Collection<String>>()
        {
            public boolean accepts(Class<?> expectedType, Object value, StatementContext ctx)
            {

                return value instanceof Collection;
            }

            public Argument build(Class<?> expectedType, Collection<String> value, StatementContext ctx)
            {
                try {
                    final Array ary = ctx.getConnection().createArrayOf("varchar", value.toArray());
                    return new Argument()
                    {
                        public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
                        {
                            statement.setArray(position, ary);
                        }
                    };
                }
                catch (SQLException e) {
                    throw new IllegalStateException("bonkers creating array!");
                }
            }
        });

        h.execute("insert into something (id, name) values (1, 'Brian'), (2, 'Henning'), (3, 'Tom')");

        List<Integer> rs = h.createQuery("select id from something where name in :names order by id")
                            .map(IntegerMapper.FIRST)
                            .bind("names", Arrays.asList("Brian", "Tom"))
                            .list();
        assertThat(rs.get(0), equalTo(1));
        assertThat(rs.get(0), equalTo(3));

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
        public boolean accepts(Class<?> expectedType, Object value, StatementContext ctx)
        {
            return expectedType == Object.class && value instanceof Name;
        }

        public Argument build(Class<?> expectedType, Name value, StatementContext ctx)
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
