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
package org.jdbi.v3.sqlobject;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.customizer.BindBeanList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.stringtemplate4.UseStringTemplateStatementRewriter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BindBeanListTest
{
    private static Handle handle;

    private static List<Something> expectedSomethings;

    @ClassRule
    public static final H2DatabaseRule db = new H2DatabaseRule();

    @BeforeClass
    public static void init()
    {
        final Jdbi dbi = db.getJdbi();
        dbi.installPlugin(new SqlObjectPlugin());
        dbi.registerRowMapper(new SomethingMapper());
        handle = dbi.open();

        handle.execute("insert into something(id, name) values(1, '1')");
        handle.execute("insert into something(id, name) values(2, '2')");

        // "control group" element that should *not* be returned by the queries
        handle.execute("insert into something(id, name) values(3, '3')");

        expectedSomethings = Arrays.asList(new Something(1, "1"), new Something(2, "2"));
    }

    @AfterClass
    public static void exit()
    {
        handle.close();
    }

    //

    @Test
    public void testSomethingWithExplicitAttributeName()
    {
        final SomethingWithExplicitAttributeName s = handle.attach(SomethingWithExplicitAttributeName.class);

        final List<Something> out = s.get(
                new SomethingKey(1, "1"),
                new SomethingKey(2, "2"));

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @UseStringTemplateStatementRewriter
    public interface SomethingWithExplicitAttributeName {
        @SqlQuery("select id, name from something where (id, name) in (<keys>)")
        List<Something> get(@BindBeanList(value = "keys", propertyNames = {"id", "name"})
                                    SomethingKey... blarg);
    }

    //

    @Test
    public void testSomethingByVarargsWithVarargs()
    {
        final SomethingByVarargs s = handle.attach(SomethingByVarargs.class);

        final List<Something> out = s.get(
                new SomethingKey(1, "1"),
                new SomethingKey(2, "2"));

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSomethingByVarargsWithEmptyVarargs()
    {
        final SomethingByVarargs s = handle.attach(SomethingByVarargs.class);

        final List<Something> out = s.get();
    }

    @UseStringTemplateStatementRewriter
    public interface SomethingByVarargs
    {
        @SqlQuery("select id, name from something where (id, name) in (<keys>)")
        List<Something> get(@BindBeanList(propertyNames = {"id", "name"}) SomethingKey... keys);
    }

    //

    @Test(expected = IllegalArgumentException.class)
    public void testSomethingByArrayWithNull()
    {
        final SomethingByArray s = handle.attach(SomethingByArray.class);

        s.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSomethingByArrayWithEmptyArray()
    {
        final SomethingByArray s = handle.attach(SomethingByArray.class);

        s.get(new SomethingKey[]{});
    }

    @Test
    public void testSomethingByArrayWithNonEmptyArray()
    {
        final SomethingByVarargs s = handle.attach(SomethingByVarargs.class);

        final List<Something> out = s.get(
                new SomethingKey(1, "1"),
                new SomethingKey(2, "2"));

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @UseStringTemplateStatementRewriter
    private interface SomethingByArray
    {
        @SqlQuery("select id, name from something where (id, name) in (<keys>)")
        List<Something> get(@BindBeanList(propertyNames = {"id", "name"}) SomethingKey[] keys);
    }

    //

    @Test
    public void testSomethingByIterableWithIterable()
    {
        final SomethingByIterable s = handle.attach(SomethingByIterable.class);

        final List<Something> out = s.get(() -> Arrays.asList(new SomethingKey(1, "1"),
                new SomethingKey(2, "2"))
                .iterator());

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSomethingByIterableWithEmptyIterable()
    {
        final SomethingByIterable s = handle.attach(SomethingByIterable.class);

        final List<Something> out = s.get(new ArrayList<>());

        assertThat(out).isEmpty();
    }

    @UseStringTemplateStatementRewriter
    public interface SomethingByIterable
    {
        @SqlQuery("select id, name from something where (id, name) in (<keys>)")
        List<Something> get(@BindBeanList(propertyNames = {"id", "name"}) Iterable<SomethingKey> keys);
    }

    //

    @Test(expected = IllegalArgumentException.class)
    public void testSomethingByIterator()
    {
        final SomethingByIterator s = handle.attach(SomethingByIterator.class);

        s.get(Arrays.asList(new SomethingKey(1, "1"),
                new SomethingKey(2, "2")).iterator());
    }

    @UseStringTemplateStatementRewriter
    public interface SomethingByIterator
    {
        @SqlQuery("select id, name from something where (id, name) in (<keys>)")
        List<Something> get(@BindBeanList(propertyNames = {"id", "name"}) Iterator<SomethingKey> keys);
    }

    public static class SomethingKey {
        private int id;
        private String name;

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
