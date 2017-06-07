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

import static org.jdbi.v3.core.rewriter.ParsedStatement.POSITIONAL_PARAM;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.statement.StatementContext;
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
        if (stmt.isPositional()) {
            bindPositional(params, statement);
        } else {
            bindNamed(params, statement);
        }
    }

    private void bindPositional(Binding params, PreparedStatement statement) {
        for (int i = 0; i < stmt.getParams().size(); i++) {
            try {
                Optional<Argument> argument = params.findForPosition(i);
                if (argument.isPresent()) {
                    argument.get().apply(i + 1, statement, context);
                }
                // any missing positional parameters could be return parameters
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException(
                        "Exception while binding positional param at (0 based) position " + i, e, context);
            }
        }
    }

    private void bindNamed(Binding params, PreparedStatement statement) {
        List<String> paramList = stmt.getParams();

        if (paramList.contains(POSITIONAL_PARAM)) {
            throw new UnableToExecuteStatementException(
                    "Cannot mix named and positional parameters in a SQL statement: " + paramList,
                    context);
        }

        for (int i = 0; i < paramList.size(); i++) {
            String param = paramList.get(i);

            try {
                params.findForName(param, context)
                        .orElseThrow(() -> new UnableToExecuteStatementException(
                                String.format("Unable to execute, no named parameter matches '%s'.", param),
                                context))
                        .apply(i + 1, statement, context);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException(
                        String.format("Exception while binding named parameter '%s'", param), e, context);
            }
        }
    }

    @Override
    public String getSql() {
        return stmt.getParsedSql();
    }
}
