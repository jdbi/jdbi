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
package org.jdbi.v3.freemarker.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.TemplateEngine;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.internal.SqlAnnotations;
import org.jdbi.v3.sqlobject.locator.SqlLocator;

import static org.jdbi.v3.freemarker.FreemarkerSqlLocator.findTemplate;

public class UseFreemarkerSqlLocatorImpl implements Configurer {
    @Override
    public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
        SqlLocator locator = (type, method, config) -> {
            String templateName = SqlAnnotations.getAnnotationValue(method).orElseGet(method::getName);
            findTemplate(sqlObjectType, templateName);
            return templateName;
        };
        TemplateEngine templateEngine = (templateName, ctx) -> {
            Template template = findTemplate(sqlObjectType, templateName);
            StringWriter writer = new StringWriter();
            try {
                template.process(ctx.getAttributes(), writer);
                return writer.toString();
            } catch (TemplateException | IOException e) {
                throw new IllegalStateException("Failed to render template " + templateName, e);
            }
        };
        registry.get(SqlObjects.class).setSqlLocator(locator);
        registry.get(SqlStatements.class).setTemplateEngine(templateEngine);
    }

    @Override
    public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method) {
        configureForType(registry, annotation, sqlObjectType);
    }
}
