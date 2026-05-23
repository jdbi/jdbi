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
package org.jdbi.v3.stringtemplate4.internal;

import java.lang.annotation.Annotation;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.TemplateEngine;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.internal.SqlAnnotations;
import org.jdbi.v3.sqlobject.locator.SqlLocator;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import static java.lang.String.format;

import static org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator.findStringTemplateGroup;

public class UseStringTemplateSqlLocatorImpl extends SimpleExtensionConfigurer {

    private final SqlLocator locator;
    private final TemplateEngine templateEngine;

    public UseStringTemplateSqlLocatorImpl(Annotation annotation, Class<?> sqlObjectType) {
        final STGroup group = findStringTemplateGroup(sqlObjectType);

        this.locator = (type, method, config) -> {
            String templateName = SqlAnnotations.getAnnotationValue(method).orElseGet(method::getName);
            if (!group.isDefined(templateName)) {
                throw new IllegalStateException(format("No StringTemplate group %s for class %s", templateName, sqlObjectType));
            }
            return templateName;
        };

        this.templateEngine = (templateName, ctx) -> {
            final ST template = group.getInstanceOf(templateName);
            ctx.getAttributes().forEach(template::add);
            return template.render();
        };
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        config.get(SqlObjects.class).setSqlLocator(locator);
        config.get(SqlStatements.class).setTemplateEngine(templateEngine);
    }
}
