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
package org.jdbi.v3.core.internal.lexer;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Lexer;
import org.junit.Test;

import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.EOF;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.ESCAPED_TEXT;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.LITERAL;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.NAMED_PARAM;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.POSITIONAL_PARAM;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.QUOTED_TEXT;

public class TestColonGrammar extends GrammarTestCase {
    @Test
    public void testNamedOnly() throws Exception {
        expect("select id from something where name like ':foo' and id = :id and name like :name",
               LITERAL, QUOTED_TEXT, LITERAL, NAMED_PARAM, LITERAL, NAMED_PARAM, EOF);
    }

    @Test
    public void testEmptyQuote() throws Exception {
        expect("select ''",
               LITERAL, QUOTED_TEXT, EOF);
    }

    @Test
    public void testEscapedEmptyQuote() throws Exception {
        expect("select '\\''",
               LITERAL, QUOTED_TEXT, EOF);
    }

    @Test
    public void testEscapedColon() throws Exception {
        expect("insert into foo (val) VALUE (:bar\\:\\:type)",
               LITERAL, NAMED_PARAM, ESCAPED_TEXT, ESCAPED_TEXT, LITERAL, EOF);
    }

    @Test
    public void testMixed() throws Exception {
        expect("select id from something where name like ':foo' and id = ? and name like :name",
               LITERAL, QUOTED_TEXT, LITERAL, POSITIONAL_PARAM, LITERAL, NAMED_PARAM, EOF);
    }

    @Test
    public void testThisBrokeATest() throws Exception {
        expect("insert into something (id, name) values (:id, :name)",
               LITERAL, NAMED_PARAM, LITERAL, NAMED_PARAM, LITERAL, EOF);
    }

    @Test
    public void testExclamationWorks() throws Exception {
        expect("select1 != 2 from dual", LITERAL, EOF);
    }

    @Test
    public void testHashInColumnNameWorks() throws Exception {
        expect("select col# from something where id = :id", LITERAL, NAMED_PARAM, EOF);
    }

    @Override
    protected String nameOf(int type) {
        switch (type) {
            case LITERAL:
                return "LITERAL";
            case QUOTED_TEXT:
                return "QUOTED_TEXT";
            case NAMED_PARAM:
                return "NAMED_PARAM";
            case EOF:
                return "EOF";
            default:
                break;
        }
        return String.valueOf(type);
    }

    @Override
    protected Lexer createLexer(String s) {
        return new ColonStatementLexer(new ANTLRStringStream(s));
    }
}
