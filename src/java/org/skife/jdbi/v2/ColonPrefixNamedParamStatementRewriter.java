package org.skife.jdbi.v2;

import antlr.Token;
import antlr.TokenStreamException;
import org.skife.jdbi.rewriter.colon.ColonStatementLexer;
import static org.skife.jdbi.rewriter.colon.ColonStatementLexerTokenTypes.*;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.RewrittenStatement;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.Argument;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * Statement rewriter which replaces named parameter tokens of the form :tokenName
 * <p>
 * This is the default statement rewriter
 */
public class ColonPrefixNamedParamStatementRewriter implements StatementRewriter
{
    public RewrittenStatement rewrite(String sql, Binding params)
    {
        StringBuilder b = new StringBuilder();
        ParsedStatement stmt = new ParsedStatement();
        ColonStatementLexer lexer = new ColonStatementLexer(new StringReader(sql));
        try {
            Token t = lexer.nextToken();
            while (t.getType() != EOF) {
                switch (t.getType()) {
                    case LITERAL:
                        b.append(t.getText());
                        break;
                    case NAMED_PARAM:
                        stmt.addNamedParamAt(t.getText().substring(1, t.getText().length()));
                        b.append("?");
                        break;
                    case QUOTED_TEXT:
                        b.append(t.getText());
                        break;
                    case DOUBLE_QUOTED_TEXT:
                        b.append(t.getText());
                        break;
                    case POSITIONAAL_PARAM:
                        b.append("?");
                        stmt.addPositionalParamAt();
                        break;
                }
                t = lexer.nextToken();
            }
        }
        catch (TokenStreamException e) {
            throw new UnableToCreateStatementException("Exception parsing for named parameter replacement", e);
        }

        return new MyRewrittenStatement(b.toString(), stmt);
    }

    private static class MyRewrittenStatement implements RewrittenStatement
    {
        private final String sql;
        private final ParsedStatement stmt;

        public MyRewrittenStatement(String sql, ParsedStatement stmt)
        {
            this.sql = sql;
            this.stmt = stmt;
        }

        public void bind(Binding params, PreparedStatement statement) throws SQLException
        {
            if (stmt.positionalOnly) {
                // no named params, is easy
                boolean finished = false;
                for (int i = 0; !finished; ++i)
                {
                    final Argument a = params.forPosition(i);
                    if (a != null)
                    {
                        a.apply(i + 1, statement);
                    }
                    else
                    {
                        finished = true;
                    }
                }
            }
            else {
                //List<String> named_params = stmt.params;
                int i = 0;
                for (String named_param : stmt.params)
                {
                    if ("*".equals(named_param)) continue;
                    Argument a = params.forName(named_param);
                    if (a == null)
                    {
                        a = params.forPosition(i);
                    }

                    if (a == null)
                    {
                        String msg = String.format("Unable to execute, no named parameter matches " +
                                                   "\"%s\" and no positional param for place %d (which is %d in " +
                                                   "the JDBC 'start at 1' scheme) has been set.",
                                                   named_param, i, i + 1);
                        throw new UnableToExecuteStatementException(msg);
                    }

                    a.apply(i + 1, statement);
                    i++;
                }
            }
        }

        public String getSql()
        {
            return sql;
        }
    }

    private static class ParsedStatement {

        private boolean positionalOnly = true;
        private List<String> params = new ArrayList<String>();

        public void addNamedParamAt(String name)
        {
            positionalOnly = false;
            params.add(name);
        }

        public void addPositionalParamAt()
        {
            params.add("*");
        }
    }
}
