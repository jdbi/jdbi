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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
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

public class TestQueries {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testCreateQueryObject() {
        final Handle h = h2Extension.getSharedHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        final List<Map<String, Object>> results = h.createQuery("select * from something order by id").mapToMap().list();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testMappedQueryObject() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final ResultIterable<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        final List<Something> r = query.list();
        assertThat(r).startsWith(new Something(1, "eric"));
    }

    @Test
    public void testMappedQueryObjectWithNulls() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name, integerValue) values (1, 'eric', null)");

        final ResultIterable<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        final List<Something> r = query.list();
        final Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntegerValue()).isNull();
    }

    @Test
    public void testMappedQueryObjectWithNullForPrimitiveIntField() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name, intValue) values (1, 'eric', null)");

        final ResultIterable<Something> query = h.createQuery("select * from something order by id").mapToBean(Something.class);

        final List<Something> r = query.list();
        final Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntValue()).isZero();
    }

    @Test
    public void testMapper() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final ResultIterable<String> query = h.createQuery("select name from something order by id").map((r, ctx) -> r.getString(1));

        final List<String> r = query.list();
        assertThat(r).startsWith("eric");
    }

    @Test
    public void testConvenienceMethod() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final List<Map<String, Object>> r = h.select("select * from something order by id").mapToMap().list();
        assertThat(r).hasSize(2);
        assertThat(r.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testConvenienceMethodWithParam() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final List<Map<String, Object>> r = h.select("select * from something where id = ?", 1).mapToMap().list();
        assertThat(r).hasSize(1);
        assertThat(r.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testPositionalArgWithNamedParam() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThatThrownBy(() -> {
            try (Query query = h.createQuery("select * from something where name = :name")) {
                query.bind(0, "eric")
                    .mapToBean(Something.class)
                    .list();
            }
        })
            .isInstanceOf(UnableToCreateStatementException.class)
            .hasMessageContaining("Missing named parameter 'name'");
    }

    @Test
    public void testMixedSetting() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThatThrownBy(() -> {
            try (Query query = h.createQuery("select * from something where name = :name and id = :id")) {
                query.bind(0, "eric")
                    .bind("id", 1)
                    .mapToBean(Something.class)
                    .list();
            }
        }).isInstanceOf(UnableToCreateStatementException.class)
            .hasMessageContaining("Missing named parameter 'name'");
    }

    @Test
    public void testHelpfulErrorOnNothingSet() {
        final Handle h = h2Extension.getSharedHandle();

        assertThatThrownBy(() -> {
            try (Query query = h.createQuery("select * from something where name = :name")) {
                query.mapToMap().list();
            }
        }).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testFirstResult() {
        final Handle h = h2Extension.getSharedHandle();

        final Query query = h.createQuery("select name from something order by id");

        assertThatThrownBy(() -> query.mapTo(String.class).first()).isInstanceOf(IllegalStateException.class);
        assertThat(query.mapTo(String.class).findFirst()).isEmpty();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThat(query.mapTo(String.class).first()).isEqualTo("eric");
        assertThat(query.mapTo(String.class).findFirst()).contains("eric");
    }

    @Test
    public void testFirstResultNull() {
        final Handle h = h2Extension.getSharedHandle();

        final Query query = h.createQuery("select name from something order by id");

        assertThatThrownBy(() -> query.mapTo(String.class).first()).isInstanceOf(IllegalStateException.class);
        assertThat(query.mapTo(String.class).findFirst()).isEmpty();

        h.execute("insert into something (id, name) values (1, null)");
        h.execute("insert into something (id, name) values (2, 'brian')");

        assertThat(query.mapTo(String.class).first()).isNull();
        assertThat(query.mapTo(String.class).findFirst()).isEmpty();
    }

    @Test
    public void testOneResult() {
        final Handle h = h2Extension.getSharedHandle();

        final Query query = h.createQuery("select name from something order by id");

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
        final Handle h = h2Extension.getSharedHandle();

        final Query query = h.createQuery("select name from something order by id");

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
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        try (ResultIterator<Something> i = h.createQuery("select * from something order by id")
            .mapToBean(Something.class)
            .iterator()) {
            assertThat(i).hasNext();
            final Something first = i.next();
            assertThat(first.getName()).isEqualTo("eric");
            assertThat(i).hasNext();
            final Something second = i.next();
            assertThat(second.getId()).isEqualTo(2);
            assertThat(i).isExhausted();
        }
    }

    @Test
    public void testIteratorBehavior() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        try (ResultIterator<Something> i = h.createQuery("select * from something order by id")
            .mapToBean(Something.class)
            .iterator()) {
            assertThat(i).hasNext();
            final Something first = i.next();
            assertThat(first.getName()).isEqualTo("eric");
            assertThat(i).hasNext();
            final Something second = i.next();
            assertThat(second.getId()).isEqualTo(2);
            assertThat(i).isExhausted();
        }
    }

    @Test
    public void testIteratorBehavior2() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        try (ResultIterator<Something> i = h.createQuery("select * from something order by id")
            .mapToBean(Something.class)
            .iterator()) {

            final Something first = i.next();
            assertThat(first.getName()).isEqualTo("eric");
            final Something second = i.next();
            assertThat(second.getId()).isEqualTo(2);
            assertThat(i).isExhausted();
        }
    }

    @Test
    public void testIteratorBehavior3() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'eric')");

        assertThat(h.createQuery("select * from something order by id").mapToBean(Something.class))
            .extracting(Something::getName)
            .containsExactly("eric", "eric");

    }

    @Test
    public void testFetchSize() {
        try (Handle h = h2Extension.getSharedHandle()) {

            try (Script script = h.createScript(findSqlOnClasspath("default-data"))) {
                script.execute();
            }

            try (Query query = h.createQuery("select id, name from something order by id")) {
                final ResultIterable<Something> ri = query.setFetchSize(1)
                    .mapToBean(Something.class);

                final ResultIterator<Something> r = ri.iterator();

                assertThat(r).hasNext();
                r.next();
                assertThat(r).hasNext();
                r.next();
                assertThat(r).isExhausted();
            }
        }
    }

    @Test
    public void testFirstWithNoResult() {
        final Handle h = h2Extension.getSharedHandle();

        final Optional<Something> s = h.createQuery("select id, name from something").mapToBean(Something.class).findFirst();
        assertThat(s).isNotPresent();
    }

    @Test
    public void testNullValueInColumn() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (?, ?)", 1, null);
        final String s = h.createQuery("select name from something where id=1").mapTo(String.class).first();
        assertThat(s).isNull();
    }

    @Test
    public void testListWithMaxRows() {
        final Handle h = h2Extension.getSharedHandle();

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
        final Handle h = h2Extension.getSharedHandle();

        h.prepareBatch("insert into something (id, name) values (?, ?)")
            .add(1, "Brian")
            .add(2, "Keith")
            .execute();

        final Map<String, Integer> rs = h.createQuery("select id, name from something")
            .<Entry<String, Integer>>map((r, ctx) -> Maps.immutableEntry(r.getString("name"), r.getInt("id")))
            .collect(toMap(Entry::getKey, Entry::getValue));

        assertThat(rs).containsOnly(entry("Brian", 1), entry("Keith", 2));
    }

    @Test
    public void testCollectList() {
        final Handle h = h2Extension.getSharedHandle();

        h.prepareBatch("insert into something (id, name) values (?, ?)")
            .add(1, "Brian")
            .add(2, "Keith")
            .execute();

        final List<String> rs = h.createQuery("select name from something order by id")
            .mapTo(String.class)
            .collect(toList());
        assertThat(rs).containsExactly("Brian", "Keith");
    }

    @Test
    public void testUsefulArgumentOutputForDebug() {
        final Handle h = h2Extension.getSharedHandle();

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
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (?, ?)", 1, "hello");
        h.execute("insert into something (id, name) values (?, ?)", 2, "world");

        final List<Something> rs = h.createQuery("select id, name from something")
            .setMaxRows(1)
            .mapToBean(Something.class)
            .list();

        assertThat(rs).hasSize(1);
    }

    @Test
    public void testQueriesWithNullResultSets() {
        final Handle h = h2Extension.getSharedHandle();

        assertThatThrownBy(() -> {
            try (Query query = h.select("insert into something (id, name) values (?, ?)", 1, "hello")) {
                query.mapToMap().list();
            }
        }).isInstanceOf(NoResultsException.class);
    }

    @Test
    public void testMapMapperOrdering() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (?, ?)", 1, "hello");
        h.execute("insert into something (id, name) values (?, ?)", 2, "world");

        final List<Map<String, Object>> rs = h.createQuery("select id, name from something")
            .mapToMap()
            .list();

        assertThat(rs).hasSize(2);
        assertThat(rs).hasOnlyElementsOfType(LinkedHashMap.class);
    }

    @Test
    public void testBindPrimitiveWithExplicitNull() {
        final Handle h = h2Extension.getSharedHandle();
        assertThat(h.createQuery("select :bool")
                .bindByType("bool", new NullArgument(Types.BOOLEAN), boolean.class)
                .mapTo(Boolean.class)
                .one())
            .isNull();
    }

    /**
     * Tests that {@link CharSequence} parameters behave as if they were Strings.
     */
    @Test
    public void testCharSequenceParms() {
        final Handle h = h2Extension.getSharedHandle();

        // create list of CharSequence types
        final List<CharSequence> names = Arrays.asList(
                String.valueOf(String.class.getSimpleName()),
                new StringBuffer(StringBuffer.class.getSimpleName()),
                new StringBuilder(StringBuilder.class.getSimpleName()),
                new CharSequence() { // anonymous inner class implementing CharSequence
                    private final CharSequence mySeq = "CharSequence";
                    @Override
                    public int length() {
                        return mySeq.length();
                    }
                    @Override
                    public char charAt(final int index) {
                        return mySeq.charAt(index);
                    }
                    @Override
                    public CharSequence subSequence(final int start, final int end) {
                        return mySeq.subSequence(start, end);
                    }
                    @Override
                    public String toString() {
                        return mySeq.toString();
                    }
                });
        for (int i = 0; i < names.size(); i++) {
            // insert CharSequence into name column of type VARCHAR
            h.execute("insert into something (id, name) values (?, ?)", i, names.get(i));
        }

        // read names back from database into Strings
        final List<String> rs1 = h.createQuery("select name from something order by id")
                .mapTo(String.class).list();
        assertThat(rs1).containsExactly("String", "StringBuffer", "StringBuilder", "CharSequence");

        // read names back from database into bean with CharSequence member
        final List<CharSeqName> rs2 = h.createQuery("select name from something order by id")
                .mapToBean(CharSeqName.class).list();
        assertThat(rs2).containsExactly(
                CharSeqName.of("String"), CharSeqName.of("StringBuffer"),
                CharSeqName.of("StringBuilder"), CharSeqName.of("CharSequence"));
    }

    /**
     * Test bean with a single member {@code name} of type {@link CharSequence}.
     */
    public static class CharSeqName {
        private CharSequence name;

        public CharSeqName() {}

        static CharSeqName of(final CharSequence name) {
            final CharSeqName csn = new CharSeqName();
            csn.setName(name);
            return csn;
        }

        public CharSequence getName() {
            return name;
        }

        public void setName(final CharSequence name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final CharSeqName other = (CharSeqName) obj;
            return Objects.equals(name, other.name);
        }
    }

    @Test
    public void testForEach() {
        final Handle h = h2Extension.getSharedHandle();
        final int nbRecs = 42;
        for (int id = 1; id <= nbRecs; id++) {
            h.execute("insert into something (id, name) values (?, ?)", id, null);
        }

        final ResultIterable<Integer> ri = h.createQuery("select id from something").mapTo(Integer.class);

        assertThatThrownBy(() -> ri.forEach(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("Action required");

        final AtomicInteger sum = new AtomicInteger(0);
        final int count = ri.forEachWithCount(sum::addAndGet);
        assertThat(count).isEqualTo(nbRecs);
        assertThat(sum.intValue()).isEqualTo(903);
    }

    @Test
    public void testFilter() {
        final Handle h = h2Extension.getSharedHandle();

        int id = 0;
        for (final String name : new String[] {null, "john", "lennon", "would have liked", "java"}) {
            h.execute("insert into something (id, name) values (?, ?)", ++id, name);
        }

        final ResultIterable<String> ri1 = h.createQuery("select name from something").mapTo(String.class);

        assertThatThrownBy(() -> ri1.filter(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("Filter required");

        final var startsWithJPredicate = new Predicate<String>() {
            int times;
            @Override
            public boolean test(final String s) {
                times++;
                return s != null && s.startsWith("j");
            }
        };
        final var containsHPredicate = new Predicate<String>() {
            int times;
            @Override
            public boolean test(final String s) {
                times++;
                return s.contains("h");
            }
        };

        final ResultIterable<String> ri2 = ri1
                .filter(startsWithJPredicate)
                .filter(containsHPredicate);

        // as iteration has not yet taken place, test filters were not invoked
        assertThat(startsWithJPredicate.times).isZero();
        assertThat(containsHPredicate.times).isZero();

        assertThat(ri1).isNotSameAs(ri2);

        final ResultIterator<String> iter = ri2.iterator();
        // mess with the resultset by calling hasNext repeatedly
        IntStream.range(0, 5).forEach(i -> assertThat(iter).hasNext());

        final List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        // the first filter should have seen all records
        assertThat(startsWithJPredicate.times).isEqualTo(5);
        // the second filter only two records that matched the first filter
        assertThat(containsHPredicate.times).isEqualTo(2);

        assertThatThrownBy(iter::next).isInstanceOf(NoSuchElementException.class)
                .hasMessage("No more filtered results");

        assertThat(results).containsExactly("john");
    }

}
