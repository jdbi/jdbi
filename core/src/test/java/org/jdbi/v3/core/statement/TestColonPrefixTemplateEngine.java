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
package org.jdbi.v3.core.statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class TestColonPrefixTemplateEngine {
    private TemplateEngine templateEngine;
    private SqlParser parser;
    private StatementContext ctx;

    @Before
    public void setUp() throws Exception {
        templateEngine = new DefinedAttributeTemplateEngine();
        parser = new ColonPrefixSqlParser();
        ctx = mock(StatementContext.class);
    }

    private String render(String sql) {
        return render(sql, Collections.emptyMap());
    }

    private String render(String sql, Map<String, Object> attributes) {
        attributes.forEach((key, value) -> when(ctx.getAttribute(key)).thenReturn(value));

        return templateEngine.render(sql, ctx);
    }

    @Test
    public void testNewlinesOkay() throws Exception {
        ParsedSql parsed = parser.parse("select * from something\n where id = :id", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from something\n where id = ?");
    }

    @Test
    public void testOddCharacters() throws Exception {
        ParsedSql parsed = parser.parse("~* :boo ':nope' _%&^& *@ :id", ctx);
        assertThat(parsed.getSql()).isEqualTo("~* ? ':nope' _%&^& *@ ?");
    }

    @Test
    public void testNumbers() throws Exception {
        ParsedSql parsed = parser.parse(":bo0 ':nope' _%&^& *@ :id", ctx);
        assertThat(parsed.getSql()).isEqualTo("? ':nope' _%&^& *@ ?");
    }

    @Test
    public void testDollarSignOkay() throws Exception {
        ParsedSql parsed = parser.parse("select * from v$session", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from v$session");
    }

    @Test
    public void testHashInColumnNameOkay() throws Exception {
        ParsedSql parsed = parser.parse("select column# from thetable where id = :id", ctx);
       assertThat(parsed.getSql()).isEqualTo("select column# from thetable where id = ?");
    }

    @Test
    public void testBacktickOkay() throws Exception {
        ParsedSql parsed = parser.parse("select * from `v$session", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from `v$session");
    }

    @Test
    public void testDoubleColon() throws Exception {
        final String doubleColon = "select 1::int";
        ParsedSql parsed = parser.parse(doubleColon, ctx);
        assertThat(parsed.getSql()).isEqualTo(doubleColon);
    }

    @Test(expected = UnableToCreateStatementException.class)
    public void testBailsOutOnInvalidInput() throws Exception {
        render("select * from something\n where id = :\u0087\u008e\u0092\u0097\u009c");
    }

    @Test
    public void testSubstitutesDefinedAttributes() throws Exception {
        Map<String, Object> attributes = ImmutableMap.of(
                "column", "foo",
                "table", "bar");
        String rendered = render("select <column> from <table> where <column> = :someValue", attributes);
        ParsedSql parsed = parser.parse(rendered, ctx);
        assertThat(parsed.getSql()).isEqualTo("select foo from bar where foo = ?");
    }

    @Test(expected = UnableToCreateStatementException.class)
    public void testUndefinedAttribute() throws Exception {
        render("select * from <table>", Collections.emptyMap());
    }

    @Test
    public void testLeaveEnquotedTokensIntact() throws Exception {
        String sql = "select '<foo>' foo, \"<bar>\" bar from something";
        assertThat(render(sql, ImmutableMap.of("foo", "no", "bar", "stahp"))).isEqualTo(sql);
    }

    @Test
    public void testIgnoreAngleBracketsNotPartOfToken() throws Exception {
        String sql = "select * from foo where end_date < ? and start_date > ?";
        assertThat(render(sql)).isEqualTo(sql);
    }

    @Test
    public void testCachesRewrittenStatements() throws Exception {
        parser = spy(parser);

        String sql = "insert into something (id, name) values (:id, :name)";
        ParsedSql parsed = parser.parse(sql, ctx);
        assertThat(parsed).isSameAs(parser.parse(sql, ctx));
    }

    @Test
    public void testCommentQuote() throws Exception {
        String sql = "select 1 /* ' \" <foo> */";
        assertThat(render(sql)).isEqualTo(sql);
    }

    @Test
    public void testColonInComment() throws Exception {
        String sql = "/* comment with : colons :: inside it */ select 1";
        assertThat(render(sql)).isEqualTo(sql);
    }
}
