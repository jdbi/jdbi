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
package org.jdbi.v3.commonstext;

import java.util.function.Consumer;
import org.apache.commons.text.StringSubstitutor;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.TemplateEngine;

/**
 * Register an instance of this class ({@link org.jdbi.v3.core.statement.SqlStatements#setTemplateEngine}) to use an Apache Commons Text {@link StringSubstitutor} as a {@link TemplateEngine}. This lets you use any pair of strings as variable delimiters, enabling the use of syntax like <pre>select * from ${foo}</pre>, <pre>select * from &lt;foo&gt;</pre>, <pre>select * from %foo%</pre>, etc.
 */
public class StringSubstitutorTemplateEngine implements TemplateEngine {
    static final String DEFAULT_PREFIX = "${";
    static final String DEFAULT_SUFFIX = "}";
    static final char DEFAULT_ESCAPE = '\\';

    private final Consumer<StringSubstitutor> customizer;

    /**
     * Default is <pre>${foo}</pre> syntax.
     */
    public StringSubstitutorTemplateEngine() {
        this(substitutor -> {
            substitutor.setVariablePrefix(DEFAULT_PREFIX);
            substitutor.setVariableSuffix(DEFAULT_SUFFIX);
            substitutor.setEscapeChar(DEFAULT_ESCAPE);
        });
    }

    /**
     * Customize the given {@link StringSubstitutor} instance to set your preferred prefix, suffix, escape character, and perhaps other configuration. The instance is created by Jdbi, and is not shared nor re-used. Your customizer function however will be re-used for all instances.
     */
    public StringSubstitutorTemplateEngine(Consumer<StringSubstitutor> customizer) {
        this.customizer = customizer;
    }

    @Override
    public String render(String template, StatementContext ctx) {
        StringSubstitutor substitutor = new StringSubstitutor(ctx.getAttributes());
        customizer.accept(substitutor);
        return substitutor.replace(template);
    }

    /**
     * @deprecated use the default constructor instead
     */
    @Deprecated
    public static StringSubstitutorTemplateEngine defaults() {
        return new StringSubstitutorTemplateEngine();
    }

    /**
     * @deprecated use the constructor instead
     */
    @Deprecated
    public static StringSubstitutorTemplateEngine withCustomizer(Consumer<StringSubstitutor> customizer) {
        return new StringSubstitutorTemplateEngine(customizer);
    }

    public static StringSubstitutorTemplateEngine between(char prefix, char suffix) {
        return new StringSubstitutorTemplateEngine(substitutor -> {
            substitutor.setVariablePrefix(prefix);
            substitutor.setVariableSuffix(suffix);
        });
    }

    public static StringSubstitutorTemplateEngine between(String prefix, String suffix) {
        return new StringSubstitutorTemplateEngine(substitutor -> {
            substitutor.setVariablePrefix(prefix);
            substitutor.setVariableSuffix(suffix);
        });
    }

    public static StringSubstitutorTemplateEngine between(char prefix, char suffix, char escape) {
        return new StringSubstitutorTemplateEngine(substitutor -> {
            substitutor.setVariablePrefix(prefix);
            substitutor.setVariableSuffix(suffix);
            substitutor.setEscapeChar(escape);
        });
    }

    public static StringSubstitutorTemplateEngine between(String prefix, String suffix, char escape) {
        return new StringSubstitutorTemplateEngine(substitutor -> {
            substitutor.setVariablePrefix(prefix);
            substitutor.setVariableSuffix(suffix);
            substitutor.setEscapeChar(escape);
        });
    }
}
