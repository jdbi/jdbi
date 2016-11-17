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
package org.jdbi.v3.core.rewriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Binding;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.exception.UnableToCreateStatementException;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import org.mockito.Mockito;

public class TestColonStatementRewriter
{
    private ColonPrefixStatementRewriter rw;

    @Before
    public void setUp() throws Exception
    {
        this.rw = new ColonPrefixStatementRewriter();
    }

    private RewrittenStatement rewrite(String sql)
    {
        return rewrite(sql, Collections.emptyMap());
    }

    private RewrittenStatement rewrite(String sql, Map<String, Object> attributes) {
        StatementContext ctx = Mockito.mock(StatementContext.class);
        SqlStatements config = new SqlStatements();
        config.putAttributes(attributes);
        when(ctx.getConfig(SqlStatements.class)).thenReturn(config);

        return rw.rewrite(sql, new Binding(), ctx);
    }

    @Test
    public void testNewlinesOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from something\n where id = :id");
        assertThat(rws.getSql()).isEqualTo("select * from something\n where id = ?");
    }

    @Test
    public void testOddCharacters() throws Exception
    {
        RewrittenStatement rws = rewrite("~* :boo ':nope' _%&^& *@ :id");
        assertThat(rws.getSql()).isEqualTo("~* ? ':nope' _%&^& *@ ?");
    }

    @Test
    public void testNumbers() throws Exception
    {
        RewrittenStatement rws = rewrite(":bo0 ':nope' _%&^& *@ :id");
        assertThat(rws.getSql()).isEqualTo("? ':nope' _%&^& *@ ?");
    }

    @Test
    public void testDollarSignOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from v$session");
        assertThat(rws.getSql()).isEqualTo("select * from v$session");
    }

    @Test
    public void testHashInColumnNameOkay() throws Exception
    {
       RewrittenStatement rws = rewrite("select column# from thetable where id = :id");
       assertThat(rws.getSql()).isEqualTo("select column# from thetable where id = ?");
    }

    @Test
    public void testBacktickOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from `v$session");
        assertThat(rws.getSql()).isEqualTo("select * from `v$session");
    }

    @Test
    public void testDoubleColon() throws Exception
    {
        final String doubleColon = "select 1::int";
        RewrittenStatement rws = rewrite(doubleColon);
        assertThat(rws.getSql()).isEqualTo(doubleColon);
    }

    @Test(expected = UnableToCreateStatementException.class)
    public void testBailsOutOnInvalidInput() throws Exception
    {
        rewrite("select * from something\n where id = :\u0087\u008e\u0092\u0097\u009c");
    }

    @Test
    public void testSubstitutesDefinedAttributes() throws Exception
    {
        Map<String, Object> attributes = ImmutableMap.of(
                "column", "foo",
                "table", "bar");
        RewrittenStatement rws = rewrite("select <column> from <table> where <column> = :someValue", attributes);
        assertThat(rws.getSql()).isEqualTo("select foo from bar where foo = ?");
    }

    @Test(expected = UnableToCreateStatementException.class)
    public void testUndefinedAttribute() throws Exception
    {
        rewrite("select * from <table>", Collections.emptyMap());
    }

    @Test
    public void testLeaveEnquotedTokensIntact() throws Exception
    {
        String sql = "select '<foo>' foo, \"<bar>\" bar from something";
        assertThat(rewrite(sql, ImmutableMap.of("foo", "no", "bar", "stahp")).getSql()).isEqualTo(sql);
    }

    @Test
    public void testIgnoreAngleBracketsNotPartOfToken() throws Exception
    {
        String sql = "select * from foo where end_date < ? and start_date > ?";
        assertThat(rewrite(sql).getSql()).isEqualTo(sql);
    }

    @Test
    public void testCachesRewrittenStatements() throws Exception
    {
        final AtomicInteger ctr = new AtomicInteger(0);
        rw = new ColonPrefixStatementRewriter()
        {
            @Override
            ParsedStatement rewriteNamedParameters(final String sql) throws IllegalArgumentException
            {
                ctr.incrementAndGet();
                return super.rewriteNamedParameters(sql);
            }
        };

        rewrite("insert into something (id, name) values (:id, :name)");

        assertThat(ctr.get()).isEqualTo(1);

        rewrite("insert into something (id, name) values (:id, :name)");

        assertThat(ctr.get()).isEqualTo(1);
    }

    @Test
    public void testCommentQuote() throws Exception
    {
        String sql = "select 1 /* ' \" <foo> */";
        assertThat(rewrite(sql).getSql()).isEqualTo(sql);
    }
}
