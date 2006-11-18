package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.RewrittenStatement;
import org.skife.jdbi.v2.tweak.Argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A statement rewriter which does not, in fact, rewrite anything. This is useful
 * if you use something like Oracle which supports :foo based parameter indicators
 * natively. It does not do any name based binding, however.
 */
public class NoOpStatementRewriter implements StatementRewriter
{
    public RewrittenStatement rewrite(String sql, Binding params)
    {
        return new NoOpRewrittenStatement(sql);
    }

    private static class NoOpRewrittenStatement implements RewrittenStatement
    {
        private final String sql;

        public NoOpRewrittenStatement(String sql)
        {

            this.sql = sql;
        }

        public void bind(Binding params, PreparedStatement statement) throws SQLException
        {
            for (int i = 0; ; i++) {
                final Argument s = params.forPosition(i);
                if (s == null) { break; }
                s.apply(i + 1, statement);
            }
        }

        public String getSql()
        {
            return sql;
        }
    }
}
