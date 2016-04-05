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

import static org.jdbi.rewriter.emoji.EmojiStatementLexer.DOUBLE_QUOTED_TEXT;
import static org.jdbi.rewriter.emoji.EmojiStatementLexer.EMOJI_PARAM;
import static org.jdbi.rewriter.emoji.EmojiStatementLexer.ESCAPED_TEXT;
import static org.jdbi.rewriter.emoji.EmojiStatementLexer.LITERAL;
import static org.jdbi.rewriter.emoji.EmojiStatementLexer.POSITIONAL_PARAM;
import static org.jdbi.rewriter.emoji.EmojiStatementLexer.QUOTED_TEXT;
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
import org.jdbi.rewriter.emoji.EmojiStatementLexer;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.exceptions.UnableToExecuteStatementException;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.RewrittenStatement;
import org.jdbi.v3.tweak.StatementRewriter;

/**
 * Statement rewriter which replaces unicode emoji parameter tokens
 */
public class EmojiStatementRewriter implements StatementRewriter
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
        ParsedStatement stmt = cache.get(sql);
        if (stmt == null) {
            try {
                stmt = parseString(sql);
                cache.put(sql, stmt);
            }
            catch (IllegalArgumentException e) {
                throw new UnableToCreateStatementException("Exception parsing for emoji parameter replacement", e, ctx);
            }
        }
        return new MyRewrittenStatement(stmt, ctx);
    }

    ParsedStatement parseString(final String sql) throws IllegalArgumentException
    {
        ParsedStatement stmt = new ParsedStatement();
        StringBuilder b = new StringBuilder();
        EmojiStatementLexer lexer = new EmojiStatementLexer(new ANTLRStringStream(sql));
        Token t = lexer.nextToken();
        while (t.getType() != EmojiStatementLexer.EOF) {
            switch (t.getType()) {
            case LITERAL:
                b.append(t.getText());
                break;
            case EMOJI_PARAM:
                stmt.addEmojiParamAt(t.getText());
                b.append("?");
                break;
            case QUOTED_TEXT:
                b.append(t.getText());
                break;
            case DOUBLE_QUOTED_TEXT:
                b.append(t.getText());
                break;
            case POSITIONAL_PARAM:
                b.append("?");
                stmt.addPositionalParamAt();
                break;
            case ESCAPED_TEXT:
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
                // no emojis, is easy
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
                int i = 0;
                for (String emoji : stmt.params) {
                    if ("*".equals(emoji)) {
                        continue;
                    }
                    final int index = i;
                    Argument a = findFirstPresent(
                            () -> params.findForName(emoji),
                            () -> params.findForPosition(index))
                            .orElseThrow(() -> {
                                String msg = String.format("Unable to execute, no emoji parameter matches " +
                                                "\"%s\" and no positional param for place %d (which is %d in " +
                                                "the JDBC 'start at 1' scheme) has been set.",
                                        emoji, index, index + 1);
                                return new UnableToExecuteStatementException(msg, context);
                            });

                    try {
                        a.apply(i + 1, statement, this.context);
                    }
                    catch (SQLException e) {
                        throw new UnableToCreateStatementException(String.format("Exception while binding '%s'",
                                                                                 emoji), e, context);
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

        public void addEmojiParamAt(String param)
        {
            positionalOnly = false;
            params.add(param);
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
