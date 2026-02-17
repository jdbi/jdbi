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
package org.jdbi.freemarker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.BindBeanList;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BindBeanListTest {

    private Handle handle;

    private List<Something> expectedSomethings;

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something());

    @BeforeEach
    public void before() {
        final Jdbi db = h2Extension.getJdbi();
        db.installPlugin(new SqlObjectPlugin());
        db.registerRowMapper(new SomethingMapper());
        handle = db.open();

        handle.execute("insert into something(id, name) values(1, '1')");
        handle.execute("insert into something(id, name) values(2, '2')");

        // "control group" element that should *not* be returned by the queries
        handle.execute("insert into something(id, name) values(3, '3')");

        expectedSomethings = Arrays.asList(new Something(1, "1"), new Something(2, "2"));
    }

    @AfterEach
    public void after() {
        handle.close();
    }

    //

    @Test
    public void testSomethingWithExplicitAttributeName() {
        final SomethingWithExplicitAttributeName s = handle.attach(SomethingWithExplicitAttributeName.class);

        final List<Something> out = s.get(
                new SomethingKey(1, "1"),
                new SomethingKey(2, "2"));

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @UseFreemarkerEngine
    public interface SomethingWithExplicitAttributeName {
        @SqlQuery("select id, name from something where (id, name) in (${keys})")
        List<Something> get(@BindBeanList(value = "keys", propertyNames = {"id", "name"})
                                    SomethingKey... blarg);
    }

    //

    @Test
    public void testSomethingByVarargsWithVarargs() {
        final SomethingByVarargs s = handle.attach(SomethingByVarargs.class);

        final List<Something> out = s.get(
                new SomethingKey(1, "1"),
                new SomethingKey(2, "2"));

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @Test
    public void testSomethingByVarargsWithEmptyVarargs() {
        final SomethingByVarargs s = handle.attach(SomethingByVarargs.class);

        assertThatThrownBy(s::get).isInstanceOf(IllegalArgumentException.class);
    }

    @UseFreemarkerEngine
    public interface SomethingByVarargs {
        @SqlQuery("select id, name from something where (id, name) in (${keys})")
        List<Something> get(@BindBeanList(propertyNames = {"id", "name"}) SomethingKey... keys);
    }

    //

    @Test
    public void testSomethingByArrayWithNull() {
        final SomethingByArray s = handle.attach(SomethingByArray.class);

        assertThatThrownBy(() -> s.get(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSomethingByArrayWithEmptyArray() {
        final SomethingByArray s = handle.attach(SomethingByArray.class);

        assertThatThrownBy(() -> s.get(new SomethingKey[]{})).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSomethingByArrayWithNonEmptyArray() {
        final SomethingByVarargs s = handle.attach(SomethingByVarargs.class);

        final List<Something> out = s.get(
                new SomethingKey(1, "1"),
                new SomethingKey(2, "2"));

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @UseFreemarkerEngine
    private interface SomethingByArray {
        @SqlQuery("select id, name from something where (id, name) in (<keys>)")
        List<Something> get(@BindBeanList(propertyNames = {"id", "name"}) SomethingKey[] keys);
    }

    //

    @Test
    public void testSomethingByIterableWithIterable() {
        final SomethingByIterable s = handle.attach(SomethingByIterable.class);

        final List<Something> out = s.get(() -> Arrays.asList(new SomethingKey(1, "1"),
                new SomethingKey(2, "2"))
                .iterator());

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @Test
    public void testSomethingByIterableWithEmptyIterable() {
        final SomethingByIterable s = handle.attach(SomethingByIterable.class);

        assertThatThrownBy(() -> s.get(new ArrayList<>())).isInstanceOf(IllegalArgumentException.class);
    }

    @UseFreemarkerEngine
    public interface SomethingByIterable {
        @SqlQuery("select id, name from something where (id, name) in (${keys})")
        List<Something> get(@BindBeanList(propertyNames = {"id", "name"}) Iterable<SomethingKey> keys);
    }

    //

    public void testSomethingByIterator() {
        final SomethingByIterator s = handle.attach(SomethingByIterator.class);

        List<Something> results = s.get(Arrays.asList(
                new SomethingKey(1, "1"),
                new SomethingKey(2, "2"))
            .iterator());
        assertThat(results).hasSameElementsAs(expectedSomethings);
    }

    @UseFreemarkerEngine
    public interface SomethingByIterator {
        @SqlQuery("select id, name from something where (id, name) in (<keys>)")
        List<Something> get(@BindBeanList(propertyNames = {"id", "name"}) Iterator<SomethingKey> keys);
    }

    public static class SomethingKey {
        private final int id;
        private final String name;

        SomethingKey(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
