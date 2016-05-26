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

import com.google.common.collect.ImmutableMap;

import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.rewriter.HashPrefixStatementRewriter;
import org.jdbi.v3.rewriter.RewrittenStatement;
import org.junit.Before;
import org.junit.Test;

public class TestHashPrefixStatementRewriter
{
    private HashPrefixStatementRewriter rw;

    @Before
    public void setUp() throws Exception
    {
        this.rw = new HashPrefixStatementRewriter();
    }

    private RewrittenStatement rewrite(String sql)
    {
        Map<String, Object> attributes = Collections.emptyMap();
        return rewrite(sql, attributes);
    }

    private RewrittenStatement rewrite(String sql, Map<String, Object> attributes) {
        StatementContext ctx = new StatementContext();
        attributes.forEach(ctx::setAttribute);

        return rw.rewrite(sql, new Binding(), ctx);
    }

    @Test
    public void testNewlinesOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from something\n where id = #id");
        assertEquals("select * from something\n where id = ?", rws.getSql());
    }

    @Test
    public void testOddCharacters() throws Exception
    {
        RewrittenStatement rws = rewrite("~* #boo '#nope' _%&^& *@ #id");
        assertEquals("~* ? '#nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testNumbers() throws Exception
    {
        RewrittenStatement rws = rewrite("#bo0 '#nope' _%&^& *@ #id");
        assertEquals("? '#nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testDollarSignOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from v$session");
        assertEquals("select * from v$session", rws.getSql());
    }

    @Test
    public void testColonIsLiteral() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from foo where id = :id");
        assertEquals("select * from foo where id = :id", rws.getSql());
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
        rewrite("select * from something\n where id = #\u0087\u008e\u0092\u0097\u009c");
    }

    @Test
    public void testSubstitutesDefinedAttributes() throws Exception
    {
        Map<String, Object> attributes = ImmutableMap.of(
                "column", "foo",
                "table", "bar");
        RewrittenStatement rws = rewrite("select <column> from <table> where <column> = #someValue", attributes);
        assertEquals("select foo from bar where foo = ?", rws.getSql());
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
        assertEquals(sql, rewrite(sql, ImmutableMap.of("foo", "no", "bar", "stahp")).getSql());
    }

    @Test
    public void testIgnoreAngleBracketsNotPartOfToken() throws Exception
    {
        String sql = "select * from foo where end_date < ? and start_date > ?";
        assertEquals(sql, rewrite(sql).getSql());
    }

    @Test
    public void testCommentQuote() throws Exception
    {
        String sql = "select 1 /* ' \" <foo> */";
        assertEquals(sql, rewrite(sql).getSql());
    }
}
