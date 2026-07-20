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

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import org.jdbi.core.config.JdbiConfig;

public final class FreemarkerConfig implements JdbiConfig<FreemarkerConfig> {

    private final Configuration freemarkerConfiguration;

    public FreemarkerConfig() {
        this(defaultConfiguration());
    }

    private FreemarkerConfig(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

    private static Configuration defaultConfiguration() {
        final Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setTemplateLoader(new ClassTemplateLoader(selectClassLoader(), "/"));
        configuration.setNumberFormat("computer");
        return configuration;
    }

    @CheckReturnValue
    public FreemarkerConfig freemarkerConfiguration(Configuration freemarkerConfiguration) {
        return new FreemarkerConfig(freemarkerConfiguration);
    }

    public Configuration getFreemarkerConfiguration() {
        return freemarkerConfiguration;
    }

    public FreemarkerSqlLocator createLocator() {
        return new FreemarkerSqlLocator(this);
    }


    private static ClassLoader selectClassLoader() {
        return Optional.ofNullable(Thread.currentThread().getContextClassLoader())
            .orElseGet(FreemarkerConfig.class::getClassLoader);
    }
}
