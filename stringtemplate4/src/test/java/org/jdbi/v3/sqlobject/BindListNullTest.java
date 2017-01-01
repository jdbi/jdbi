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
import static org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling.NULL;
import static org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling.VOID;

import java.util.ArrayList;
import java.util.List;
import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rewriter.ColonPrefixStatementRewriter;
import org.jdbi.v3.core.rewriter.RewrittenStatement;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.stringtemplate4.UseStringTemplateStatementRewriter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class BindListNullTest
{
    private static final String SPY = "__test_spy";
    private static Handle handle;

    @ClassRule
    public static final H2DatabaseRule db = new H2DatabaseRule();

    @BeforeClass
    public static void init()
    {
        final Jdbi dbi = db.getJdbi();
        dbi.registerRowMapper(new SomethingMapper());
        dbi.installPlugin(new SqlObjectPlugin());
        handle = dbi.open();

        handle.execute("insert into something(id, name) values(1, null)");
        handle.execute("insert into something(id, name) values(2, null)");
        handle.execute("insert into something(id, name) values(3, null)");
        handle.execute("insert into something(id, name) values(4, null)");

        handle.execute("insert into something(id, name) values(5, 'bla')");
        handle.execute("insert into something(id, name) values(6, 'null')");
        handle.execute("insert into something(id, name) values(7, '')");
    }

    @AfterClass
    public static void exit()
    {
        handle.close();
    }

    @Test
    public void testSomethingByIterableHandleNullWithNull()
    {
        final SomethingByIterableHandleNull s = handle.attach(SomethingByIterableHandleNull.class);

        final List<Something> out = s.get(null);

        assertThat(out).isEmpty();
    }

    @Test
    public void testSomethingByIterableHandleNullWithEmptyList()
    {
        final SomethingByIterableHandleNull s = handle.attach(SomethingByIterableHandleNull.class);

        final List<Something> out = s.get(new ArrayList<Object>());

        assertThat(out).isEmpty();
    }

    public interface SomethingByIterableHandleNull
    {
        @SqlQuery("select id, name from something where name in (<names>)")
        List<Something> get(@BindList(onEmpty = NULL) Iterable<Object> names);
    }

    //

    @Test
    public void testSomethingByIterableHandleVoidWithNull()
    {
        final List<String> log = new ArrayList<>();
        handle.define(SPY, log);
        final SomethingByIterableHandleVoid s = handle.attach(SomethingByIterableHandleVoid.class);

        final List<Something> out = s.get(null);

        assertThat(out).isEmpty();
        assertThat(log).hasSize(1).allMatch(e -> e.contains(" where id in ();"));
    }

    @Test
    public void testSomethingByIterableHandleVoidWithEmptyList()
    {
        final List<String> log = new ArrayList<>();
        handle.define(SPY, log);
        final SomethingByIterableHandleVoid s = handle.attach(SomethingByIterableHandleVoid.class);

        final List<Something> out = s.get(new ArrayList<Object>());

        assertThat(out).isEmpty();
        assertThat(log).hasSize(1).allMatch(e -> e.contains(" where id in ();"));
    }

    @UseStringTemplateStatementRewriter(SpyingRewriter.class)
    public interface SomethingByIterableHandleVoid
    {
        @SqlQuery("select id, name from something where id in (<ids>);")
        List<Something> get(@BindList(onEmpty = VOID) Iterable<Object> ids);
    }

    public static class SpyingRewriter extends ColonPrefixStatementRewriter
    {
        @SuppressWarnings("unchecked")
        @Override
        public RewrittenStatement rewrite(String sql, Binding params,
                StatementContext ctx)
        {
            ((List<String>)ctx.getAttribute(SPY)).add(sql);
            return super.rewrite(sql, params, ctx);
        }
    }
}
