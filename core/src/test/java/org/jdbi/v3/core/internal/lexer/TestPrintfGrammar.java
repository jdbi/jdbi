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

import static org.jdbi.v3.core.internal.lexer.FormatterStatementLexer.*;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Lexer;
import org.junit.Test;

public class TestPrintfGrammar extends GrammarTestCase {
    @Test
    public void testFoo() throws Exception {
        expect("select id from something where name like '%d' and id = %d and name like %s",
               LITERAL, QUOTED_TEXT, LITERAL, INTEGER, LITERAL, STRING, EOF);
    }

    @Override
    protected String nameOf(int type) {
        switch (type) {
            case QUOTED_TEXT:
                return "QUOTED_TEXT";
            case INTEGER:
                return "INTEGER";
            case STRING:
                return "STRING";
            case LITERAL:
                return "LITERAL";
            case EOF:
                return "EOF";
        }
        return "unknown";
    }


    @Override
    protected Lexer createLexer(String s) {
        return new FormatterStatementLexer(new ANTLRStringStream(s));
    }
}
