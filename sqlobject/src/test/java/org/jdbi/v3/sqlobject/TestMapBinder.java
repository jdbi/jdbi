/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.sqlobject;

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.sqlobject.customizers.UseRowMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestMapBinder
{
    private Jdbi    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        dbi = Jdbi.create(ds).installPlugin(new SqlObjectPlugin());
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100), a varchar(100), b int, c varchar(100))");
    }

    @After
    public void cleanUp()
    {
        handle.close();
    }

    @Test
    public void testInsert() throws Exception
    {
        Spiffy s = handle.attach(Spiffy.class);
        s.insert(allMap(5, "woo", 3, "too"));

        Result elem = s.load(5);
        assertEquals("too", elem.c);
        assertEquals(3, elem.b);
    }

    @Test
    public void testUpdate() throws Exception
    {
        Spiffy s = handle.attach(Spiffy.class);
        s.insert(allMap(4, "woo", 1, "too"));
        Map<String, Object> update = new HashMap<>();

        update.put("a", "goo");
        update.put("b", 2);
        update.put("c", null);

        assertEquals(1, s.update(4, update));

        Result elem = s.load(4);
        assertEquals("goo", elem.a);
        assertEquals(2, elem.b);
        assertEquals("too", elem.c);
    }

    @Test
    public void testUpdatePrefix() throws Exception
    {
        Spiffy s = handle.attach(Spiffy.class);
        s.insert(allMap(4, "woo", 1, "too"));
        Map<Object, Object> update = new HashMap<>();

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
        Map<String, Object> map = new HashMap<>();
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
        @UseRowMapper(ResultMapper.class)
        Result load(@Bind("id") int id);
    }

    static class ResultMapper implements RowMapper<Result>
    {
        @Override
        public Result map(ResultSet r, StatementContext ctx)
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
