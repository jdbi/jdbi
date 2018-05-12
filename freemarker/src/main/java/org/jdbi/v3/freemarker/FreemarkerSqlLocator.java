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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * Locates SQL in {@code .sql.ftl} Freemarker files on the classpath.
 */
public class FreemarkerSqlLocator {
    private static final Map<String, Template> CACHE = ExpiringMap.builder()
            .expiration(10, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .build();

    private FreemarkerSqlLocator() {}

    public static Template findTemplate(Class<?> type, String templateName) {
        String path = getPath(type);

        return CACHE.computeIfAbsent(path + "#" + templateName, k -> {
            try {
                Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
                ClassTemplateLoader ctl1 = new ClassTemplateLoader(type, "/");
                ClassTemplateLoader ctl2 = new ClassTemplateLoader(type, "/" + path);
                MultiTemplateLoader mtl = new MultiTemplateLoader(new TemplateLoader[] {ctl1, ctl2});
                configuration.setTemplateLoader(mtl);
                return configuration.getTemplate(templateName + ".sql.ftl");
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load Freemarker template " + templateName + " in " + path, e);
            }

        });
    }

    private static String getPath(Class<?> type) {
        return type.getName().replace(".", "/");
    }
}
