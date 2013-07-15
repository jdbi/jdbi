package org.jdbi.v3.sqlobject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.jdbi.v3.tweak.ResultSetMapper;

import junit.framework.TestCase;

public class TestMapBinder extends TestCase
{
    private DBI    dbi;
    private Handle handle;

    @Override
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100), a varchar(100), b int, c varchar(100))");
    }

    @Override
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }


    public void testInsert() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(allMap(5, "woo", 3, "too"));

        Result elem = s.load(5);
        assertEquals("too", elem.c);
        assertEquals(3, elem.b);
    }


    public void testUpdate() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(allMap(4, "woo", 1, "too"));
        Map<String, Object> update = new HashMap<String, Object>();

        update.put("a", "goo");
        update.put("b", 2);
        update.put("c", null);

        assertEquals(1, s.update(4, update));

        Result elem = s.load(4);
        assertEquals("goo", elem.a);
        assertEquals(2, elem.b);
        assertEquals("too", elem.c);
    }

    public void testUpdatePrefix() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(allMap(4, "woo", 1, "too"));
        Map<Object, Object> update = new HashMap<Object, Object>();

        update.put("b", 2);
        update.put(new A(), "goo");

        assertEquals(1, s.updatePrefix(4, update));

        Result elem = s.load(4);
        assertEquals("goo", elem.a);
        assertEquals(1, elem.b); // filtered out by annotation value
        assertEquals("too", elem.c);
    }

    private Map<String, Object> allMap(int id, Object a, int b, Object c)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", id);
        map.put("a", a);
        map.put("b", b);
        map.put("c", c);
        return map;
    }

    interface Spiffy
    {
        @SqlUpdate("insert into something (id, a, b, c) values (:id, :a, :b, :c)")
        int insert(@BindMap Map<String, Object> bindings);

        @SqlUpdate("update something set a=coalesce(:a, a), b=coalesce(:b, b), c=coalesce(:c, c) where id=:id")
        int update(@Bind("id") int id, @BindMap Map<String, Object> updates);

        @SqlUpdate("update something set a=coalesce(:asdf.a, a), c=coalesce(:asdf.c, c) where id=:id")
        int updatePrefix(@Bind("id") int id, @BindMap(prefix="asdf", value={"a","c"}, implicitKeyStringConversion=true) Map<Object, Object> updates);

        @SqlQuery("select * from something where id = :id")
        @Mapper(ResultMapper.class)
        Result load(@Bind("id") int id);
    }

    static class ResultMapper implements ResultSetMapper<Result>
    {
        @Override
        public Result map(int index, ResultSet r, StatementContext ctx)
        throws SQLException
        {
            Result ret = new Result();
            ret.id = r.getInt("id");
            ret.a = r.getString("a");
            ret.b = r.getInt("b");
            ret.c = r.getString("c");
            return ret;
        }
    }

    static class Result
    {
        String a, c;
        int id, b;
    }

    static class A
    {
        @Override
        public String toString()
        {
            return "a";
        }
    }
}
