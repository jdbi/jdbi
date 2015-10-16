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
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestIterator
{
    @Rule
    public MemoryDatabase db = new MemoryDatabase();

    private Handle h;

    @Before
    public void setUp() throws Exception {
        h = db.openHandle();
    }

    @After
    public void doTearDown() throws Exception {
        assertTrue("Handle was not closed correctly!", ((BasicHandle)h).isClosed());
    }

    @Test
    public void testSimple() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    public void testEmptyWorksToo() throws Exception {
        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertFalse(it.hasNext());
    }

    @Test
    public void testHasNext() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
    }

    @Test
    public void testNext() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertTrue(it.hasNext());
        it.next();
        it.next();
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    public void testJustNext() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        it.next();
        it.next();
        it.next();
    }

    @Test
    public void testTwoTwo() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        it.next();
        it.next();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
    }

    @Test
    public void testTwoOne() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        assertTrue(it.hasNext());
        it.next();
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    public void testExplodeIterator() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();
        h.createStatement("insert into something (id, name) values (3, 'john')").execute();

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        try {
            assertTrue(it.hasNext());
            it.next();
            assertTrue(it.hasNext());
            it.next();
            assertTrue(it.hasNext());
            it.next();
            assertFalse(it.hasNext());
        }
        catch (Throwable t) {
            fail("unexpected throwable:" + t.getMessage());
        }

        try {
            it.next();
            fail("Expected IllegalStateException did not show up!");
        }
        catch (IllegalStateException iae) {
            // TestCase does not deal with the annotations...
        }
    }

    @Test
    public void testEmptyExplosion() throws Exception {

        ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator();

        try {
            it.next();
            fail("Expected NoSuchElementException did not show up!");
        }
        catch (NoSuchElementException nsee) {
            // TestCase does not deal with the annotations...
        }
    }

    @Test
    public void testNonPathologicalJustNext() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();

        // Yes, you *should* use first(). But sometimes, an iterator is passed 17 levels deep and then
        // used in this way (Hello Jackson!).
        final Map<String, Object> result = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator()
            .next();

        assertEquals(1L, result.get("id"));
        assertEquals("eric", result.get("name"));
    }

    @Test
    public void testStillLeakingJustNext() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        // Yes, you *should* use first(). But sometimes, an iterator is passed 17 levels deep and then
        // used in this way (Hello Jackson!).
        final Map<String, Object> result = h.createQuery("select * from something order by id")
            .cleanupHandle()
            .iterator()
            .next();

        assertEquals(1L, result.get("id"));
        assertEquals("eric", result.get("name"));

        assertFalse(((BasicHandle) h).isClosed());

        // The Query created by createQuery() above just leaked a Statement and a ResultSet. It is necessary
        // to explicitly close the iterator in that case. However, as this test case is using the CachingStatementBuilder,
        // closing the handle will close the statements (which also closes the result sets).
        //
        // Don't try this at home folks. It is still very possible to leak stuff with the iterators.

        h.close();
    }

    @Test
    public void testLessLeakingJustNext() throws Exception {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        try (final ResultIterator<Map<String, Object>> it = h.createQuery("select * from something order by id")
                .cleanupHandle()
                .iterator()) {
            final Map<String, Object> result =  it.next();

            assertEquals(1L, result.get("id"));
            assertEquals("eric", result.get("name"));

            assertFalse(((BasicHandle) h).isClosed());
        }
    }
}
