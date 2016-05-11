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
package org.jdbi.v3;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableMap;

import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.tweak.RewrittenStatement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


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
        StatementContext ctx = new StatementContext();
        attributes.forEach(ctx::setAttribute);

        return rw.rewrite(sql, new Binding(), ctx);
    }

    @Test
    public void testNewlinesOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from something\n where id = :id");
        assertEquals("select * from something\n where id = ?", rws.getSql());
    }

    @Test
    public void testOddCharacters() throws Exception
    {
        RewrittenStatement rws = rewrite("~* :boo ':nope' _%&^& *@ :id");
        assertEquals("~* ? ':nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testNumbers() throws Exception
    {
        RewrittenStatement rws = rewrite(":bo0 ':nope' _%&^& *@ :id");
        assertEquals("? ':nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testDollarSignOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from v$session");
        assertEquals("select * from v$session", rws.getSql());
    }

    @Test
    public void testHashInColumnNameOkay() throws Exception
    {
       RewrittenStatement rws = rewrite("select column# from thetable where id = :id");
       assertEquals("select column# from thetable where id = ?", rws.getSql());
    }

    @Test
    public void testBacktickOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from `v$session");
        assertEquals("select * from `v$session", rws.getSql());
    }

    @Test(expected = UnableToCreateStatementException.class)
    public void testBailsOutOnInvalidInput() throws Exception
    {
        rewrite("select * from something\n where id = :\u0087\u008e\u0092\u0097\u009c");
        Assert.fail("Expected 'UnableToCreateStatementException' but got none");
    }

    @Test
    public void testSubstitutesAttributesForAngleBracketTokens() throws Exception
    {
        Map<String, Object> attributes = ImmutableMap.of(
                "column", "foo",
                "table", "bar",
                "condition", "baz");
        RewrittenStatement rws = rewrite("select <column> from <table> where <column> = :someValue", attributes);
        assertEquals("select foo from bar where foo = ?", rws.getSql());
    }

    @Test(expected = UnableToCreateStatementException.class)
    public void testMissingAttributeForAngleBracketToken() throws Exception
    {
        rewrite("select * from <table>", Collections.emptyMap());
    }

    @Test
    public void testLeaveEnquotedAngleBracketTokensIntact() throws Exception
    {
        String sql = "select '<foo>' foo, \"<bar>\" bar from something";
        assertEquals(sql, rewrite(sql, ImmutableMap.of("foo", "no", "bar", "stahp")).getSql());
    }

    @Test
    public void testIgnoreAngleBracketsNotEnclosingAToken() throws Exception
    {
        String sql = "select * from foo where end_date < ? and start_date > ?";
        assertEquals(sql, rewrite(sql, ImmutableMap.of("table", "foo")).getSql());
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

        assertEquals(1, ctr.get());

        rewrite("insert into something (id, name) values (:id, :name)");

        assertEquals(1, ctr.get());
    }

    public void testCommentQuote() throws Exception
    {
        rewrite("select 1 /* ' \" */");
    }
}
