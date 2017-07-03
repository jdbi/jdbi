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

import static org.jdbi.v3.core.internal.lexer.ColonStatementLexer.*; // SUPPRESS CHECKSTYLE WARNING
import static org.jdbi.v3.core.rewriter.DefinedAttributeRewriter.rewriteDefines;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.jdbi.v3.core.internal.lexer.ColonStatementLexer;
import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * Statement rewriter which replaces named parameter tokens of the form <code>:tokenName</code>
 * <p>
 * This is the default statement rewriter
 * </p>
 */
public class ColonPrefixStatementRewriter implements StatementRewriter {
    private final Map<String, ParsedStatement> cache = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public RewrittenStatement rewrite(String sql, Binding params, StatementContext ctx) {
        try {
            ParsedStatement stmt = cache.computeIfAbsent(rewriteDefines(sql, ctx), this::rewriteNamedParameters);
            return new InternalRewrittenStatement(stmt, ctx);
        } catch (IllegalArgumentException e) {
            throw new UnableToCreateStatementException("Exception parsing for named parameter replacement", e, ctx);
        }
    }

    ParsedStatement rewriteNamedParameters(String sql) throws IllegalArgumentException {
        ParsedStatement stmt = new ParsedStatement();
        StringBuilder b = new StringBuilder(sql.length());
        ColonStatementLexer lexer = new ColonStatementLexer(new ANTLRStringStream(sql));
        Token t = lexer.nextToken();
        while (t.getType() != EOF) {
            switch (t.getType()) {
                case COMMENT:
                case LITERAL:
                case QUOTED_TEXT:
                case DOUBLE_QUOTED_TEXT:
                    b.append(t.getText());
                    break;
                case NAMED_PARAM:
                    stmt.addNamedParam(t.getText().substring(1));
                    b.append("?");
                    break;
                case POSITIONAL_PARAM:
                    b.append("?");
                    stmt.addPositionalParam();
                    break;
                case ESCAPED_TEXT:
                    b.append(t.getText().substring(1));
                    break;
                default:
                    break;
            }
            t = lexer.nextToken();
        }
        stmt.setParsedSql(b.toString());
        return stmt;
    }
}
