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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestClosingHandle
{
    @Rule
    public MemoryDatabase db = new MemoryDatabase();

    private BasicHandle h;

    @Before
    public void setUp() throws Exception {
        h = (BasicHandle) db.openHandle();
    }

    @After
    public void doTearDown() throws Exception {
        if (h != null) h.close();
    }

    @Test
    public void testNotClosing() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id").list();
        assertEquals(2, results.size());
        Map<String, Object> first_row = results.get(0);
        assertEquals("eric", first_row.get("name"));
        assertFalse(h.isClosed());
    }

    @Test
    public void testClosing() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .list();

        assertEquals(2, results.size());
        Map<String, Object> first_row = results.get(0);
        assertEquals("eric", first_row.get("name"));
        assertTrue(h.isClosed());
    }

    @Test
    public void testIterateKeepHandle() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .iterator();

        int cnt = 0;
        while(it.hasNext()) {
            cnt++;
            it.next();
        }

        assertEquals(2, cnt);
        assertFalse(h.isClosed());
    }

    @Test
    public void testIterateAllTheWay() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        int cnt = 0;
        while(it.hasNext()) {
            cnt++;
            it.next();
        }

        assertEquals(2, cnt);
        assertTrue(h.isClosed());
    }

    @Test
    public void testIteratorBehaviour() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertTrue(it.hasNext());
        assertFalse(h.isClosed());
        it.next();
        assertTrue(it.hasNext());
        assertFalse(h.isClosed());
        it.next();
        assertTrue(it.hasNext());
        assertFalse(h.isClosed());
        it.next();
        assertFalse(it.hasNext());
        assertTrue(h.isClosed());
    }

    @Test
    public void testIteratorClose() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertTrue(it.hasNext());
        assertFalse(h.isClosed());
        it.next();
        assertTrue(it.hasNext());
        assertFalse(h.isClosed());

        it.close();
        assertFalse(it.hasNext());
        assertTrue(h.isClosed());
    }
}

