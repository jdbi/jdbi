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
package org.jdbi.v3.core.statement.internal;

import java.util.HashSet;
import java.util.Set;

import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.TemplateEngine;

public class DefineBindsTemplateEngine implements TemplateEngine {
    private final TemplateEngine delegate;

    public DefineBindsTemplateEngine(TemplateEngine delegate) {
        this.delegate = delegate;
    }

    @Override
    public String render(String template, StatementContext ctx) {
        final Set<String> binds = new HashSet<>(ctx.getBinding().getNames());
        binds.removeAll(ctx.getAttributes().keySet());
        for (String boundButNotDefined : binds) {
            ctx.getBinding().findForName(boundButNotDefined, ctx)
                .map(a -> a instanceof NullArgument ? null : a)
                .ifPresent(a -> ctx.define(boundButNotDefined, a));
        }
        return delegate.render(template, ctx);
    }
}
