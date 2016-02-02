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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Maps;

import org.jdbi.HandyMapThing;
import org.jdbi.v3.exceptions.NoResultsException;
import org.jdbi.v3.exceptions.StatementException;
import org.jdbi.v3.exceptions.UnableToExecuteStatementException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestQueries
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();
    private Handle h;

    @Before
    public void setUp() throws Exception
    {
        h = db.openHandle();
    }

    @After
    public void doTearDown() throws Exception
    {
        if (h != null) h.close();
    }

    @Test
    public void testCreateQueryObject() throws Exception
    {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id").list();
        assertEquals(2, results.size());
        Map<String, Object> first_row = results.get(0);
        assertEquals("eric", first_row.get("name"));
    }

    @Test
    public void testMappedQueryObject() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Query<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        List<Something> r = query.list();
        Something eric = r.get(0);
        assertEquals("eric", eric.getName());
        assertEquals(1, eric.getId());
    }

    @Test
    public void testMappedQueryObjectWithNulls() throws Exception
    {
        h.insert("insert into something (id, name, integerValue) values (1, 'eric', null)");

        Query<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        List<Something> r = query.list();
        Something eric = r.get(0);
        assertEquals("eric", eric.getName());
        assertEquals(1, eric.getId());
        assertNull(eric.getIntegerValue());
    }

    @Test
    public void testMappedQueryObjectWithNullForPrimitiveIntField() throws Exception
    {
        h.insert("insert into something (id, name, intValue) values (1, 'eric', null)");

        Query<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        List<Something> r = query.list();
        Something eric = r.get(0);
        assertEquals("eric", eric.getName());
        assertEquals(1, eric.getId());
        assertEquals(0, eric.getIntValue());
    }

    @Test
    public void testMapper() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Query<String> query = h.createQuery("select name from something order by id").map((index, r, ctx) -> r.getString(1));

        String name = query.list().get(0);
        assertEquals("eric", name);
    }

    @Test
    public void testConvenienceMethod() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        List<Map<String, Object>> r = h.select("select * from something order by id");
        assertEquals(2, r.size());
        assertEquals("eric", r.get(0).get("name"));
    }

    @Test
    public void testConvenienceMethodWithParam() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        List<Map<String, Object>> r = h.select("select * from something where id = ?", 1);
        assertEquals(1, r.size());
        assertEquals("eric", r.get(0).get("name"));
    }

    @Test
    public void testPositionalArgWithNamedParam() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        List<Something> r = h.createQuery("select * from something where name = :name")
                             .bind(0, "eric")
                             .mapToBean(Something.class)
                             .list();

        assertEquals(1, r.size());
        assertEquals("eric", r.get(0).getName());
    }

    @Test
    public void testMixedSetting() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        List<Something> r = h.createQuery("select * from something where name = :name and id = :id")
                             .bind(0, "eric")
                             .bind("id", 1)
                             .mapToBean(Something.class)
                             .list();

        assertEquals(1, r.size());
        assertEquals("eric", r.get(0).getName());
    }

    @Test
    public void testHelpfulErrorOnNothingSet() throws Exception
    {
        try {
            h.createQuery("select * from something where name = :name").list();
            fail("should have raised exception");
        }
        catch (UnableToExecuteStatementException e) {
            assertTrue("execution goes through here", true);
        }
        catch (Exception e) {
            fail("Raised incorrect exception");
        }
    }

    @Test
    public void testFirstResult() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Something r = h.createQuery("select * from something order by id")
                       .mapToBean(Something.class)
                       .findFirst()
                       .get();

        assertEquals("eric", r.getName());
    }

    @Test
    public void testIteratedResult() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        try (ResultIterator<Something> i = h.createQuery("select * from something order by id")
                                       .mapToBean(Something.class)
                                       .iterator()) {
            assertTrue(i.hasNext());
            Something first = i.next();
            assertEquals("eric", first.getName());
            assertTrue(i.hasNext());
            Something second = i.next();
            assertEquals(2, second.getId());
            assertFalse(i.hasNext());
        }
    }

    @Test
    public void testIteratorBehavior() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        try (ResultIterator<Something> i = h.createQuery("select * from something order by id")
                                       .mapToBean(Something.class)
                                       .iterator()) {
            assertTrue(i.hasNext());
            assertTrue(i.hasNext());
            Something first = i.next();
            assertEquals("eric", first.getName());
            assertTrue(i.hasNext());
            Something second = i.next();
            assertEquals(2, second.getId());
            assertFalse(i.hasNext());
        }
    }

    @Test
    public void testIteratorBehavior2() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        try (ResultIterator<Something> i = h.createQuery("select * from something order by id")
                                       .mapToBean(Something.class)
                                       .iterator()) {

            Something first = i.next();
            assertEquals("eric", first.getName());
            Something second = i.next();
            assertEquals(2, second.getId());
            assertFalse(i.hasNext());
        }
    }

    @Test
    public void testIteratorBehavior3() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'eric')");

        int count = 0;
        for (Something s : h.createQuery("select * from something order by id").mapToBean(Something.class)) {
            count++;
            assertEquals("eric", s.getName());
        }

        assertEquals(2, count);

    }

    @Test
    public void testFetchSize() throws Exception
    {
        h.createScript("default-data").execute();

        Query<Something> q = h.createQuery("select id, name from something order by id").mapToBean(Something.class);

        q.setFetchSize(1);

        ResultIterator<Something> r = q.iterator();

        assertTrue(r.hasNext());
        r.next();
        assertTrue(r.hasNext());
        r.next();
        assertFalse(r.hasNext());
    }

    @Test
    public void testFirstWithNoResult() throws Exception
    {
        Optional<Something> s = h.createQuery("select id, name from something").mapToBean(Something.class).findFirst();
        assertFalse(s.isPresent());
    }

    @Test
    public void testListWithMaxRows() throws Exception
    {
        h.prepareBatch("insert into something (id, name) values (:id, :name)")
         .add(1, "Brian")
         .add(2, "Keith")
         .add(3, "Eric")
         .execute();

        try (Stream<Something> stream = h.createQuery("select id, name from something")
                .mapToBean(Something.class)
                .stream()) {
            assertEquals(1, stream.limit(1).collect(Collectors.toList()).size());
        }

        try (Stream<Something> stream = h.createQuery("select id, name from something")
                .mapToBean(Something.class)
                .stream()) {
            assertEquals(2, stream.limit(2).collect(Collectors.toList()).size());
        }
    }

    @Test
    public void testFold() throws Exception
    {
        h.prepareBatch("insert into something (id, name) values (:id, :name)")
         .add(1, "Brian")
         .add(2, "Keith")
         .execute();

        try (Stream<Entry<String, Integer>> stream = h.createQuery("select id, name from something")
                .<Entry<String, Integer>>map((i, r, ctx) -> Maps.immutableEntry(r.getString("name"), r.getInt("id")))
                .stream()) {
            Map<String, Integer> rs = stream
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            assertEquals(2, rs.size());
            assertEquals(Integer.valueOf(1), rs.get("Brian"));
            assertEquals(Integer.valueOf(2), rs.get("Keith"));
        }
    }

    @Test
    public void testCollectList() throws Exception
    {
        h.prepareBatch("insert into something (id, name) values (:id, :name)")
         .add(1, "Brian")
         .add(2, "Keith")
         .execute();

        try (Stream<String> stream = h.createQuery("select name from something order by id")
                .mapTo(String.class)
                .stream()) {
            List<String> rs = stream
                    .collect(Collectors.toList());
            assertEquals(2, rs.size());
            assertEquals(Arrays.asList("Brian", "Keith"), rs);
        }
    }

    @Test
    public void testUsefulArgumentOutputForDebug() throws Exception
    {
        try {
            h.createStatement("insert into something (id, name) values (:id, :name)")
             .bind("name", "brian")
             .bind(7, 8)
             .bindFromMap(new HandyMapThing<String>().add("one", "two"))
             .bindFromProperties(new Object())
             .execute();
        }
        catch (StatementException e) {
            assertTrue(e.getMessage()
                        .contains("arguments:{ positional:{7:8}, named:{name:brian}, finder:[{one=two},{lazy bean proprty arguments \"java.lang.Object"));
        }
    }

    @Test
    public void testStatementCustomizersPersistAfterMap() throws Exception
    {
        h.insert("insert into something (id, name) values (?, ?)", 1, "hello");
        h.insert("insert into something (id, name) values (?, ?)", 2, "world");

        List<Something> rs = h.createQuery("select id, name from something")
                              .setMaxRows(1)
                              .mapToBean(Something.class)
                              .list();

        assertEquals(1, rs.size());
    }

    @Test
    public void testQueriesWithNullResultSets() throws Exception
    {
        try {
            h.select("insert into something (id, name) values (?, ?)", 1, "hello");
        }
        catch (NoResultsException e) {
            return;
        }
        fail("expected NoResultsException");
    }
}
