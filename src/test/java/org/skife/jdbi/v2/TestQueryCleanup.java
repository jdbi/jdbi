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

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.tweak.HandleCallback;

import com.google.common.collect.Iterators;

public class TestQueryCleanup
{
    private static final int COUNT = 100;

    private DBI dbi;

    @Before
    public void setUp()
        throws Exception
    {
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1", UUID.randomUUID()));
        dbi = new DBI(ds);

        dbi.withHandle(new HandleCallback<Void>() {
            @Override
            public Void withHandle(final Handle handle)
            {
                handle.execute("create table something (id int primary key, name varchar(100))");

                for (int i = 0; i < COUNT; i++) {
                    handle.createStatement("insert into something (id, name) values (:id, :name)")
                        .bind("id", i)
                        .bind("name", UUID.randomUUID().toString())
                        .execute();
                }

                return null;
            }
        });
    }

    @Test
    public void testBasicCleanupHandle()
        throws Exception
    {
        final Handle handle = dbi.open();
        final Query<Integer> q = handle.createQuery("SELECT COUNT(1) FROM something").mapTo(Integer.class);
        final ResultIterator<Integer> it = q.iterator();
        assertEquals(COUNT, Iterators.getOnlyElement(it).intValue());
        assertFalse(it.hasNext());
        assertFalse(handle.getConnection().isClosed());
        handle.close();
        assertTrue(handle.getConnection().isClosed());
    }

    @Test
    public void testBasicCleanupIterator()
        throws Exception
    {
        final Handle handle = dbi.open();
        final Query<Integer> q = handle.createQuery("SELECT COUNT(1) FROM something")
            .cleanupHandle()
            .mapTo(Integer.class);
        final ResultIterator<Integer> it = q.iterator();
        assertEquals(COUNT, Iterators.getOnlyElement(it).intValue());
        assertFalse(it.hasNext());
        assertTrue(handle.getConnection().isClosed());
    }

    @Test
    public void testBasicCleanupHalfwayHandle1()
        throws Exception
    {
        final Handle handle = dbi.open();
        final Query<Integer> q = handle.createQuery("SELECT id FROM something").mapTo(Integer.class);
        final ResultIterator<Integer> it = q.iterator();

        for (int i = 0; i < COUNT / 2; i++) {
            assertTrue(it.hasNext());
            it.next();
        }
        assertTrue(it.hasNext());
        assertFalse(handle.getConnection().isClosed());
        handle.close();
        assertTrue(handle.getConnection().isClosed());
    }

    @Test
    public void testBasicCleanupHalfwayHandle2()
        throws Exception
    {
        final Handle handle = dbi.open();
        final Query<Integer> q = handle.createQuery("SELECT id FROM something")
            .cleanupHandle()
            .mapTo(Integer.class);
        final ResultIterator<Integer> it = q.iterator();

        for (int i = 0; i < COUNT / 2; i++) {
            assertTrue(it.hasNext());
            it.next();
        }
        assertTrue(it.hasNext());
        assertFalse(handle.getConnection().isClosed());
        handle.close();
        assertTrue(handle.getConnection().isClosed());
    }

    @Test
    public void testBasicCleanupHalfwayIterator()
        throws Exception
    {
        final Handle handle = dbi.open();
        final Query<Integer> q = handle.createQuery("SELECT id FROM something")
            .cleanupHandle()
            .mapTo(Integer.class);
        final ResultIterator<Integer> it = q.iterator();

        for (int i = 0; i < COUNT / 2; i++) {
            assertTrue(it.hasNext());
            it.next();
        }
        assertTrue(it.hasNext());
        assertFalse(handle.getConnection().isClosed());
        it.close();
        assertTrue(handle.getConnection().isClosed());
    }

    @Test
    public void testDoubleCleanup()
        throws Exception
    {
        final Handle handle = dbi.open();
        final Query<Integer> q = handle
            .createQuery("SELECT id FROM something")
            .cleanupHandle()
            .cleanupHandle()
            .mapTo(Integer.class);
        final ResultIterator<Integer> it = q.iterator();

        while (it.hasNext()) {
            it.next();
        }
        assertFalse(it.hasNext());
        assertTrue(handle.getConnection().isClosed());
    }

    @Test
    public void testDoubleCleanupHalfwayHandle()
        throws Exception
    {
        final Handle handle = dbi.open();
        final Query<Integer> q = handle
            .createQuery("SELECT id FROM something")
            .cleanupHandle()
            .cleanupHandle()
            .mapTo(Integer.class);
        final ResultIterator<Integer> it = q.iterator();

        for (int i = 0; i < COUNT / 2; i++) {
            assertTrue(it.hasNext());
            it.next();
        }
        assertTrue(it.hasNext());
        assertFalse(handle.getConnection().isClosed());
        handle.close();
        assertTrue(handle.getConnection().isClosed());
    }

    @Test
    public void testDoubleCleanupHalfwayIterator()
        throws Exception
    {
        final Handle handle = dbi.open();
        final Query<Integer> q = handle
            .createQuery("SELECT id FROM something")
            .cleanupHandle()
            .cleanupHandle()
            .mapTo(Integer.class);
        final ResultIterator<Integer> it = q.iterator();

        for (int i = 0; i < COUNT / 2; i++) {
            assertTrue(it.hasNext());
            it.next();
        }
        assertTrue(it.hasNext());
        assertFalse(handle.getConnection().isClosed());
        it.close();
        assertTrue(handle.getConnection().isClosed());
    }
}
