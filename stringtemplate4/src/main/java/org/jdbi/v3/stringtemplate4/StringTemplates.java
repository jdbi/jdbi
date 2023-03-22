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
package org.jdbi.v3.stringtemplate4;

import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration options for {@link StringTemplateEngine}.
 */
public class StringTemplates implements JdbiConfig<StringTemplates> {
    private boolean failOnMissingAttribute = false;

    public StringTemplates() {}

    StringTemplates(StringTemplates other) {
        this.failOnMissingAttribute = other.failOnMissingAttribute;
    }

    /**
     * @return whether missing attributes in a StringTemplate are a rendering error
     */
    public boolean isFailOnMissingAttribute() {
        return failOnMissingAttribute;
    }

    /**
     * Control whether missing attributes cause a rendering exception. Defaults to false.
     * @param failOnMissingAttribute whether a missing attribute throws an exception
     * @return this configuration instance
     */
    public StringTemplates setFailOnMissingAttribute(boolean failOnMissingAttribute) {
        this.failOnMissingAttribute = failOnMissingAttribute;
        return this;
    }

    @Override
    public StringTemplates createCopy() {
        return new StringTemplates(this);
    }
}
