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
import org.jdbi.core.internal.lexer.HashStatementLexer;
import org.jdbi.core.statement.internal.ErrorListener;
import org.jdbi.meta.Beta;

import static org.antlr.v4.runtime.Recognizer.EOF;
import static org.jdbi.core.internal.lexer.HashStatementLexer.COMMENT;
import static org.jdbi.core.internal.lexer.HashStatementLexer.DOUBLE_QUOTED_TEXT;
import static org.jdbi.core.internal.lexer.HashStatementLexer.ESCAPED_TEXT;
import static org.jdbi.core.internal.lexer.HashStatementLexer.LITERAL;
import static org.jdbi.core.internal.lexer.HashStatementLexer.NAMED_PARAM;
import static org.jdbi.core.internal.lexer.HashStatementLexer.POSITIONAL_PARAM;
import static org.jdbi.core.internal.lexer.HashStatementLexer.QUOTED_TEXT;

/**
 * SQL parser which recognizes named parameter tokens of the form
 * <code>#tokenName</code>.
 */
public class HashPrefixSqlParser extends CachingSqlParser {

    public HashPrefixSqlParser() {}

    @Beta
    public HashPrefixSqlParser(final JdbiCacheBuilder cacheBuilder) {
        super(cacheBuilder);
    }

    @Override
    public String nameParameter(final String rawName, final StatementContext ctx) {
        return "#" + rawName;
    }

    @Override
    ParsedSql internalParse(final String sql) {
        final ParsedSql.Builder parsedSql = ParsedSql.builder();
        final HashStatementLexer lexer = new HashStatementLexer(CharStreams.fromString(sql));
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
