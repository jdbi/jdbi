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
package org.jdbi.freemarker;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.TemplateEngine;

/**
 * Rewrites a Freemarker template, using the attributes on the {@link StatementContext} as template parameters.
 */
public class FreemarkerEngine implements TemplateEngine.Parsing {
    private static final FreemarkerEngine INSTANCE = new FreemarkerEngine();

    /**
     * @deprecated use {@link #instance()} for a shared engine instead
     */
    @Deprecated(since = "3.39.0", forRemoval = true)
    @SuppressFBWarnings("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR")
    public FreemarkerEngine() {}

    public static FreemarkerEngine instance() {
        return INSTANCE;
    }

    @Override
    public Optional<Function<StatementContext, String>> parse(String sqlTemplate, ConfigRegistry config) {
        try {
            Template template = new Template(null, sqlTemplate,
                    config.get(FreemarkerConfig.class).getFreemarkerConfiguration());
            return Optional.of(ctx -> {
                try {
                    StringWriter writer = new StringWriter();
                    template.process(ctx.getAttributes(), writer);
                    return writer.toString();
                } catch (IOException | TemplateException e) {
                    throw new IllegalStateException("Failed to render template: " + sqlTemplate, e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to process template: " + sqlTemplate, e);
        }
    }
}
