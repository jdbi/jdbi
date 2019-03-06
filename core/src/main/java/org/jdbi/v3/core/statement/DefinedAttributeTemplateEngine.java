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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.jdbi.v3.core.internal.lexer.DefineStatementLexer;
import org.jdbi.v3.core.statement.internal.ErrorListener;

import static org.antlr.v4.runtime.Recognizer.EOF;
import static org.jdbi.v3.core.internal.lexer.DefineStatementLexer.COMMENT;
import static org.jdbi.v3.core.internal.lexer.DefineStatementLexer.DEFINE;
import static org.jdbi.v3.core.internal.lexer.DefineStatementLexer.DOUBLE_QUOTED_TEXT;
import static org.jdbi.v3.core.internal.lexer.DefineStatementLexer.ESCAPED_TEXT;
import static org.jdbi.v3.core.internal.lexer.DefineStatementLexer.LITERAL;
import static org.jdbi.v3.core.internal.lexer.DefineStatementLexer.QUOTED_TEXT;

/**
 * Template engine which replaces angle-bracketed tokens like
 * <code>&lt;name&gt;</code> with the string value of the named attribute.
 * Attribute names may contain letters (a-z, A-Z), digits (0-9), or underscores
 * (<code>_</code>).
 */
public class DefinedAttributeTemplateEngine implements TemplateEngine {
    @Override
    public String render(String template, StatementContext ctx) {
        StringBuilder b = new StringBuilder();
        DefineStatementLexer lexer = new DefineStatementLexer(CharStreams.fromString(template));
        lexer.addErrorListener(new ErrorListener());
        try {
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
                            throw new UnableToCreateStatementException("Undefined attribute for token '" + text + "'", ctx);
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
        } catch (RuntimeException e) {
            throw new UnableToCreateStatementException("Error rendering SQL template: '" + template + "'", e, ctx);
        }
    }
}
