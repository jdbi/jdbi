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
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.meta.Beta;

/**
 * Renders an SQL statement from a template.
 *
 * Note for implementors: define a suitable public constructor for SqlObject's {@code UseTemplateEngine} annotation, and/or create your own custom annotation in case your {@link TemplateEngine} has configuration parameters! Suitable constructors are the no-arg constructor, one that takes a {@link java.lang.Class}, and one that takes both a {@link java.lang.Class} and a {@link java.lang.reflect.Method}.
 *
 * @see DefinedAttributeTemplateEngine
 */
@FunctionalInterface
@SuppressWarnings("FunctionalInterfaceMethodChanged") // this is a false positive b/c a lambda can only be casted to TemplateEngine or Parsing, not both
public interface TemplateEngine {
    /**
     * Convenience constant that returns the input template.
     */
    TemplateEngine NOP = (template, ctx) -> template;

    /**
     * Renders an SQL statement from the given template, using the statement
     * context as needed.
     *
     * @param template The SQL to rewrite
     * @param ctx      The statement context for the statement being executed
     * @return something which can provide the actual SQL to prepare a statement from
     * and which can bind the correct arguments to that prepared statement
     */
    String render(String template, StatementContext ctx);

    /**
     * Parse a SQL template and return a parsed representation ready to apply to a statement.
     * This allows the parsed representation to be cached and reused.
     * @param template the sql template to parse
     * @param config the Jdbi configuration at prepare time
     * @return a parsed representation, if available
     */
    @Beta
    default Optional<Function<StatementContext, String>> parse(String template, ConfigRegistry config) {
        return Optional.empty();
    }

    @FunctionalInterface
    @Beta
    interface Parsing extends TemplateEngine {
        @Override
        default String render(String template, StatementContext ctx) {
            return parse(template, ctx.getConfig())
                    .orElseThrow(() -> new UnableToCreateStatementException("Caching template engine did not prepare"))
                    .apply(ctx);
        }

        @Override
        Optional<Function<StatementContext, String>> parse(String template, ConfigRegistry config);
    }
}
