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

import java.util.function.BiFunction;
import org.apache.commons.text.StringSubstitutor;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.TemplateEngine;

@FunctionalInterface
public interface StringSubstitutorTemplateEngine extends TemplateEngine {
    StringSubstitutorTemplateEngine DEFAULTS = substitutor -> {};

    BiFunction<String, String, StringSubstitutorTemplateEngine> BETWEEN = (prefix, suffix) -> substitutor -> {
        substitutor.setVariablePrefix(prefix);
        substitutor.setVariableSuffix(suffix);
    };

    void customize(StringSubstitutor substitutor);

    @Override
    default String render(String template, StatementContext ctx) {
        StringSubstitutor substitutor = new StringSubstitutor(ctx.getAttributes());
        customize(substitutor);
        return substitutor.replace(template);
    }
}
