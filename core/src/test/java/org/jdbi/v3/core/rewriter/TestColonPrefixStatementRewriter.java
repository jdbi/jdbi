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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import org.mockito.Mockito;

public class TestColonPrefixStatementRewriter
{
    private StatementRewriter rewriter;
    private StatementParser parser;

    @Before
    public void setUp() throws Exception
    {
        rewriter = new DefinedAttributeRewriter();
        parser = new ColonPrefixStatementParser();
    }

    private String rewrite(String sql)
    {
        return rewrite(sql, Collections.emptyMap());
    }

    private String rewrite(String sql, Map<String, Object> attributes) {
        StatementContext ctx = Mockito.mock(StatementContext.class);
        attributes.forEach((key, value) -> when(ctx.getAttribute(key)).thenReturn(value));

        return rewriter.rewrite(sql, ctx);
    }

    @Test
    public void testNewlinesOkay() throws Exception
    {
        ParsedStatement parsed = parser.parse("select * from something\n where id = :id");
        assertThat(parsed.getSql()).isEqualTo("select * from something\n where id = ?");
    }

    @Test
    public void testOddCharacters() throws Exception
    {
        ParsedStatement parsed = parser.parse("~* :boo ':nope' _%&^& *@ :id");
        assertThat(parsed.getSql()).isEqualTo("~* ? ':nope' _%&^& *@ ?");
    }

    @Test
    public void testNumbers() throws Exception
    {
        ParsedStatement parsed = parser.parse(":bo0 ':nope' _%&^& *@ :id");
        assertThat(parsed.getSql()).isEqualTo("? ':nope' _%&^& *@ ?");
    }

    @Test
    public void testDollarSignOkay() throws Exception
    {
        ParsedStatement parsed = parser.parse("select * from v$session");
        assertThat(parsed.getSql()).isEqualTo("select * from v$session");
    }

    @Test
    public void testHashInColumnNameOkay() throws Exception
    {
        ParsedStatement parsed = parser.parse("select column# from thetable where id = :id");
       assertThat(parsed.getSql()).isEqualTo("select column# from thetable where id = ?");
    }

    @Test
    public void testBacktickOkay() throws Exception
    {
        ParsedStatement parsed = parser.parse("select * from `v$session");
        assertThat(parsed.getSql()).isEqualTo("select * from `v$session");
    }

    @Test
    public void testDoubleColon() throws Exception
    {
        final String doubleColon = "select 1::int";
        ParsedStatement parsed = parser.parse(doubleColon);
        assertThat(parsed.getSql()).isEqualTo(doubleColon);
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
        String rewritten = rewrite("select <column> from <table> where <column> = :someValue", attributes);
        ParsedStatement parsed = parser.parse(rewritten);
        assertThat(parsed.getSql()).isEqualTo("select foo from bar where foo = ?");
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
        assertThat(rewrite(sql, ImmutableMap.of("foo", "no", "bar", "stahp"))).isEqualTo(sql);
    }

    @Test
    public void testIgnoreAngleBracketsNotPartOfToken() throws Exception
    {
        String sql = "select * from foo where end_date < ? and start_date > ?";
        assertThat(rewrite(sql)).isEqualTo(sql);
    }

    @Test
    public void testCachesRewrittenStatements() throws Exception
    {
        parser = spy(parser);

        String sql = "insert into something (id, name) values (:id, :name)";
        ParsedStatement parsed = parser.parse(sql);
        assertThat(parsed).isSameAs(parser.parse(sql));
    }

    @Test
    public void testCommentQuote() throws Exception
    {
        String sql = "select 1 /* ' \" <foo> */";
        assertThat(rewrite(sql)).isEqualTo(sql);
    }

    @Test
    public void testColonInComment() throws Exception {
        String sql = "/* comment with : colons :: inside it */ select 1";
        assertThat(rewrite(sql)).isEqualTo(sql);
    }
}
