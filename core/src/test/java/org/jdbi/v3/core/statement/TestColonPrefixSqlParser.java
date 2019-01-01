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
import static org.mockito.Mockito.spy;

public class TestColonPrefixSqlParser {
    private SqlParser parser;
    private StatementContext ctx;

    @Before
    public void setUp() {
        parser = new ColonPrefixSqlParser();
        ctx = mock(StatementContext.class);
    }

    @Test
    public void testNewlinesOkay() {
        ParsedSql parsed = parser.parse("select * from something\n where id = :id", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from something\n where id = ?");
    }

    @Test
    public void testOddCharacters() {
        ParsedSql parsed = parser.parse("~* :boo ':nope' _%&^& *@ :id", ctx);
        assertThat(parsed.getSql()).isEqualTo("~* ? ':nope' _%&^& *@ ?");
    }

    @Test
    public void testNumbers() {
        ParsedSql parsed = parser.parse(":bo0 ':nope' _%&^& *@ :id", ctx);
        assertThat(parsed.getSql()).isEqualTo("? ':nope' _%&^& *@ ?");
    }

    @Test
    public void testDollarSignOkay() {
        ParsedSql parsed = parser.parse("select * from v$session", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from v$session");
    }

    @Test
    public void testHashInColumnNameOkay() {
        ParsedSql parsed = parser.parse("select column# from thetable where id = :id", ctx);
       assertThat(parsed.getSql()).isEqualTo("select column# from thetable where id = ?");
    }

    @Test
    public void testBacktickOkay() {
        ParsedSql parsed = parser.parse("select * from `v$session", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from `v$session");
    }

    @Test
    public void testDoubleColon() {
        final String doubleColon = "select 1::int";
        ParsedSql parsed = parser.parse(doubleColon, ctx);
        assertThat(parsed.getSql()).isEqualTo(doubleColon);
    }

    @Test
    public void testBailsOutOnInvalidInput() {
        assertThatThrownBy(() -> parser.parse("select * from something\n where id = :\u0087\u008e\u0092\u0097\u009c", ctx).getSql())
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testSubstitutesDefinedAttributes() {
        String sql = "select foo from bar where foo = :someValue";
        ParsedSql parsed = parser.parse(sql, ctx);
        assertThat(parsed.getSql()).isEqualTo("select foo from bar where foo = ?");
    }

    @Test
    public void testCachesRewrittenStatements() {
        parser = spy(parser);

        String sql = "insert into something (id, name) values (:id, :name)";
        ParsedSql parsed = parser.parse(sql, ctx);
        assertThat(parsed).isSameAs(parser.parse(sql, ctx));
    }

    @Test
    public void testEscapedQuestionMark() {
        String sql = "SELECT '{\"a\":1, \"b\":2}'::jsonb ?? :key";
        ParsedSql parsed = parser.parse(sql, ctx);

        assertThat(parsed).isEqualTo(ParsedSql.builder()
            .append("SELECT '{\"a\":1, \"b\":2}'::jsonb ?? ")
            .appendNamedParameter("key")
            .build());
    }

    @Test
    public void testParentheses() {
        ParsedSql parsed = parser.parse("select #:(var)cast", ctx);
        assertThat(parsed.getSql()).isEqualTo("select #?cast");
        assertThat(parsed.getParameters().getParameterNames()).containsExactly("var");
    }
}
