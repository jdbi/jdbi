/*
 * Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.unstable.grammar;

import static org.skife.jdbi.rewriter.printf.FormattedStatementLexerTokenTypes.LITERAL;
import static org.skife.jdbi.rewriter.printf.FormattedStatementLexerTokenTypes.QUOTED_TEXT;
import static org.skife.jdbi.rewriter.printf.FormattedStatementLexerTokenTypes.INTEGER;
import static org.skife.jdbi.rewriter.printf.FormattedStatementLexerTokenTypes.STRING;
import static org.skife.jdbi.rewriter.printf.FormattedStatementLexerTokenTypes.EOF;
import org.skife.jdbi.rewriter.printf.FormattedStatementLexer;
import antlr.CharScanner;

import java.io.Reader;

/**
 *
 */
public class TestPrintfGrammar extends GrammarTestCase
{

    public void testFoo() throws Exception
    {
        expect("select id from something where name like '%d' and id = %d and name like %s",
               LITERAL, QUOTED_TEXT, LITERAL, INTEGER, LITERAL, STRING, EOF);
    }

    protected String nameOf(int type)
    {
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


    protected CharScanner createLexer(Reader r)
    {
        return new FormattedStatementLexer(r);
    }
}
