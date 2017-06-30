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

import static org.jdbi.v3.core.internal.lexer.HashStatementLexer.*; // SUPPRESS CHECKSTYLE WARNING

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.internal.lexer.HashStatementLexer;

/**
 * Statement rewriter which replaces named parameter tokens of the form #tokenName
 */
public class HashPrefixStatementParser implements StatementParser {
    private final Map<String, ParsedStatement> cache = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public ParsedStatement parse(String sql) {
        try {
            return cache.computeIfAbsent(sql, this::internalParse);
        } catch (IllegalArgumentException e) {
            throw new UnableToCreateStatementException("Exception parsing for named parameter replacement", e);
        }
    }

    ParsedStatement internalParse(final String sql) {
        ParsedStatement.Builder stmt = ParsedStatement.builder();
        HashStatementLexer lexer = new HashStatementLexer(new ANTLRStringStream(sql));
        Token t = lexer.nextToken();
        while (t.getType() != EOF) {
            switch (t.getType()) {
                case COMMENT:
                case LITERAL:
                case QUOTED_TEXT:
                case DOUBLE_QUOTED_TEXT:
                    stmt.append(t.getText());
                    break;
                case NAMED_PARAM:
                    stmt.appendNamedParameter(t.getText().substring(1));
                    break;
                case POSITIONAL_PARAM:
                    stmt.appendPositionalParameter();
                    break;
                case ESCAPED_TEXT:
                    stmt.append(t.getText().substring(1));
                    break;
                default:
                    break;
            }
            t = lexer.nextToken();
        }
        return stmt.build();
    }
}
