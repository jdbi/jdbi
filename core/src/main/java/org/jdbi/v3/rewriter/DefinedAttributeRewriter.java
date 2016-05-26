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
package org.jdbi.v3.rewriter;

import static org.jdbi.v3.internal.lexer.DefineStatementLexer.COMMENT;
import static org.jdbi.v3.internal.lexer.DefineStatementLexer.DEFINE;
import static org.jdbi.v3.internal.lexer.DefineStatementLexer.DOUBLE_QUOTED_TEXT;
import static org.jdbi.v3.internal.lexer.DefineStatementLexer.EOF;
import static org.jdbi.v3.internal.lexer.DefineStatementLexer.ESCAPED_TEXT;
import static org.jdbi.v3.internal.lexer.DefineStatementLexer.LITERAL;
import static org.jdbi.v3.internal.lexer.DefineStatementLexer.QUOTED_TEXT;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.internal.lexer.DefineStatementLexer;

class DefinedAttributeRewriter {
    static String rewriteDefines(String sql, StatementContext ctx) {
        StringBuilder b = new StringBuilder();
        DefineStatementLexer lexer = new DefineStatementLexer(new ANTLRStringStream(sql));
        Token t = lexer.nextToken();
        while (t.getType() != EOF) {
            switch (t.getType()) {
                case COMMENT:
                case LITERAL:
                case QUOTED_TEXT:
                case DOUBLE_QUOTED_TEXT:
                    b.append(t.getText());
                    break;
                case DEFINE:
                    String text = t.getText();
                    String key = text.substring(1, text.length() - 1);
                    Object value = ctx.getAttribute(key);
                    if (value == null) {
                        throw new IllegalArgumentException("Undefined attribute for token '" + text + "'");
                    }
                    b.append(value);
                    break;
                case ESCAPED_TEXT:
                    b.append(t.getText().substring(1));
                    break;
                default:
                    break;
            }
            t = lexer.nextToken();
        }
        return b.toString();
    }
}
