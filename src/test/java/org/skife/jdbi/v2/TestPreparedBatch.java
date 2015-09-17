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
package org.skife.jdbi.v2;

import org.junit.Test;
import org.skife.jdbi.derby.DerbyHelper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.StringMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class TestPreparedBatch extends DBITestCase
{
    @Test
    public void testDesignApi() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        PreparedBatchPart p = b.add();
        p = p.bind("id", 1).bind("name", "Eric").next();
        p.bind("id", 2).bind("name", "Brian").next().bind("id", 3).bind("name", "Keith");
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").map(Something.class).list();
        assertEquals(3, r.size());
        assertEquals("Keith", r.get(2).getName());
    }

    @Test
    public void testBigishBatch() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        int count = 100;
        for (int i = 0; i < count; ++i)
        {
            b.add().bind("id", i).bind("name", "A Name");

        }
        b.execute();

        int row_count = h.createQuery("select count(id) from something").map(new ResultSetMapper<Integer>()
        {
            @Override
            public Integer map(int index, ResultSet r, StatementContext ctx) throws SQLException
            {
                return r.getInt(1);
            }
        }).first();

        assertEquals(count, row_count);
    }

    @Test
    public void testBindProperties() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (?, ?)");

        b.add(0, "Keith");
        b.add(1, "Eric");
        b.add(2, "Brian");

        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").map(Something.class).list();
        assertEquals(3, r.size());
        assertEquals("Brian", r.get(2).getName());
    }

    @Test
    public void testBindMaps() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        Map<String, Object> one = DerbyHelper.map("id", 0).add("name", "Keith");
        b.add(one);
        b.add(DerbyHelper.map("id", Integer.parseInt("1")).add("name", "Eric"));
        b.add(DerbyHelper.map("id", Integer.parseInt("2")).add("name", "Brian"));

        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").map(Something.class).list();
        assertEquals(3, r.size());
        assertEquals("Brian", r.get(2).getName());
    }

    @Test
    public void testMixedModeBatch() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        Map<String, Object> one = DerbyHelper.map("id", 0);
        b.add(one).bind("name", "Keith");
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").map(Something.class).list();
        assertEquals(1, r.size());
        assertEquals("Keith", r.get(0).getName());
    }

    @Test
    public void testPositionalBinding() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.add().bind(0, 0).bind(1, "Keith").submit().execute();

        List<Something> r = h.createQuery("select * from something order by id").map(Something.class).list();
        assertEquals(1, r.size());
        assertEquals("Keith", r.get(0).getName());
    }

    @Test
    public void testSetOnTheBatchItself() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bind("id", 1);
        b.bind("name", "Jeff");
        b.add();

        b.bind("id", 2);
        b.bind("name", "Tom");
        b.add();

        b.execute();

        assertEquals(h.createQuery("select name from something order by id").map(StringMapper.FIRST).list(),
                     Arrays.asList("Jeff", "Tom"));

    }

    @Test
    public void testMixedBatchSetting() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bind("id", 1);
        b.add().bind("name", "Jeff");

        b.bind("id", 2);
        b.add().bind("name", "Tom");

        b.execute();

        assertEquals(h.createQuery("select name from something order by id").map(StringMapper.FIRST).list(),
                     Arrays.asList("Jeff", "Tom"));
    }
}
