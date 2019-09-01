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

import java.util.Optional;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.jdbi.v3.core.locator.internal.ClasspathBuilder;

/**
 * Locates SQL in {@code .sql.ftl} Freemarker files on the classpath.
 */
public class FreemarkerSqlLocator {
    /**
     * @deprecated don't use static scope
     */
    @Deprecated
    private static final Configuration CONFIGURATION;

    private final FreemarkerConfig config;

    FreemarkerSqlLocator(FreemarkerConfig config) {
        this.config = config;
    }

    static {
        Configuration c = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        c.setTemplateLoader(new ClassTemplateLoader(selectClassLoader(), "/"));
        c.setNumberFormat("computer");
        CONFIGURATION = c;
    }

    private static ClassLoader selectClassLoader() {
        return Optional.ofNullable(Thread.currentThread().getContextClassLoader())
            .orElseGet(FreemarkerSqlLocator.class::getClassLoader);
    }

    /**
     * @deprecated this static method does not respect configuration, use {@link FreemarkerConfig#createLocator()}
     */
    @Deprecated
    public static Template findTemplate(Class<?> type, String templateName) {
        String path = new ClasspathBuilder()
            .appendFullyQualifiedClassName(type)
            .appendVerbatim(templateName)
            .setExtension("sql.ftl")
            .build();

        try {
            return CONFIGURATION.getTemplate(path);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Freemarker template " + templateName + " in " + path, e);
        }
    }

    public Template locate(Class<?> type, String templateName) {
        String path = new ClasspathBuilder()
            .appendFullyQualifiedClassName(type)
            .appendVerbatim(templateName)
            .setExtension("sql.ftl")
            .build();

        try {
            return config.getFreemarkerConfiguration().getTemplate(path);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Freemarker template " + templateName + " in " + path, e);
        }
    }
}
