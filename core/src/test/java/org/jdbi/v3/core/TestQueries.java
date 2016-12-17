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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.locator.ClasspathSqlLocator.findSqlOnClasspath;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.Maps;

import org.jdbi.v3.core.exception.NoResultsException;
import org.jdbi.v3.core.exception.StatementException;
import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestQueries
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id").mapToMap().list();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testMappedQueryObject() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        ResultIterable<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        List<Something> r = query.list();
        assertThat(r.get(0)).isEqualTo(new Something(1, "eric"));
    }

    @Test
    public void testMappedQueryObjectWithNulls() throws Exception
    {
        h.insert("insert into something (id, name, integerValue) values (1, 'eric', null)");

        ResultIterable<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        List<Something> r = query.list();
        Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntegerValue()).isNull();
    }

    @Test
    public void testMappedQueryObjectWithNullForPrimitiveIntField() throws Exception
    {
        h.insert("insert into something (id, name, intValue) values (1, 'eric', null)");

        ResultIterable<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        List<Something> r = query.list();
        Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntValue()).isZero();
    }

    @Test
    public void testMapper() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        ResultIterable<String> query = h.createQuery("select name from something order by id").map((r, ctx) -> r.getString(1));

        String name = query.list().get(0);
        assertThat(name).isEqualTo("eric");
    }

    @Test
    public void testConvenienceMethod() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        List<Map<String, Object>> r = h.select("select * from something order by id").mapToMap().list();
        assertThat(r).hasSize(2);
        assertThat(r.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testConvenienceMethodWithParam() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        List<Map<String, Object>> r = h.select("select * from something where id = ?", 1).mapToMap().list();
        assertThat(r).hasSize(1);
        assertThat(r.get(0).get("name")).isEqualTo("eric");
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

        assertThat(r).extracting(Something::getName).containsExactly("eric");
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

        assertThat(r).extracting(Something::getName).containsExactly("eric");
    }

    @Test(expected = UnableToExecuteStatementException.class)
    public void testHelpfulErrorOnNothingSet() throws Exception
    {
        h.createQuery("select * from something where name = :name").mapToMap().list();
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

        assertThat(r.getName()).isEqualTo("eric");
    }

    @Test
    public void testIteratedResult() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        try (ResultIterator<Something> i = h.createQuery("select * from something order by id")
                                       .mapToBean(Something.class)
                                       .iterator()) {
            assertThat(i.hasNext()).isTrue();
            Something first = i.next();
            assertThat(first.getName()).isEqualTo("eric");
            assertThat(i.hasNext()).isTrue();
            Something second = i.next();
            assertThat(second.getId()).isEqualTo(2);
            assertThat(i.hasNext()).isFalse();
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
            assertThat(i.hasNext()).isTrue();
            Something first = i.next();
            assertThat(first.getName()).isEqualTo("eric");
            assertThat(i.hasNext()).isTrue();
            Something second = i.next();
            assertThat(second.getId()).isEqualTo(2);
            assertThat(i.hasNext()).isFalse();
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
            assertThat(first.getName()).isEqualTo("eric");
            Something second = i.next();
            assertThat(second.getId()).isEqualTo(2);
            assertThat(i.hasNext()).isFalse();
        }
    }

    @Test
    public void testIteratorBehavior3() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'eric')");

        assertThat(h.createQuery("select * from something order by id").mapToBean(Something.class))
                .extracting(Something::getName)
                .containsExactly("eric", "eric");

    }

    @Test
    public void testFetchSize() throws Exception
    {
        h.createScript(findSqlOnClasspath("default-data")).execute();

        ResultIterable<Something> ri = h.createQuery("select id, name from something order by id")
                .setFetchSize(1)
                .mapToBean(Something.class);

        ResultIterator<Something> r = ri.iterator();

        assertThat(r.hasNext()).isTrue();
        r.next();
        assertThat(r.hasNext()).isTrue();
        r.next();
        assertThat(r.hasNext()).isFalse();
    }

    @Test
    public void testFirstWithNoResult() throws Exception
    {
        Optional<Something> s = h.createQuery("select id, name from something").mapToBean(Something.class).findFirst();
        assertThat(s.isPresent()).isFalse();
    }

    @Test
    public void testNullValueInColumn() throws Exception
    {
        h.insert("insert into something (id, name) values (?, ?)", 1, null);
        Optional<String> s = h.createQuery("select name from something where id=1").mapTo(String.class).findFirst();
        assertThat(s.isPresent()).isFalse();
    }

    @Test
    public void testListWithMaxRows() throws Exception
    {
        h.prepareBatch("insert into something (id, name) values (:id, :name)")
         .add(1, "Brian")
         .add(2, "Keith")
         .add(3, "Eric")
         .execute();


        assertThat(h.createQuery("select id, name from something")
                .mapToBean(Something.class)
                .withStream(stream -> stream.limit(1).count())
                .longValue()).isEqualTo(1);

        assertThat(h.createQuery("select id, name from something")
                .mapToBean(Something.class)
                .withStream(stream -> stream.limit(2).count())
                .longValue()).isEqualTo(2);
    }

    @Test
    public void testFold() throws Exception
    {
        h.prepareBatch("insert into something (id, name) values (:id, :name)")
         .add(1, "Brian")
         .add(2, "Keith")
         .execute();

        Map<String, Integer> rs = h.createQuery("select id, name from something")
                .<Entry<String, Integer>>map((r, ctx) -> Maps.immutableEntry(r.getString("name"), r.getInt("id")))
                .collect(toMap(Entry::getKey, Entry::getValue));

        assertThat(rs).containsOnly(entry("Brian", 1), entry("Keith", 2));
    }

    @Test
    public void testCollectList() throws Exception
    {
        h.prepareBatch("insert into something (id, name) values (:id, :name)")
         .add(1, "Brian")
         .add(2, "Keith")
         .execute();

        List<String> rs = h.createQuery("select name from something order by id")
                .mapTo(String.class)
                .collect(toList());
        assertThat(rs).containsExactly("Brian", "Keith");
    }

    @Test
    public void testUsefulArgumentOutputForDebug() throws Exception
    {
        expectedException.expect(StatementException.class);
        expectedException.expectMessage("arguments:{ positional:{7:8}, named:{name:brian}, finder:[{one=two},{lazy bean property arguments \"java.lang.Object");

        h.createUpdate("insert into something (id, name) values (:id, :name)")
                .bind("name", "brian")
                .bind(7, 8)
                .bindMap(new HandyMapThing<String>().add("one", "two"))
                .bindBean(new Object())
                .execute();
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

        assertThat(rs).hasSize(1);
    }

    @Test
    public void testQueriesWithNullResultSets() throws Exception
    {
        expectedException.expect(NoResultsException.class);

        h.select("insert into something (id, name) values (?, ?)", 1, "hello").mapToMap().list();
    }
}
