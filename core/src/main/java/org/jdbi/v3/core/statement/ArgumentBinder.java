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
package org.jdbi.v3.core.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.rewriter.ParsedParameters;

class ArgumentBinder {
    static void bind(ParsedParameters parameters, Binding binding, PreparedStatement statement, StatementContext context) {
        List<String> parameterNames = parameters.getParameterNames();

        if (parameters.isPositional()) {
            bindPositional(parameterNames.size(), binding, statement, context);
        } else {
            bindNamed(parameterNames, binding, statement, context);
        }
    }

    private static void bindPositional(int size, Binding binding, PreparedStatement statement, StatementContext context) {
        for (int i = 0; i < size; i++) {
            try {
                Argument argument = binding.findForPosition(i).orElse(null);
                if (argument != null) {
                    argument.apply(i + 1, statement, context);
                }
                // any missing positional parameters could be return parameters
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException(
                        "Exception while binding positional param at (0 based) position " + i, e, context);
            }
        }
    }

    private static void bindNamed(List<String> parameterNames, Binding binding, PreparedStatement statement, StatementContext context) {
        for (int i = 0; i < parameterNames.size(); i++) {
            String param = parameterNames.get(i);

            try {
                binding.findForName(param, context)
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
}
