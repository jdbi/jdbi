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
package org.jdbi.v3.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestClosingHandle
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle h;

    @Before
    public void setUp() throws Exception {
        h = db.openHandle();
    }

    @After
    public void doTearDown() throws Exception {
        if (h != null) h.close();
    }

    @Test
    public void testNotClosing() throws Exception {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id").list();
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).containsEntry("name", "eric");
        assertThat(h.isClosed()).isFalse();
    }

    @Test
    public void testClosing() throws Exception {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .list();

        assertThat(results).hasSize(2);
        assertThat(results.get(0)).containsEntry("name", "eric");
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testIterateKeepHandle() throws Exception {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .iterator();

        assertThat(it).hasSize(2);
        assertThat(h.isClosed()).isFalse();
    }

    @Test
    public void testIterateAllTheWay() throws Exception {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertThat(it).hasSize(2);
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testIteratorBehaviour() throws Exception {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();
        it.next();
        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();
        it.next();
        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();
        it.next();
        assertThat(it.hasNext()).isFalse();
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testIteratorClose() throws Exception {
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
        h.createUpdate("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();
        it.next();
        assertThat(it.hasNext()).isTrue();
        assertThat(h.isClosed()).isFalse();

        it.close();
        assertThat(it.hasNext()).isFalse();
        assertThat(h.isClosed()).isTrue();
    }
}

