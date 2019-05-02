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
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.internal.lexer.ColonStatementLexer;
import org.jdbi.v3.core.statement.internal.ErrorListener;

import static org.antlr.v4.runtime.Recognizer.EOF;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.COMMENT;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.DOUBLE_QUOTED_TEXT;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.ESCAPED_TEXT;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.LITERAL;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.NAMED_PARAM;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.POSITIONAL_PARAM;
import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.QUOTED_TEXT;

/**
 * SQL parser which recognizes named parameter tokens of the form
 * <code>:tokenName</code>
 * <p>
 * This is the default SQL parser
 * </p>
 */
public class ColonPrefixSqlParser implements SqlParser {
    private static final String CACHE_PREFIX = "ColonPrefixSqlParser:";

    private static final JdbiCache<String, ParsedSql> PARSED_SQL_CACHE =
        JdbiCaches.declare(rawSql -> CACHE_PREFIX + rawSql, ColonPrefixSqlParser::internalParse);

    @Override
    public ParsedSql parse(String sql, StatementContext ctx) {
        return PARSED_SQL_CACHE.get(sql, ctx);
    }

    @Override
    public String nameParameter(String rawName, StatementContext ctx) {
        return ":" + rawName;
    }

    private static ParsedSql internalParse(String sql) throws IllegalArgumentException {
        ParsedSql.Builder parsedSql = ParsedSql.builder();
        ColonStatementLexer lexer = new ColonStatementLexer(CharStreams.fromString(sql));
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
