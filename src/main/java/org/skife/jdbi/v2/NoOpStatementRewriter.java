/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.RewrittenStatement;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A statement rewriter which does not, in fact, rewrite anything. This is useful
 * if you use something like Oracle which supports :foo based parameter indicators
 * natively. It does not do any name based binding, however.
 */
public class NoOpStatementRewriter implements StatementRewriter
{
    public RewrittenStatement rewrite(String sql, Binding params, StatementContext ctx)
    {
        return new NoOpRewrittenStatement(sql, ctx);
    }

    private static class NoOpRewrittenStatement implements RewrittenStatement
    {
        private final String sql;
        private final StatementContext context;

        public NoOpRewrittenStatement(String sql, StatementContext ctx)
        {
            this.context = ctx;
            this.sql = sql;
        }

        public void bind(Binding params, PreparedStatement statement) throws SQLException
        {
            for (int i = 0; ; i++) {
                final Argument s = params.forPosition(i);
                if (s == null) { break; }
                s.apply(i + 1, statement, this.context);
            }
        }

        public String getSql()
        {
            return sql;
        }
    }
}
