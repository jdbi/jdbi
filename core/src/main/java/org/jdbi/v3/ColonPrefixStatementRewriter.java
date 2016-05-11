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
package org.jdbi.v3;

import static org.jdbi.v3.internal.JdbiOptionals.findFirstPresent;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.jdbi.rewriter.colon.ColonStatementLexer;
import org.jdbi.rewriter.define.DefineStatementLexer;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.exceptions.UnableToExecuteStatementException;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.RewrittenStatement;
import org.jdbi.v3.tweak.StatementRewriter;

/**
 * Statement rewriter which replaces named parameter tokens of the form :tokenName
 * <p/>
 * This is the default statement rewriter
 */
public class ColonPrefixStatementRewriter implements StatementRewriter
{
    private final Map<String, ParsedStatement> cache = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Munge up the SQL as desired. Responsible for figuring out ow to bind any
     * arguments in to the resultant prepared statement.
     *
     * @param sql The SQL to rewrite
     * @param params contains the arguments which have been bound to this statement.
     * @param ctx The statement context for the statement being executed
     * @return something which can provide the actual SQL to prepare a statement from
     *         and which can bind the correct arguments to that prepared statement
     */
    @Override
    public RewrittenStatement rewrite(String sql, Binding params, StatementContext ctx)
    {
        try {
            ParsedStatement stmt = cache.computeIfAbsent(rewriteDefines(sql, ctx), this::rewriteNamedParameters);
            return new MyRewrittenStatement(stmt, ctx);
        }
        catch (IllegalArgumentException e) {
            throw new UnableToCreateStatementException("Exception parsing for named parameter replacement", e, ctx);
        }
    }

    String rewriteDefines(String sql, StatementContext ctx)
    {
        StringBuilder b = new StringBuilder();
        DefineStatementLexer lexer = new DefineStatementLexer(new ANTLRStringStream(sql));
        Token t = lexer.nextToken();
        while (t.getType() != DefineStatementLexer.EOF) {
            switch (t.getType()) {
                case DefineStatementLexer.COMMENT:
                case DefineStatementLexer.LITERAL:
                case DefineStatementLexer.QUOTED_TEXT:
                case DefineStatementLexer.DOUBLE_QUOTED_TEXT:
                    b.append(t.getText());
                    break;
                case DefineStatementLexer.DEFINE:
                    String text = t.getText();
                    String key = text.substring(1, text.length() - 1);
                    Object value = ctx.getAttribute(key);
                    if (value == null) {
                        throw new IllegalArgumentException("Undefined attribute for token '" + text + "'");
                    }
                    b.append(value);
                    break;
                case DefineStatementLexer.ESCAPED_TEXT:
                    b.append(t.getText().substring(1));
                    break;
                default:
                    break;
            }
            t = lexer.nextToken();
        }
        return b.toString();
    }

    ParsedStatement rewriteNamedParameters(String sql) throws IllegalArgumentException
    {
        ParsedStatement stmt = new ParsedStatement();
        StringBuilder b = new StringBuilder();
        ColonStatementLexer lexer = new ColonStatementLexer(new ANTLRStringStream(sql));
        Token t = lexer.nextToken();
        while (t.getType() != ColonStatementLexer.EOF) {
            switch (t.getType()) {
            case ColonStatementLexer.COMMENT:
            case ColonStatementLexer.LITERAL:
            case ColonStatementLexer.QUOTED_TEXT:
            case ColonStatementLexer.DOUBLE_QUOTED_TEXT:
                b.append(t.getText());
                break;
            case ColonStatementLexer.NAMED_PARAM:
                stmt.addNamedParamAt(t.getText().substring(1));
                b.append("?");
                break;
            case ColonStatementLexer.POSITIONAL_PARAM:
                b.append("?");
                stmt.addPositionalParamAt();
                break;
            case ColonStatementLexer.ESCAPED_TEXT:
                b.append(t.getText().substring(1));
                break;
            default:
                break;
            }
            t = lexer.nextToken();
        }
        stmt.sql = b.toString();
        return stmt;
    }

    private static class MyRewrittenStatement implements RewrittenStatement
    {
        private final ParsedStatement stmt;
        private final StatementContext context;

        MyRewrittenStatement(ParsedStatement stmt, StatementContext ctx)
        {
            this.context = ctx;
            this.stmt = stmt;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void bind(Binding params, PreparedStatement statement) throws SQLException
        {
            if (stmt.positionalOnly) {
                // no named params, is easy
                boolean finished = false;
                for (int i = 0; !finished; ++i) {
                    final Optional<Argument> a = params.findForPosition(i);
                    if (a.isPresent()) {
                        try {
                        a.get().apply(i + 1, statement, this.context);
                        }
                        catch (SQLException e) {
                            throw new UnableToExecuteStatementException(
                                    String.format("Exception while binding positional param at (0 based) position %d",
                                                  i), e, context);
                        }
                    }
                    else {
                        finished = true;
                    }
                }
            }
            else {
                //List<String> named_params = stmt.params;
                int i = 0;
                for (String named_param : stmt.params) {
                    if ("*".equals(named_param)) {
                        continue;
                    }
                    final int index = i;
                    Argument a = findFirstPresent(
                            () -> params.findForName(named_param),
                            () -> params.findForPosition(index))
                            .orElseThrow(() -> {
                                String msg = String.format("Unable to execute, no named parameter matches " +
                                                "\"%s\" and no positional param for place %d (which is %d in " +
                                                "the JDBC 'start at 1' scheme) has been set.",
                                        named_param, index, index + 1);
                                return new UnableToExecuteStatementException(msg, context);
                            });

                    try {
                        a.apply(i + 1, statement, this.context);
                    }
                    catch (SQLException e) {
                        throw new UnableToCreateStatementException(String.format("Exception while binding '%s'",
                                                                                 named_param), e, context);
                    }
                    i++;
                }
            }
        }

        @Override
        public String getSql()
        {
            return stmt.getParsedSql();
        }
    }

    static class ParsedStatement
    {
        private String sql;
        private boolean positionalOnly = true;
        private final List<String> params = new ArrayList<>();

        public void addNamedParamAt(String name)
        {
            positionalOnly = false;
            params.add(name);
        }

        public void addPositionalParamAt()
        {
            params.add("*");
        }

        public String getParsedSql()
        {
            return sql;
        }
    }
}
