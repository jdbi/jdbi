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
package org.jdbi.sqlobject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.customizer.BindList;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.stringtemplate4.UseStringTemplateEngine;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jdbi.sqlobject.customizer.BindList.EmptyHandling.NULL_VALUE;
import static org.jdbi.sqlobject.customizer.BindList.EmptyHandling.THROW;
import static org.jdbi.sqlobject.customizer.BindList.EmptyHandling.VOID;

public class BindListTest {

    @RegisterExtension
    public final JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private List<Something> expectedSomethings;

    @BeforeEach
    public void before() {
        handle = h2Extension.getJdbi()
            .registerRowMapper(new SomethingMapper())
            .open();

        handle.execute("insert into something(id, name) values(1, '1')");
        handle.execute("insert into something(id, name) values(2, '2')");

        // "control group" element that should *not* be returned by the queries
        handle.execute("insert into something(id, name) values(3, '3')");

        expectedSomethings = Arrays.asList(new Something(1, "1"), new Something(2, "2"));
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

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

        final List<Something> out = s.get(() -> Arrays.asList(1, 2).iterator());

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
        List<String> getForNull(@BindList(value = "name", onEmpty = NULL_VALUE) List<String> name);

        @SqlQuery("select name from something <if(name)> where name in (<name>) <endif>")
        @UseStringTemplateEngine
        List<String> getForValue(@BindList(value = "name", onEmpty = NULL_VALUE) List<String> name);
    }
}
