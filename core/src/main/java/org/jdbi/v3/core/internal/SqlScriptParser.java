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

import java.util.ArrayList;
import java.util.List;

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

    private final TokenHandler tokenHandler;

    public SqlScriptParser(TokenHandler tokenHandler) {
        this.tokenHandler = tokenHandler;
    }

    public String parse(CharStream charStream) {
        StringBuilder sb = new StringBuilder();
        SqlScriptLexer lexer = new SqlScriptLexer(charStream);
        lexer.addErrorListener(new ErrorListener());
        boolean endOfFile = false;
        while (!endOfFile) {
            Token t = lexer.nextToken();
            switch (t.getType()) {
                // EOF ends parsing
                case Token.EOF:
                    endOfFile = true;
                    break;
                // Strip comments out
                case COMMENT:
                case MULTI_LINE_COMMENT:
                    break;
                // collapse newlines
                case NEWLINES:
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    break;
                // everything else is done by the handler
                case SEMICOLON:
                case BLOCK_BEGIN:
                case BLOCK_END:
                case QUOTED_TEXT:
                case LITERAL:
                case OTHER:
                    tokenHandler.handle(t, sb);
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

    public static final class ScriptTokenHandler implements TokenHandler {

        private final List<String> statements = new ArrayList<>();
        private Token lastToken = null;

        private int blockLevel = 0;

        @Override
        public void handle(Token t, StringBuilder sb) {
            switch (t.getType()) {
                case BLOCK_BEGIN:
                    blockLevel++;
                    sb.append(t.getText());
                    break;
                case BLOCK_END:
                    if (blockLevel == 0) {
                        throw new IllegalStateException("Found END in SQL script without BEGIN!");
                    }

                    blockLevel--;
                    sb.append(t.getText());
                    break;
                case SEMICOLON:
                    if (blockLevel == 0) {
                        if (lastToken != null && lastToken.getType() == BLOCK_END) {
                            sb.append(t.getText());
                        }
                        addStatement(sb.toString());
                        sb.setLength(0);
                    } else {
                        sb.append(t.getText());
                    }
                    break;
                default:
                    sb.append(t.getText());
            }
            lastToken = t;
        }

        public List<String> getStatements() {
            return statements;
        }

        public List<String> addStatement(String statement) {
            String trimmedStatement = statement.trim();
            if (!trimmedStatement.isEmpty()) {
                statements.add(trimmedStatement);
            }
            return statements;
        }
    }
}
