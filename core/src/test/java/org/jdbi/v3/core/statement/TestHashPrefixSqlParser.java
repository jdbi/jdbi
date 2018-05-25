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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class TestHashPrefixSqlParser {
    private SqlParser parser;
    private StatementContext ctx;

    @Before
    public void setUp() throws Exception {
        this.parser = new HashPrefixSqlParser();
        ctx = mock(StatementContext.class);
    }

    private String render(String sql) {
        return render(sql, Collections.emptyMap());
    }

    private String render(String sql, Map<String, Object> attributes) {
        StatementContext ctx = StatementContextAccess.createContext();
        attributes.forEach(ctx::define);

        return templateEngine.render(sql, ctx);
    }

    @Test
    public void testNewlinesOkay() throws Exception {
        ParsedSql parsed = parser.parse("select * from something\n where id = #id", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from something\n where id = ?");
    }

    @Test
    public void testOddCharacters() throws Exception {
        ParsedSql parsed = parser.parse("~* #boo '#nope' _%&^& *@ #id", ctx);
        assertThat(parsed.getSql()).isEqualTo("~* ? '#nope' _%&^& *@ ?");
    }

    @Test
    public void testNumbers() throws Exception {
        ParsedSql parsed = parser.parse("#bo0 '#nope' _%&^& *@ #id", ctx);
        assertThat(parsed.getSql()).isEqualTo("? '#nope' _%&^& *@ ?");
    }

    @Test
    public void testDollarSignOkay() throws Exception {
        ParsedSql parsed = parser.parse("select * from v$session", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from v$session");
    }

    @Test
    public void testColonIsLiteral() throws Exception {
        ParsedSql parsed = parser.parse("select * from foo where id = :id", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from foo where id = :id");
    }

    @Test
    public void testBacktickOkay() throws Exception {
        ParsedSql parsed = parser.parse("select * from `v$session", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from `v$session");
    }

    @Test
    public void testBailsOutOnInvalidInput() throws Exception {
        assertThatThrownBy(() -> parser.parse("select * from something\n where id = #\u0087\u008e\u0092\u0097\u009c", ctx))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testSubstitutesDefinedAttributes() throws Exception {
        String sql = "select foo from bar where foo = #someValue";
        ParsedSql parsed = parser.parse(sql, ctx);
        assertThat(parsed.getSql()).isEqualTo("select foo from bar where foo = ?");
    }

    @Test
    public void testCommentQuote() throws Exception {
        String sql = "select 1 /* ' \" <foo> */";
        assertThat(parser.parse(sql, ctx).getSql()).isEqualTo(sql);
    }

    @Test
    public void testEscapedQuestionMark() throws Exception {
        String sql = "SELECT '{\"a\":1, \"b\":2}'::jsonb ?? #key";
        ParsedSql parsed = parser.parse(sql, ctx);

        assertThat(parsed).isEqualTo(ParsedSql.builder()
            .append("SELECT '{\"a\":1, \"b\":2}'::jsonb ?? ")
            .appendNamedParameter("key")
            .build());
    }
}
