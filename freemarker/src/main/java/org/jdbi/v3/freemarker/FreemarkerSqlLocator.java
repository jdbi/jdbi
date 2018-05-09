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

import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.net.URL;
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

    private static File findTemplateDirectory(Class<?> type) {
        try {
            String classFolder = getPath(type);
            URL resource = type.getClassLoader().getResource(classFolder);
            if (resource != null) {
                return new File(resource.toURI());
            }
        } catch (URISyntaxException ignored) { }
        return null;
    }

    public static Template findTemplate(Class<?> type, String templateName) {
        File templateDirectory = findTemplateDirectory(type);
        if (templateDirectory == null) {
            throw new IllegalStateException("No template directory found for class " + type);
        }
        File templateFile = new File(templateDirectory, templateName + ".sql.ftl");
        return CACHE.computeIfAbsent(templateFile.getPath(), (p) -> {
            Exception ex;
            try {
                if (templateFile.exists()) {
                    Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
                    ClassTemplateLoader ctl1 = new ClassTemplateLoader(type, "/");
                    ClassTemplateLoader ctl2 = new ClassTemplateLoader(type, "/" + getPath(type));
                    MultiTemplateLoader mtl = new MultiTemplateLoader(new TemplateLoader[] {ctl1, ctl2});
                    configuration.setTemplateLoader(mtl);
                    return new Template(templateName, new FileReader(templateFile), configuration);
                }
                ex = new IllegalArgumentException("Template file " + templateFile.getPath() + " does not exist");
            } catch (Exception templateLoadingException) {
                ex = templateLoadingException;
            }
            throw new IllegalStateException("Failed to load Freemarker template " + templateName + " in " + templateDirectory.getAbsolutePath(), ex);
        });
    }

    private static String getPath(Class<?> type) {
        return type.getName().replace(".", "/");
    }
}
