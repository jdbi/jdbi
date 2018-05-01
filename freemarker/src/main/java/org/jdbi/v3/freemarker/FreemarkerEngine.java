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
package org.jdbi.v3.freemarker;

import org.jdbi.v3.core.statement.TemplateEngine;

import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Rewrites a Freemarker template, using the attributes on the {@link StatementContext} as template parameters.
 */
public class FreemarkerEngine implements TemplateEngine {
    @Override
    public String render(String sql, StatementContext ctx) {
        try {
            Template template = new Template(null, sql, null);
            StringWriter writer = new StringWriter();
            template.process(ctx.getAttributes(), writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Failed to process template: " + sql, e);
        }

    }
}
