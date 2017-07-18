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
package org.jdbi.v3.core.rewriter;

import static org.jdbi.v3.core.internal.lexer.HashStatementLexer.COMMENT;
import static org.jdbi.v3.core.internal.lexer.HashStatementLexer.DOUBLE_QUOTED_TEXT;
import static org.jdbi.v3.core.internal.lexer.HashStatementLexer.EOF;
import static org.jdbi.v3.core.internal.lexer.HashStatementLexer.ESCAPED_TEXT;
import static org.jdbi.v3.core.internal.lexer.HashStatementLexer.LITERAL;
import static org.jdbi.v3.core.internal.lexer.HashStatementLexer.NAMED_PARAM;
import static org.jdbi.v3.core.internal.lexer.HashStatementLexer.POSITIONAL_PARAM;
import static org.jdbi.v3.core.internal.lexer.HashStatementLexer.QUOTED_TEXT;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.jdbi.v3.core.internal.lexer.HashStatementLexer;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * SQL parser which recognizes named parameter tokens of the form
 * <code>#tokenName</code>.
 */
public class HashPrefixSqlParser implements SqlParser {
    private final Map<String, ParsedSql> cache = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public ParsedSql parse(String sql, StatementContext ctx) {
        try {
            return cache.computeIfAbsent(sql, this::internalParse);
        } catch (IllegalArgumentException e) {
            throw new UnableToCreateStatementException("Exception parsing for named parameter replacement", e, ctx);
        }
    }

    private ParsedSql internalParse(final String sql) {
        ParsedSql.Builder parsedSql = ParsedSql.builder();
        HashStatementLexer lexer = new HashStatementLexer(new ANTLRStringStream(sql));
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
