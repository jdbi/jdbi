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

/**
 * Locates SQL in {@code .sql.ftl} Freemarker files on the classpath.
 */
public class FreemarkerSqlLocator {
    private static final Configuration CONFIGURATION;

    private FreemarkerSqlLocator() {}

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

    public static Template findTemplate(Class<?> type, String templateName) {
        String path = type.getName().replace(".", "/") + "/" + templateName + ".sql.ftl";

        try {
            return CONFIGURATION.getTemplate(path);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Freemarker template " + templateName + " in " + path, e);
        }
    }

}
