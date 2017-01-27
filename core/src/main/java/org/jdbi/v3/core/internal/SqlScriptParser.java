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
package org.jdbi.v3.core.internal;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;
import org.jdbi.v3.core.internal.lexer.SqlScriptLexer;

/**
 * An SQL script parser.
 *
 * <p>It performs lexical analysis of a script and generates events for semicolons.
 * As a result it returns a script without comments and newlines.</p>
 */
public class SqlScriptParser {

    private final TokenHandler semicolonHandler;

    public SqlScriptParser(TokenHandler semicolonHandler) {
        this.semicolonHandler = semicolonHandler;
    }

    public String parse(CharStream charStream) {
        StringBuilder sb = new StringBuilder();
        SqlScriptLexer lexer = new SqlScriptLexer(charStream);
        boolean endOfFile = false;
        while (!endOfFile) {
            Token t = lexer.nextToken();
            switch (t.getType()) {
                case Token.EOF:
                    endOfFile = true;
                    break;
                case SqlScriptLexer.SEMICOLON:
                    semicolonHandler.handle(t, sb);
                    break;
                case SqlScriptLexer.COMMENT:
                case SqlScriptLexer.MULTI_LINE_COMMENT:
                    break;
                case SqlScriptLexer.NEWLINES:
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    break;
                case SqlScriptLexer.QUOTED_TEXT:
                case SqlScriptLexer.LITERAL:
                case SqlScriptLexer.OTHER:
                    sb.append(t.getText());
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognizable token " + t);
            }
        }
        return sb.toString();
    }

    public interface TokenHandler {
        void handle(Token t, StringBuilder sb);
    }
}
