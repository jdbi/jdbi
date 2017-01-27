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

import static org.jdbi.v3.core.internal.JdbiOptionals.findFirstPresent;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

class InternalRewrittenStatement implements RewrittenStatement {
    private final ParsedStatement stmt;
    private final StatementContext context;

    InternalRewrittenStatement(ParsedStatement stmt, StatementContext ctx) {
        this.context = ctx;
        this.stmt = stmt;
    }

    @Override
    public void bind(Binding params, PreparedStatement statement) throws SQLException {
        if (stmt.positionalOnly) {
            // no named params, is easy
            boolean finished = false;
            for (int i = 0; !finished; ++i) {
                final Optional<Argument> a = params.findForPosition(i);
                if (a.isPresent()) {
                    try {
                        a.get().apply(i + 1, statement, this.context);
                    } catch (SQLException e) {
                        throw new UnableToExecuteStatementException(
                                String.format("Exception while binding positional param at (0 based) position %d",
                                        i), e, context);
                    }
                } else {
                    finished = true;
                }
            }
        } else {
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
                } catch (SQLException e) {
                    throw new UnableToCreateStatementException(String.format("Exception while binding '%s'",
                            named_param), e, context);
                }
                i++;
            }
        }
    }

    @Override
    public String getSql() {
        return stmt.getParsedSql();
    }
}
