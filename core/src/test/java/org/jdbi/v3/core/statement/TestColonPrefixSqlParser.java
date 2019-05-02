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

public class TestColonPrefixSqlParser {
    private SqlParser parser;
    private StatementContext ctx;

    @Before
    public void setUp() {
        parser = new ColonPrefixSqlParser();
        ctx = StatementContextAccess.createContext();
    }

    @Test
    public void testNewlinesOkay() {
        assertThat(parser.parse("select * from something\n where id = :id", ctx))
            .isEqualTo(ParsedSql.builder()
                .append("select * from something\n where id = ")
                .appendNamedParameter("id")
                .build());
    }

    @Test
    public void testEmptyQuotes() {
        String sql = "select ''";
        assertThat(parser.parse(sql, ctx))
            .isEqualTo(ParsedSql.builder().append(sql).build());
    }

    @Test
    public void testEscapedQuoteInQuotes() {
        String sql = "select '\\''";
        assertThat(parser.parse(sql, ctx))
            .isEqualTo(ParsedSql.builder().append(sql).build());
    }

    @Test
    public void testEscapedColon() {
        assertThat(parser.parse("select \\:foo", ctx))
            .isEqualTo(ParsedSql.builder().append("select :foo").build());
    }

    @Test
    public void testMixedNamedAndPositionalParameters() {
        assertThatThrownBy(() -> parser.parse("select :foo, ?", ctx))
            .isInstanceOf(UnableToExecuteStatementException.class)
            .hasMessageContaining("Cannot mix named and positional parameters");
    }

    @Test
    public void testThisBrokeATest() {
        assertThat(parser.parse("insert into something (id, name) values (:id, :name)", ctx))
            .isEqualTo(ParsedSql.builder()
                .append("insert into something (id, name) values (")
                .appendNamedParameter("id")
                .append(", ")
                .appendNamedParameter("name")
                .append(")")
                .build());
    }

    @Test
    public void testExclamationWorks() {
        String sql = "select 1 != 2 from dual";
        assertThat(parser.parse(sql, ctx))
            .isEqualTo(ParsedSql.builder().append(sql).build());
    }

    @Test
    public void testHashInColumnNameWorks() {
        assertThat(parser.parse("select col# from something where id = :id", ctx))
            .isEqualTo(ParsedSql.builder()
                .append("select col# from something where id = ")
                .appendNamedParameter("id")
                .build());
    }

    @Test
    public void testOddCharacters() {
        assertThat(parser.parse("~* :boo ':nope' _%&^& *@ :id", ctx))
            .isEqualTo(ParsedSql.builder()
                .append("~* ")
                .appendNamedParameter("boo")
                .append(" ':nope' _%&^& *@ ")
                .appendNamedParameter("id")
                .build());
    }

    @Test
    public void testNumbers() {
        assertThat(parser.parse(":bo0 ':nope' _%&^& *@ :id", ctx))
            .isEqualTo(ParsedSql.builder()
                .appendNamedParameter("bo0")
                .append(" ':nope' _%&^& *@ ")
                .appendNamedParameter("id")
                .build());
    }

    @Test
    public void testDollarSignOkay() {
        String sql = "select * from v$session";
        assertThat(parser.parse(sql, ctx))
            .isEqualTo(ParsedSql.builder().append(sql).build());
    }

    @Test
    public void testHashInColumnNameOkay() {
        assertThat(parser.parse("select column# from thetable where id = :id", ctx))
            .isEqualTo(ParsedSql.builder()
                .append("select column# from thetable where id = ")
                .appendNamedParameter("id")
                .build());
    }

    @Test
    public void testBacktickOkay() {
        String sql = "select * from `v$session";
        assertThat(parser.parse(sql, ctx))
            .isEqualTo(ParsedSql.builder().append(sql).build());
    }

    @Test
    public void testDoubleColon() {
        final String doubleColon = "select 1::int";
        assertThat(parser.parse(doubleColon, ctx))
            .isEqualTo(ParsedSql.builder().append(doubleColon).build());
    }

    @Test
    public void testNonLatinParameterName() {
        assertThat(parser.parse("select * from something\n where id = :\u0087\u008e\u0092\u0097\u009c", ctx))
            .describedAs("Colon followed by non-ID characters (by Java rules) is treated as a literal instead of a named parameter")
            .isEqualTo(ParsedSql.builder()
                .append("select * from something\n where id = ")
                .appendNamedParameter("\u0087\u008e\u0092\u0097\u009c")
                .build());
    }

    @Test
    public void testCachesRewrittenStatements() {
        String sql = "insert into something (id, name) values (:id, :name)";
        ParsedSql parsed = parser.parse(sql, ctx);
        assertThat(parsed).isSameAs(parser.parse(sql, ctx));
    }

    @Test
    public void testEscapedQuestionMark() {
        assertThat(parser.parse("SELECT '{\"a\":1, \"b\":2}'::jsonb ?? :key", ctx))
            .isEqualTo(ParsedSql.builder()
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
        assertThat(parser.parse("SELECT :제목", ctx))
            .isEqualTo(ParsedSql.builder()
                .append("SELECT ")
                .appendNamedParameter("제목")
                .build());
    }

    @Test
    public void testEmojiParameterNames() {
        assertThat(parser.parse("insert into something (id, name) values (:😱, :😂)", ctx))
            .isEqualTo(ParsedSql.builder()
                .append("insert into something (id, name) values (")
                .appendNamedParameter("😱")
                .append(", ")
                .appendNamedParameter("😂")
                .append(")")
                .build());
    }
}
