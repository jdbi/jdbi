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
package org.jdbi.stringtemplate4;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;

/**
 * Configuration options for {@link StringTemplateEngine}.
 */
public final class StringTemplates implements JdbiConfig<StringTemplates> {
    private final boolean failOnMissingAttribute;

    public StringTemplates() {
        this(false);
    }

    private StringTemplates(boolean failOnMissingAttribute) {
        this.failOnMissingAttribute = failOnMissingAttribute;
    }

    /**
     * Returns whether missing attributes in a StringTemplate are a rendering error.
     *
     * @return whether missing attributes in a StringTemplate are a rendering error
     */
    public boolean isFailOnMissingAttribute() {
        return failOnMissingAttribute;
    }

    /**
     * Returns a copy of this configuration controlling whether missing attributes cause a rendering exception.
     * Defaults to false.
     * @param failOnMissingAttribute whether a missing attribute throws an exception
     * @return the derived configuration
     */
    @CheckReturnValue
    public StringTemplates failOnMissingAttribute(boolean failOnMissingAttribute) {
        return new StringTemplates(failOnMissingAttribute);
    }

}
