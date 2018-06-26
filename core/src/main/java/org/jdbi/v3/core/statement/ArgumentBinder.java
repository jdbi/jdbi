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

import java.util.Optional;
import org.jdbi.v3.core.argument.Argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

class ArgumentBinder {
    private ArgumentBinder() {
        throw new UnsupportedOperationException("utility class");
    }

    static void bind(ParsedParameters parameters, Binding binding, PreparedStatement statement, StatementContext context) {
        if (parameters.isPositional()) {
            bindPositional(parameters, binding, statement, context);
        } else {
            bindNamed(parameters, binding, statement, context);
        }
    }

    private static void bindPositional(ParsedParameters params, Binding binding, PreparedStatement statement, StatementContext context) {
        for (int i = 0; i < params.getParameterCount(); i++) {
            Optional<Argument> argument = binding.findForPosition(i);

            if (argument.isPresent()) {
                try {
                    argument.get().apply(i + 1, statement, context);
                } catch (SQLException e) {
                    throw new UnableToCreateStatementException("Exception while binding positional param at (0 based) position " + i, e, context);
                }
            }
        }
    }

    private static void bindNamed(ParsedParameters params, Binding binding, PreparedStatement statement, StatementContext context) {
        List<String> parameterNames = params.getParameterNames();
        if (parameterNames.isEmpty() && !binding.isEmpty() && !context.getConfig(SqlStatements.class).getAllowUnusedBindings()) {
            throw new UnableToCreateStatementException(String.format("Unable to execute. The query doesn't have named parameters, but provided binding '%s'.", binding), context);
        }

        for (int i = 0; i < parameterNames.size(); i++) {
            String param = parameterNames.get(i);

            try {
                binding.findForName(param, context)
                        .orElseThrow(() -> new UnableToCreateStatementException(String.format("Unable to execute, no named parameter matches '%s'.", param), context))
                        .apply(i + 1, statement, context);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException(String.format("Exception while binding named parameter '%s'", param), e, context);
            }
        }
    }
}
