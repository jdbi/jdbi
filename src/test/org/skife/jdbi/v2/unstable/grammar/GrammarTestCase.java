package org.skife.jdbi.v2.unstable.grammar;

import antlr.CharScanner;
import antlr.Token;
import junit.framework.TestCase;

import java.io.Reader;
import java.io.StringReader;

/**
 *
 */
public abstract class GrammarTestCase extends TestCase
{

    public void expect(String s, int... tokens) throws Exception
    {
        CharScanner lexer = createLexer(new StringReader(s));
        for (int token : tokens) {
            Token t = lexer.nextToken();
            assertEquals(String.format("Expected %s, got %s, with '%s'", nameOf(token), nameOf(t.getType()), t.getText()),
                         token, t.getType());
        }
    }

    protected abstract CharScanner createLexer(Reader r);

    protected abstract String nameOf(int type);
}
