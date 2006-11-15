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
