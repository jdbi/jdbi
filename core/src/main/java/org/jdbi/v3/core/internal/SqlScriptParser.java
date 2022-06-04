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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.jdbi.v3.core.internal.lexer.SqlScriptLexer;
import org.jdbi.v3.core.statement.internal.ErrorListener;

import static org.jdbi.v3.core.internal.lexer.SqlScriptLexer.BLOCK_BEGIN;
import static org.jdbi.v3.core.internal.lexer.SqlScriptLexer.BLOCK_END;
import static org.jdbi.v3.core.internal.lexer.SqlScriptLexer.COMMENT;
import static org.jdbi.v3.core.internal.lexer.SqlScriptLexer.LITERAL;
import static org.jdbi.v3.core.internal.lexer.SqlScriptLexer.MULTI_LINE_COMMENT;
import static org.jdbi.v3.core.internal.lexer.SqlScriptLexer.NEWLINES;
import static org.jdbi.v3.core.internal.lexer.SqlScriptLexer.OTHER;
import static org.jdbi.v3.core.internal.lexer.SqlScriptLexer.QUOTED_TEXT;
import static org.jdbi.v3.core.internal.lexer.SqlScriptLexer.SEMICOLON;

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
        lexer.addErrorListener(new ErrorListener());
        boolean endOfFile = false;
        int blockLevel = 0;
        while (!endOfFile) {
            Token t = lexer.nextToken();
            switch (t.getType()) {
                case Token.EOF:
                    endOfFile = true;
                    break;
                case SEMICOLON:
                    if (blockLevel == 0) {
                        semicolonHandler.handle(t, sb);
                    } else {
                        // preserve semicolons within begin/end block
                        sb.append(t.getText());
                    }
                    break;
                case BLOCK_BEGIN:
                case BLOCK_END:
                    blockLevel += BLOCK_BEGIN == t.getType() ? +1 : -1;
                    sb.append(t.getText());
                    break;
                case COMMENT:
                case MULTI_LINE_COMMENT:
                    break;
                case NEWLINES:
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    break;
                case QUOTED_TEXT:
                case LITERAL:
                case OTHER:
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
