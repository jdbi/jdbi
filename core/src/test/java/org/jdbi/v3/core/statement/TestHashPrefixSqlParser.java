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

public class TestHashPrefixSqlParser {
    private SqlParser parser;
    private StatementContext ctx;

    @Before
    public void setUp() {
        this.parser = new HashPrefixSqlParser();
        ctx = StatementContextAccess.createContext();
    }

    @Test
    public void testNewlinesOkay() {
        ParsedSql parsed = parser.parse("select * from something\n where id = #id", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from something\n where id = ?");
    }

    @Test
    public void testOddCharacters() {
        ParsedSql parsed = parser.parse("~* #boo '#nope' _%&^& *@ #id", ctx);
        assertThat(parsed.getSql()).isEqualTo("~* ? '#nope' _%&^& *@ ?");
    }

    @Test
    public void testNumbers() {
        ParsedSql parsed = parser.parse("#bo0 '#nope' _%&^& *@ #id", ctx);
        assertThat(parsed.getSql()).isEqualTo("? '#nope' _%&^& *@ ?");
    }

    @Test
    public void testDollarSignOkay() {
        ParsedSql parsed = parser.parse("select * from v$session", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from v$session");
    }

    @Test
    public void testColonIsLiteral() {
        ParsedSql parsed = parser.parse("select * from foo where id = :id", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from foo where id = :id");
    }

    @Test
    public void testBacktickOkay() {
        ParsedSql parsed = parser.parse("select * from `v$session", ctx);
        assertThat(parsed.getSql()).isEqualTo("select * from `v$session");
    }

    @Test
    public void testNonLatinNamedParameter() {
        String sql = "select * from something\n where id = #\u0087\u008e\u0092\u0097\u009c";
        assertThat(parser.parse(sql, ctx))
            .isEqualTo(ParsedSql.builder().append("select * from something\n where id = ")
                .appendNamedParameter("\u0087\u008e\u0092\u0097\u009c")
                .build());
    }

    @Test
    public void testCachesRewrittenStatements() {
        String sql = "insert into something (id, name) values (#id, #name)";
        ParsedSql parsed = parser.parse(sql, ctx);
        assertThat(parsed).isSameAs(parser.parse(sql, ctx));
    }

    @Test
    public void testSubstitutesDefinedAttributes() {
        String sql = "select foo from bar where foo = #someValue";
        ParsedSql parsed = parser.parse(sql, ctx);
        assertThat(parsed.getSql()).isEqualTo("select foo from bar where foo = ?");
    }

    @Test
    public void testCommentQuote() {
        String sql = "select 1 /* ' \" <foo> */";
        assertThat(parser.parse(sql, ctx).getSql()).isEqualTo(sql);
    }

    @Test
    public void testEscapedQuestionMark() {
        String sql = "SELECT '{\"a\":1, \"b\":2}'::jsonb ?? #key";
        ParsedSql parsed = parser.parse(sql, ctx);

        assertThat(parsed).isEqualTo(ParsedSql.builder()
            .append("SELECT '{\"a\":1, \"b\":2}'::jsonb ?? ")
            .appendNamedParameter("key")
            .build());
    }

    @Test
    public void testKoreanDatabaseObjectNamesAreLiterals() {
        String sql = "SELECT 제목 FROM 업무_게시물";

        assertThat(parser.parse(sql, ctx))
            .isEqualTo(ParsedSql.builder().append(sql).build());
    }

    @Test
    public void testKoreanParameterName() {
        String sql = "SELECT #제목";

        assertThat(parser.parse(sql, ctx))
            .isEqualTo(ParsedSql.builder()
                .append("SELECT ")
                .appendNamedParameter("제목")
                .build());
    }

    @Test
    public void testEmojiParameterNames() {
        assertThat(parser.parse("insert into something (id, name) values (#😱, #😂)", ctx))
            .isEqualTo(ParsedSql.builder()
                .append("insert into something (id, name) values (")
                .appendNamedParameter("😱")
                .append(", ")
                .appendNamedParameter("😂")
                .append(")")
                .build());
    }
}
