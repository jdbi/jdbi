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
package org.jdbi.v3;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class TestNamedParams
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testInsert() throws Exception
    {
        Handle h = db.openHandle();
        Update insert = h.createStatement("insert into something (id, name) values (:id, :name)");
        insert.bind("id", 1);
        insert.bind("name", "Brian");
        int count = insert.execute();
        assertEquals(1, count);
    }

    @Test
    public void testDemo() throws Exception
    {
        Handle h = db.getSharedHandle();
        h.createStatement("insert into something (id, name) values (:id, :name)")
                .bind("id", 1)
                .bind("name", "Brian")
                .execute();
        h.insert("insert into something (id, name) values (?, ?)", 2, "Eric");
        h.insert("insert into something (id, name) values (?, ?)", 3, "Erin");

        List<Something> r = h.createQuery("select id, name from something " +
                                          "where name like :name " +
                                          "order by id")
                .bind("name", "Eri%")
                .mapToBean(Something.class)
                .list();

        assertEquals(2, r.size());
        assertEquals(2, r.get(0).getId());
        assertEquals(3, r.get(1).getId());
    }

    @Test
    public void testBeanPropertyBinding() throws Exception
    {
        Handle h = db.openHandle();
        Update s = h.createStatement("insert into something (id, name) values (:id, :name)");
        s.bindFromProperties(new Something(0, "Keith"));
        int insert_count = s.execute();
        assertEquals(1, insert_count);
    }

    @Test
    public void testMapKeyBinding() throws Exception
    {
        Handle h = db.openHandle();
        Update s = h.createStatement("insert into something (id, name) values (:id, :name)");
        Map<String, Object> args = new HashMap<>();
        args.put("id", 0);
        args.put("name", "Keith");
        s.bindFromMap(args);
        int insert_count = s.execute();
        assertEquals(1, insert_count);
    }

    @Test
    public void testCascadedLazyArgs() throws Exception
    {
        Handle h = db.openHandle();
        Update s = h.createStatement("insert into something (id, name) values (:id, :name)");
        Map<String, Object> args = new HashMap<>();
        args.put("id", 0);
        s.bindFromMap(args);
        s.bindFromProperties(new Object()
        {
            @SuppressWarnings("unused")
            public String getName() { return "Keith"; }
        });
        int insert_count = s.execute();
        assertEquals(1, insert_count);
        Something something = h.createQuery("select id, name from something").mapToBean(Something.class).findOnly();
        assertEquals("Keith", something.getName());
        assertEquals(0, something.getId());
    }
}
