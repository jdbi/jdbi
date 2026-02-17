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
package org.jdbi.freemarker.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.SimpleExtensionConfigurer;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.TemplateEngine;
import org.jdbi.freemarker.FreemarkerConfig;
import org.jdbi.sqlobject.SqlObjects;
import org.jdbi.sqlobject.internal.SqlAnnotations;
import org.jdbi.sqlobject.locator.SqlLocator;

import static org.jdbi.freemarker.FreemarkerSqlLocator.findTemplate;

public class UseFreemarkerSqlLocatorImpl extends SimpleExtensionConfigurer {

    private final SqlLocator locator;
    private final TemplateEngine templateEngine;

    public UseFreemarkerSqlLocatorImpl(Annotation annotation, Class<?> sqlObjectType) {
        this.locator = (type, method, config) ->
            SqlAnnotations.getAnnotationValue(method).orElseGet(method::getName);

        this.templateEngine = (templateName, ctx) -> {
            Template template = findTemplate(ctx.getConfig(FreemarkerConfig.class).getFreemarkerConfiguration(), sqlObjectType, templateName);

            try (StringWriter writer = new StringWriter()) {
                template.process(ctx.getAttributes(), writer);
                return writer.toString();
            } catch (TemplateException | IOException e) {
                throw new IllegalStateException("Failed to render template " + templateName, e);
            }
        };
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        config.get(SqlObjects.class).setSqlLocator(locator);
        config.get(SqlStatements.class).setTemplateEngine(templateEngine);
    }
}
