/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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

import org.jdbi.derby.Tools;
import org.jdbi.v3.BasicHandle;
import org.jdbi.v3.ResultIterator;

import java.util.List;
import java.util.Map;

public class TestClosingHandle extends DBITestCase
{
    private BasicHandle h;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        h = openHandle();
    }

    @Override
    public void tearDown() throws Exception {
        if (h != null) h.close();
        Tools.stop();
    }

    public void testNotClosing() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id").list();
        assertEquals(2, results.size());
        Map<String, Object> first_row = results.get(0);
        assertEquals("eric", first_row.get("name"));
        assertFalse(h.isClosed());
    }

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

