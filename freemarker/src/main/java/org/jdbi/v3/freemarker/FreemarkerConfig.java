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
import org.jdbi.v3.core.config.JdbiConfig;

public class FreemarkerConfig implements JdbiConfig<FreemarkerConfig> {

    private Configuration freemarkerConfiguration;

    public FreemarkerConfig() {
        freemarkerConfiguration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        freemarkerConfiguration.setTemplateLoader(new ClassTemplateLoader(selectClassLoader(), "/"));
        freemarkerConfiguration.setNumberFormat("computer");
    }

    private FreemarkerConfig(FreemarkerConfig other) {
        this.freemarkerConfiguration = other.freemarkerConfiguration;
    }

    public FreemarkerConfig setFreemarkerConfiguration(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
        return this;
    }

    public Configuration getFreemarkerConfiguration() {
        return freemarkerConfiguration;
    }

    public FreemarkerSqlLocator createLocator() {
        return new FreemarkerSqlLocator(this);
    }

    @Override
    public FreemarkerConfig createCopy() {
        return new FreemarkerConfig(this);
    }

    private static ClassLoader selectClassLoader() {
        return Optional.ofNullable(Thread.currentThread().getContextClassLoader())
            .orElseGet(FreemarkerConfig.class::getClassLoader);
    }
}
