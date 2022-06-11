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
package org.jdbi.v3.core.statement;

import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import com.google.common.collect.Maps;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.result.NoResultsException;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.jdbi.v3.core.locator.ClasspathSqlLocator.findSqlOnClasspath;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestQueries {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testCreateQueryObject() {
        Handle h = h2Extension.openHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id").mapToMap().list();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testMappedQueryObject() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        ResultIterable<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        List<Something> r = query.list();
        assertThat(r).startsWith(new Something(1, "eric"));
    }

    @Test
    public void testMappedQueryObjectWithNulls() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name, integerValue) values (1, 'eric', null)");

        ResultIterable<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        List<Something> r = query.list();
        Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntegerValue()).isNull();
    }

    @Test
    public void testMappedQueryObjectWithNullForPrimitiveIntField() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name, intValue) values (1, 'eric', null)");

        ResultIterable<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        List<Something> r = query.list();
        Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntValue()).isZero();
    }

    @Test
    public void testMapper() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        ResultIterable<String> query = h.createQuery("select name from something order by id").map((r, ctx) -> r.getString(1));

        List<String> r = query.list();
        assertThat(r).startsWith("eric");
    }

    @Test
    public void testConvenienceMethod() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        List<Map<String, Object>> r = h.select("select * from something order by id").mapToMap().list();
        assertThat(r).hasSize(2);
        assertThat(r.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testConvenienceMethodWithParam() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        List<Map<String, Object>> r = h.select("select * from something where id = ?", 1).mapToMap().list();
        assertThat(r).hasSize(1);
        assertThat(r.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testPositionalArgWithNamedParam() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThatThrownBy(() ->
            h.createQuery("select * from something where name = :name")
                .bind(0, "eric")
                .mapToBean(Something.class)
                .list())
            .isInstanceOf(UnableToCreateStatementException.class)
            .hasMessageContaining("Missing named parameter 'name'");
    }

    @Test
    public void testMixedSetting() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThatThrownBy(() ->
            h.createQuery("select * from something where name = :name and id = :id")
                .bind(0, "eric")
                .bind("id", 1)
                .mapToBean(Something.class)
                .list())
            .isInstanceOf(UnableToCreateStatementException.class)
            .hasMessageContaining("Missing named parameter 'name'");
    }

    @Test
    public void testHelpfulErrorOnNothingSet() {
        Handle h = h2Extension.openHandle();

        assertThatThrownBy(() -> h.createQuery("select * from something where name = :name").mapToMap().list())
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testFirstResult() {
        Handle h = h2Extension.openHandle();

        Query query = h.createQuery("select name from something order by id");

        assertThatThrownBy(() -> query.mapTo(String.class).first()).isInstanceOf(IllegalStateException.class);
        assertThat(query.mapTo(String.class).findFirst()).isEmpty();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThat(query.mapTo(String.class).first()).isEqualTo("eric");
        assertThat(query.mapTo(String.class).findFirst()).contains("eric");
    }

    @Test
    public void testFirstResultNull() {
        Handle h = h2Extension.openHandle();

        Query query = h.createQuery("select name from something order by id");

        assertThatThrownBy(() -> query.mapTo(String.class).first()).isInstanceOf(IllegalStateException.class);
        assertThat(query.mapTo(String.class).findFirst()).isEmpty();

        h.execute("insert into something (id, name) values (1, null)");
        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThat(query.mapTo(String.class).first()).isNull();
        assertThat(query.mapTo(String.class).findFirst()).isEmpty();
    }

    @Test
    public void testOneResult() {
        Handle h = h2Extension.openHandle();

        Query query = h.createQuery("select name from something order by id");

        assertThatThrownBy(() -> query.mapTo(String.class).one()).isInstanceOf(IllegalStateException.class);
        assertThat(query.mapTo(String.class).findOne()).isEmpty();

        h.execute("insert into something (id, name) values (1, 'eric')");

        assertThat(query.mapTo(String.class).one()).isEqualTo("eric");
        assertThat(query.mapTo(String.class).findOne()).contains("eric");

        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThatThrownBy(() -> query.mapTo(String.class).one()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> query.mapTo(String.class).findOne()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testOneResultNull() {
        Handle h = h2Extension.openHandle();

        Query query = h.createQuery("select name from something order by id");

        assertThatThrownBy(() -> query.mapTo(String.class).one()).isInstanceOf(IllegalStateException.class);
        assertThat(query.mapTo(String.class).findOne()).isEmpty();

        h.execute("insert into something (id, name) values (1, null)");

        assertThat(query.mapTo(String.class).one()).isNull();
        assertThat(query.mapTo(String.class).findOne()).isEmpty();

        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThatThrownBy(() -> query.mapTo(String.class).one()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> query.mapTo(String.class).findOne()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testIteratedResult() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

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
    public void testIteratorBehavior() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

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
    public void testIteratorBehavior2() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

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
    public void testIteratorBehavior3() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'eric')");

        assertThat(h.createQuery("select * from something order by id").mapToBean(Something.class))
            .extracting(Something::getName)
            .containsExactly("eric", "eric");

    }

    @Test
    public void testFetchSize() {
        Handle h = h2Extension.openHandle();

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
    public void testFirstWithNoResult() {
        Handle h = h2Extension.openHandle();

        Optional<Something> s = h.createQuery("select id, name from something").mapToBean(Something.class).findFirst();
        assertThat(s).isNotPresent();
    }

    @Test
    public void testNullValueInColumn() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (?, ?)", 1, null);
        String s = h.createQuery("select name from something where id=1").mapTo(String.class).first();
        assertThat(s).isNull();
    }

    @Test
    public void testListWithMaxRows() {
        Handle h = h2Extension.openHandle();

        h.prepareBatch("insert into something (id, name) values (?, ?)")
            .add(1, "Brian")
            .add(2, "Keith")
            .add(3, "Eric")
            .execute();

        assertThat(h.createQuery("select id, name from something")
            .mapToBean(Something.class)
            .withStream(stream -> stream.limit(1).count())
            .longValue()).isOne();

        assertThat(h.createQuery("select id, name from something")
            .mapToBean(Something.class)
            .withStream(stream -> stream.limit(2).count())
            .longValue()).isEqualTo(2);
    }

    @Test
    public void testFold() {
        Handle h = h2Extension.openHandle();

        h.prepareBatch("insert into something (id, name) values (?, ?)")
            .add(1, "Brian")
            .add(2, "Keith")
            .execute();

        Map<String, Integer> rs = h.createQuery("select id, name from something")
            .<Entry<String, Integer>>map((r, ctx) -> Maps.immutableEntry(r.getString("name"), r.getInt("id")))
            .collect(toMap(Entry::getKey, Entry::getValue));

        assertThat(rs).containsOnly(entry("Brian", 1), entry("Keith", 2));
    }

    @Test
    public void testCollectList() {
        Handle h = h2Extension.openHandle();

        h.prepareBatch("insert into something (id, name) values (?, ?)")
            .add(1, "Brian")
            .add(2, "Keith")
            .execute();

        List<String> rs = h.createQuery("select name from something order by id")
            .mapTo(String.class)
            .collect(toList());
        assertThat(rs).containsExactly("Brian", "Keith");
    }

    @Test
    public void testUsefulArgumentOutputForDebug() {
        Handle h = h2Extension.openHandle();

        assertThatThrownBy(() -> h.createUpdate("insert into something (id, name) values (:id, :name)")
            .bind("name", "brian")
            .bind(7, 8)
            .bindMap(new HandyMapThing<String>().add("one", "two"))
            .bindBean(new Object())
            .execute())
            .isInstanceOf(StatementException.class)
            .hasMessageContaining("binding:{positional:{7:8}, named:{one:two,name:brian}, finder:[{lazy bean property arguments \"java.lang.Object");
    }

    @Test
    public void testStatementCustomizersPersistAfterMap() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (?, ?)", 1, "hello");
        h.execute("insert into something (id, name) values (?, ?)", 2, "world");

        List<Something> rs = h.createQuery("select id, name from something")
            .setMaxRows(1)
            .mapToBean(Something.class)
            .list();

        assertThat(rs).hasSize(1);
    }

    @Test
    public void testQueriesWithNullResultSets() {
        Handle h = h2Extension.openHandle();

        assertThatThrownBy(() -> h.select("insert into something (id, name) values (?, ?)", 1, "hello").mapToMap().list())
            .isInstanceOf(NoResultsException.class);
    }

    @Test
    public void testMapMapperOrdering() {
        Handle h = h2Extension.openHandle();

        h.execute("insert into something (id, name) values (?, ?)", 1, "hello");
        h.execute("insert into something (id, name) values (?, ?)", 2, "world");

        List<Map<String, Object>> rs = h.createQuery("select id, name from something")
            .mapToMap()
            .list();

        assertThat(rs).hasSize(2);
        assertThat(rs).hasOnlyElementsOfType(LinkedHashMap.class);
    }

    @Test
    public void testBindPrimitiveWithExplicitNull() {
        Handle h = h2Extension.openHandle();
        assertThat(h.createQuery("select :bool")
                .bindByType("bool", new NullArgument(Types.BOOLEAN), boolean.class)
                .mapTo(Boolean.class)
                .one())
            .isNull();
    }

    @Test
    public void testForEach() {
        Handle h = h2Extension.openHandle();
        int nbRecs = 42;
        for (int id = 1; id <= nbRecs; id++) {
            h.execute("insert into something (id, name) values (?, ?)", id, null);
        }

        ResultIterable<Integer> ri = h.createQuery("select id from something").mapTo(Integer.class);

        assertThatThrownBy(() -> ri.forEach(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("Action required");

        AtomicInteger sum = new AtomicInteger(0);
        int count = ri.forEachWithCount(sum::addAndGet);
        assertThat(count).isEqualTo(nbRecs);
        assertThat(sum.intValue()).isEqualTo(903);
    }

    @Test
    public void testFilter() {
        Handle h = h2Extension.openHandle();

        int id = 0;
        for (String name : new String[] {null, "john", "lennon", "would have liked", "java"}) {
            h.execute("insert into something (id, name) values (?, ?)", ++id, name);
        }

        ResultIterable<String> ri1 = h.createQuery("select name from something").mapTo(String.class);

        assertThatThrownBy(() -> ri1.filter(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("Filter required");

        Predicate<String> startsWithJPredicate = spy(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s != null && s.startsWith("j");
            }
        });
        Predicate<String> containsHPredicate = spy(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.contains("h");
            }
        });

        ResultIterable<String> ri2 = ri1
                .filter(startsWithJPredicate)
                .filter(containsHPredicate);

        // as iteration has not yet taken place, test filters were not invoked
        verify(startsWithJPredicate, never()).test(any());
        verify(containsHPredicate, never()).test(any());

        assertThat(ri1).isNotSameAs(ri2);

        ResultIterator<String> iter = ri2.iterator();
        // mess with the resultset by calling hasNext repeatedly
        IntStream.range(0, 5).forEach(i -> assertTrue(iter.hasNext()));

        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        // the first filter should have seen all records
        verify(startsWithJPredicate, times(5)).test(nullable(String.class));
        // the second filter only two records that matched the first filter
        verify(containsHPredicate, times(2)).test(anyString());

        assertThatThrownBy(iter::next).isInstanceOf(NoSuchElementException.class)
                .hasMessage("No more filtered results");

        assertThat(results).containsExactly("john");
    }

}
