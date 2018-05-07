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
package org.jdbi.v3.stringsubstitutor;

import org.apache.commons.text.StringSubstitutor;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.TemplateEngine;

/**
 * Register an instance of this interface ({@link org.jdbi.v3.core.statement.SqlStatements#setTemplateEngine}) to use an Apache Commons Text {@link StringSubstitutor} as a {@link TemplateEngine}. This lets you use any pair of strings as variable delimiters, enabling the use of syntax like <pre>select * from ${foo}</pre>, <pre>select * from &lt;foo&gt;</pre>, <pre>select * from %foo%</pre>, etc.
 */
@FunctionalInterface
public interface StringSubstitutorTemplateEngine extends TemplateEngine {
    /**
     * Convenience constant that uses a {@link StringSubstitutor} set to defaults. At the time of writing, that means <pre>${foo}</pre> syntax.
     */
    StringSubstitutorTemplateEngine DEFAULTS = substitutor -> {};

    /**
     * Customize the given {@link StringSubstitutor} instance to set your preferred prefix, suffix, escape character, and perhaps other configuration. The instance is created by Jdbi, and is not shared nor re-used.
     */
    void customize(StringSubstitutor substitutor);

    @Override
    default String render(String template, StatementContext ctx) {
        StringSubstitutor substitutor = new StringSubstitutor(ctx.getAttributes());
        customize(substitutor);
        return substitutor.replace(template);
    }
}
