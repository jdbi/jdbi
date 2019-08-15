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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.stringtemplate4.UseStringTemplateEngine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling.NULL_VALUE;
import static org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling.THROW;
import static org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling.VOID;

public class BindListTest {
    @Rule
    public final H2DatabaseRule h2 = new H2DatabaseRule().withSomething().withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private List<Something> expectedSomethings;

    @Before
    public void before() {
        handle = h2.getJdbi()
            .registerRowMapper(new SomethingMapper())
            .open();

        handle.execute("insert into something(id, name) values(1, '1')");
        handle.execute("insert into something(id, name) values(2, '2')");

        // "control group" element that should *not* be returned by the queries
        handle.execute("insert into something(id, name) values(3, '3')");

        expectedSomethings = Arrays.asList(new Something(1, "1"), new Something(2, "2"));
    }

    //

    @Test
    public void testSomethingWithExplicitAttributeName() {
        final SomethingWithExplicitAttributeName s = handle.attach(SomethingWithExplicitAttributeName.class);

        final List<Something> out = s.get(1, 2);

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @UseStringTemplateEngine
    public interface SomethingWithExplicitAttributeName {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindList("ids") int... blarg);
    }

    //

    @Test
    public void testSomethingByVarargsHandleDefaultWithVarargs() {
        final SomethingByVarargsHandleDefault s = handle.attach(SomethingByVarargsHandleDefault.class);

        final List<Something> out = s.get(1, 2);

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @UseStringTemplateEngine
    public interface SomethingByVarargsHandleDefault {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindList int... ids);
    }

    //

    @Test
    public void testSomethingByArrayHandleVoidWithArray() {
        final SomethingByArrayHandleVoid s = handle.attach(SomethingByArrayHandleVoid.class);

        final List<Something> out = s.get(new int[]{1, 2});

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @Test
    public void testSomethingByArrayHandleVoidWithEmptyArray() {
        final SomethingByArrayHandleVoid s = handle.attach(SomethingByArrayHandleVoid.class);

        final List<Something> out = s.get(new int[]{});

        assertThat(out).isEmpty();
    }

    @Test
    public void testSomethingByArrayHandleVoidWithNull() {
        final SomethingByArrayHandleVoid s = handle.attach(SomethingByArrayHandleVoid.class);

        final List<Something> out = s.get(null);

        assertThat(out).isEmpty();
    }

    @UseStringTemplateEngine
    public interface SomethingByArrayHandleVoid {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindList(onEmpty = VOID) int[] ids);
    }

    //

    @Test
    public void testSomethingByArrayHandleThrowWithNull() {
        final SomethingByArrayHandleThrow s = handle.attach(SomethingByArrayHandleThrow.class);

        assertThatThrownBy(() -> s.get(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSomethingByArrayHandleThrowWithEmptyArray() {
        final SomethingByArrayHandleThrow s = handle.attach(SomethingByArrayHandleThrow.class);

        assertThatThrownBy(() -> s.get(new int[]{})).isInstanceOf(IllegalArgumentException.class);
    }

    @UseStringTemplateEngine
    private interface SomethingByArrayHandleThrow {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindList(onEmpty = THROW) int[] ids);
    }

    //

    @Test
    public void testSomethingByIterableHandleDefaultWithIterable() {
        final SomethingByIterableHandleDefault s = handle.attach(SomethingByIterableHandleDefault.class);

        final List<Something> out = s.get(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return Arrays.asList(1, 2).iterator();
            }
        });

        assertThat(out).hasSameElementsAs(expectedSomethings);
    }

    @Test
    public void testSomethingByIterableHandleDefaultWithEmptyIterable() {
        final SomethingByIterableHandleDefault s = handle.attach(SomethingByIterableHandleDefault.class);

        final List<Something> out = s.get(new ArrayList<>());

        assertThat(out).isEmpty();
    }

    @UseStringTemplateEngine
    public interface SomethingByIterableHandleDefault {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindList(onEmpty = VOID) Iterable<Integer> ids);
    }

    //

    @Test
    public void testSomethingByIterableHandleThrowWithEmptyIterable() {
        final SomethingByIterableHandleThrow s = handle.attach(SomethingByIterableHandleThrow.class);

        assertThatThrownBy(() -> s.get(new ArrayList<>())).isInstanceOf(IllegalArgumentException.class);
    }

    @UseStringTemplateEngine
    private interface SomethingByIterableHandleThrow {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindList(onEmpty = THROW) Iterable<Integer> ids);
    }

    //

    @Test
    public void testSomethingByIteratorHandleDefault() {
        final SomethingByIteratorHandleDefault s = handle.attach(SomethingByIteratorHandleDefault.class);

        assertThat(s.get(Arrays.asList(1, 2).iterator())).hasSameElementsAs(expectedSomethings);
    }

    @UseStringTemplateEngine
    private interface SomethingByIteratorHandleDefault {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindList Iterator<Integer> ids);
    }

    //

    @Test
    public void ifNullOrEmptyWithNullValueOptionThenResultIsFalsy() {
        handle.createUpdate("insert into something(id, name) values(4, null)").execute();

        // control groups

        List<String> allNames = handle.createQuery("select name from something")
            .mapTo(String.class)
            .list();
        assertThat(allNames).hasSize(4);

        List<String> nullNames = handle.createQuery("select name from something where name is null")
            .mapTo(String.class)
            .list();
        assertThat(nullNames).hasSize(1);

        // actual cases

        ConditionalDao dao = handle.attach(ConditionalDao.class);

        List<String> names = dao.getForNull(null);
        assertThat(names)
            .describedAs("ST did not evaluate null as truthy, query did not select by `name is null`")
            .hasSize(4);

        names = dao.getForNull(Collections.emptyList());
        assertThat(names)
            .describedAs("ST did not evaluate empty list as truthy, query did not select by `name is null`")
            .hasSize(4);
    }

    @Test
    public void ifValueGivenWithNullValueOptionThenResultIsTruthy() {
        ConditionalDao dao = handle.attach(ConditionalDao.class);

        List<String> names = dao.getForValue(Collections.singletonList("2"));
        assertThat(names).hasSize(1);
    }

    private interface ConditionalDao {
        // `in (null)` doesn't work on h2
        @SqlQuery("select name from something <if(name)> where name is <name> <endif>")
        @UseStringTemplateEngine
        List<String> getForNull(@Nullable @BindList(value = "name", onEmpty = NULL_VALUE) List<String> name);

        @SqlQuery("select name from something <if(name)> where name in (<name>) <endif>")
        @UseStringTemplateEngine
        List<String> getForValue(@Nonnull @BindList(value = "name", onEmpty = NULL_VALUE) List<String> name);
    }
}
