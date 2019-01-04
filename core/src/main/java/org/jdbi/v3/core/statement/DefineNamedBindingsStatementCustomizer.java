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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jdbi.v3.core.argument.Argument;

import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

class DefineNamedBindingsStatementCustomizer implements StatementCustomizer {
    private final DefineNamedBindingMode mode;

    DefineNamedBindingsStatementCustomizer(DefineNamedBindingMode mode) {
        this.mode = mode;
    }

    @Override
    public void beforeTemplating(PreparedStatement stmt, StatementContext ctx) throws SQLException {
        final Set<String> alreadyDefined = ctx.getAttributes().keySet();
        final Binding binding = ctx.getBinding();
        binding.getNames().stream()
            .filter(name -> !alreadyDefined.contains(name))
            .flatMap(name -> namedArg(name, binding.findForName(name, ctx)))
            .forEach(na -> mode.apply(na.arg)
                .ifPresent(a -> ctx.define(na.name, a)));
    }

    private Stream<NamedArg> namedArg(String name, Optional<Argument> arg) {
        return toStream(arg.map(a -> new NamedArg(name, a)));
    }

    static class NamedArg {
        final String name;
        final Argument arg;

        NamedArg(String name, Argument arg) {
            this.name = name;
            this.arg = arg;
        }
    }
}
