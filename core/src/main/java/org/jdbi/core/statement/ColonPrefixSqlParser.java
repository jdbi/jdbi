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
package org.jdbi.core.statement;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.jdbi.core.cache.JdbiCacheBuilder;
import org.jdbi.core.internal.lexer.ColonStatementLexer;
import org.jdbi.core.statement.internal.ErrorListener;
import org.jdbi.meta.Beta;

import static org.antlr.v4.runtime.Recognizer.EOF;
import static org.jdbi.core.internal.lexer.ColonStatementLexer.COMMENT;
import static org.jdbi.core.internal.lexer.ColonStatementLexer.DOUBLE_QUOTED_TEXT;
import static org.jdbi.core.internal.lexer.ColonStatementLexer.ESCAPED_TEXT;
import static org.jdbi.core.internal.lexer.ColonStatementLexer.LITERAL;
import static org.jdbi.core.internal.lexer.ColonStatementLexer.NAMED_PARAM;
import static org.jdbi.core.internal.lexer.ColonStatementLexer.POSITIONAL_PARAM;
import static org.jdbi.core.internal.lexer.ColonStatementLexer.QUOTED_TEXT;

/**
 * SQL parser which recognizes named parameter tokens of the form
 * <code>:tokenName</code>
 * <p>
 * This is the default SQL parser
 * </p>
 */
public class ColonPrefixSqlParser extends CachingSqlParser {

    public ColonPrefixSqlParser() {}

    @Beta
    public ColonPrefixSqlParser(final JdbiCacheBuilder cacheBuilder) {
        super(cacheBuilder);
    }

    @Override
    public String nameParameter(final String rawName, final StatementContext ctx) {
        return ":" + rawName;
    }

    @Override
    ParsedSql internalParse(final String sql) {
        final ParsedSql.Builder parsedSql = ParsedSql.builder();
        final ColonStatementLexer lexer = new ColonStatementLexer(CharStreams.fromString(sql));
        lexer.addErrorListener(new ErrorListener());
        Token t = lexer.nextToken();
        while (t.getType() != EOF) {
            switch (t.getType()) {
                case COMMENT:
                case LITERAL:
                case QUOTED_TEXT:
                case DOUBLE_QUOTED_TEXT:
                    parsedSql.append(t.getText());
                    break;
                case NAMED_PARAM:
                    parsedSql.appendNamedParameter(t.getText().substring(1));
                    break;
                case POSITIONAL_PARAM:
                    parsedSql.appendPositionalParameter();
                    break;
                case ESCAPED_TEXT:
                    parsedSql.append(t.getText().substring(1));
                    break;
                default:
                    break;
            }
            t = lexer.nextToken();
        }
        return parsedSql.build();
    }
}
