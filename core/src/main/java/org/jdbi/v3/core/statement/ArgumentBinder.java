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
        // best effort: just try +1 (unless we expose a method to get the full binding count)
        boolean moreArgumentsProvidedThanDeclared = binding.findForPosition(params.getParameterCount()).isPresent();
        if (moreArgumentsProvidedThanDeclared && !context.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException("Superfluous positional param at (0 based) position " + params.getParameterCount(), context);
        }

        for (int i = 0; i < params.getParameterCount(); i++) {
            final int index = i;
            try {
                binding.findForPosition(i)
                    .orElseThrow(() -> new UnableToCreateStatementException("Missing positional param at (0 based) position " + index, context))
                    .apply(i + 1, statement, context);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException("Exception while binding positional param at (0 based) position " + i, e, context);
            }
        }
    }

    private static void bindNamed(ParsedParameters params, Binding binding, PreparedStatement statement, StatementContext context) {
        List<String> paramNames = params.getParameterNames();

        // best effort: compare empty to non-empty because we can't list the individual binding names (unless we expose a method to do so)
        boolean argumentsProvidedButNoneDeclared = paramNames.isEmpty() && !binding.isEmpty();
        if (argumentsProvidedButNoneDeclared && !context.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException(String.format("Superfluous named parameters provided while the query declares none: '%s'.", binding), context);
        }

        for (int i = 0; i < paramNames.size(); i++) {
            final String name = paramNames.get(i);

            try {
                binding.findForName(name, context)
                    .orElseThrow(() -> new UnableToCreateStatementException(String.format("Missing named parameter '%s'.", name), context))
                    .apply(i + 1, statement, context);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException(String.format("Exception while binding named parameter '%s'", name), e, context);
            }
        }
    }
}
