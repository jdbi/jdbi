/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.*;

import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestPreparedBatch extends DBITestCase
{
    public void testDesignApi() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b = b.setInteger("id", 1).setString("name", "Eric").addAnother();
        b.setInteger("id", 2).setString("name", "Brian").addAnother()
                .setInteger("id", 3).setString("name", "Keith");

        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").map(Something.class).list();
        assertEquals(3, r.size());
        assertEquals("Keith", r.get(2).getName());
    }

    public void testBigBatch() throws Exception
    {
        Handle h = openHandle();
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        int count = 1000;
        b.setInteger("id", 0);
        b.setString("name", "First Name");
        for (int i = 1; i < count; ++i)
        {
            b.addAnother();
            b.setInteger("id", i);
            b.setString("name", "A Name");
        }
        b.execute();

        int row_count = h.createQuery("select count(id) from something").map(new ResultSetMapper<Integer>()
        {
            public Integer map(int index, ResultSet r) throws SQLException
            {
                return r.getInt(1);
            }
        }).first();

        assertEquals(count, row_count);
    }
}
