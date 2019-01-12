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
import java.util.Set;

import org.jdbi.v3.core.argument.NullArgument;

class DefineNamedBindingsStatementCustomizer implements StatementCustomizer {
    @Override
    public void beforeTemplating(PreparedStatement stmt, StatementContext ctx) throws SQLException {
        final Set<String> alreadyDefined = ctx.getAttributes().keySet();
        final Binding binding = ctx.getBinding();
        binding.getNames().stream()
            .filter(name -> !alreadyDefined.contains(name))
            .forEach(name -> binding.findForName(name, ctx).ifPresent(
                    a -> ctx.define(name, a instanceof NullArgument ? false : true)));
    }
}
