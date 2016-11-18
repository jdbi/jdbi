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

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.sqlobject.customizers.BindIn.EmptyHandling.THROW;
import static org.jdbi.v3.sqlobject.customizers.BindIn.EmptyHandling.VOID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.BindIn;
import org.jdbi.v3.stringtemplate.UseStringTemplateStatementRewriter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class BindInTest
{
    private static Handle handle;

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

        final List<Something> out = s.get(1, 2);

        assertThat(out).hasSize(2);
    }

    @UseStringTemplateStatementRewriter
    public interface SomethingWithExplicitAttributeName
    {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindIn("ids") int... blarg);
    }

    //

    @Test
    public void testSomethingByVarargsHandleDefaultWithVarargs()
    {
        final SomethingByVarargsHandleDefault s = handle.attach(SomethingByVarargsHandleDefault.class);

        final List<Something> out = s.get(1, 2);

        assertThat(out).hasSize(2);
    }

    @UseStringTemplateStatementRewriter
    public interface SomethingByVarargsHandleDefault
    {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindIn int... ids);
    }

    //

    @Test
    public void testSomethingByArrayHandleVoidWithArray()
    {
        final SomethingByArrayHandleVoid s = handle.attach(SomethingByArrayHandleVoid.class);

        final List<Something> out = s.get(new int[]{1, 2});

        assertThat(out).hasSize(2);
    }

    @Test
    public void testSomethingByArrayHandleVoidWithEmptyArray()
    {
        final SomethingByArrayHandleVoid s = handle.attach(SomethingByArrayHandleVoid.class);

        final List<Something> out = s.get(new int[]{});

        assertThat(out).isEmpty();
    }

    @Test
    public void testSomethingByArrayHandleVoidWithNull()
    {
        final SomethingByArrayHandleVoid s = handle.attach(SomethingByArrayHandleVoid.class);

        final List<Something> out = s.get(null);

        assertThat(out).isEmpty();
    }

    @UseStringTemplateStatementRewriter
    public interface SomethingByArrayHandleVoid
    {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindIn(onEmpty = VOID) int[] ids);
    }

    //

    @Test(expected = IllegalArgumentException.class)
    public void testSomethingByArrayHandleThrowWithNull()
    {
        final SomethingByArrayHandleThrow s = handle.attach(SomethingByArrayHandleThrow.class);

        s.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSomethingByArrayHandleThrowWithEmptyArray()
    {
        final SomethingByArrayHandleThrow s = handle.attach(SomethingByArrayHandleThrow.class);

        s.get(new int[]{});
    }

    @UseStringTemplateStatementRewriter
    private interface SomethingByArrayHandleThrow
    {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindIn(onEmpty = THROW) int[] ids);
    }

    //

    @Test
    public void testSomethingByIterableHandleDefaultWithIterable()
    {
        final SomethingByIterableHandleDefault s = handle.attach(SomethingByIterableHandleDefault.class);

        final List<Something> out = s.get(new Iterable<Integer>()
        {
            @Override
            public Iterator<Integer> iterator()
            {
                return Arrays.asList(1, 2).iterator();
            }
        });

        assertThat(out).hasSize(2);
    }

    @Test
    public void testSomethingByIterableHandleDefaultWithEmptyIterable()
    {
        final SomethingByIterableHandleDefault s = handle.attach(SomethingByIterableHandleDefault.class);

        final List<Something> out = s.get(new ArrayList<Integer>());

        assertThat(out).isEmpty();
    }

    @UseStringTemplateStatementRewriter
    public interface SomethingByIterableHandleDefault
    {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindIn(onEmpty = VOID) Iterable<Integer> ids);
    }

    //

    @Test(expected = IllegalArgumentException.class)
    public void testSomethingByIterableHandleThrowWithEmptyIterable()
    {
        final SomethingByIterableHandleThrow s = handle.attach(SomethingByIterableHandleThrow.class);

        s.get(new ArrayList<Integer>());
    }

    @UseStringTemplateStatementRewriter
    private interface SomethingByIterableHandleThrow
    {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindIn(onEmpty = THROW) Iterable<Integer> ids);
    }

    //

    @Test(expected = IllegalArgumentException.class)
    public void testSomethingByIteratorHandleDefault()
    {
        final SomethingByIteratorHandleDefault s = handle.attach(SomethingByIteratorHandleDefault.class);

        s.get(Arrays.asList(1, 2).iterator());
    }

    @UseStringTemplateStatementRewriter
    private interface SomethingByIteratorHandleDefault
    {
        @SqlQuery("select id, name from something where id in (<ids>)")
        List<Something> get(@BindIn Iterator<Integer> ids);
    }
}
