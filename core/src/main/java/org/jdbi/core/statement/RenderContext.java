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
package org.jdbi.core.statement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.core.config.ConfigRegistry;

/**
 * The information a {@link TemplateEngine} needs to render a SQL template: the {@link ConfigRegistry}
 * and the defined attributes ("defines") in effect for this rendering. Defines are layered — a value
 * defined for this specific execution takes precedence over a configuration-level default.
 */
public final class RenderContext {
    private final ConfigRegistry config;
    private final Map<String, Object> defines;

    /**
     * A render context with no per-execution defines; defined attributes come from the configuration.
     *
     * @param config the configuration in effect
     * @return a render context backed only by the configuration
     */
    public static RenderContext of(final ConfigRegistry config) {
        return new RenderContext(config, Collections.emptyMap());
    }

    /**
     * @param config  the configuration in effect
     * @param defines per-execution defined attributes, layered over the configuration's defaults
     */
    public RenderContext(final ConfigRegistry config, final Map<String, Object> defines) {
        this.config = config;
        this.defines = defines;
    }

    /**
     * @return the configuration in effect for this rendering
     */
    public ConfigRegistry getConfig() {
        return config;
    }

    /**
     * Returns the value of a defined attribute, or {@code null} if it is not defined. Per-execution
     * defines take precedence over configuration-level defaults.
     *
     * @param key the attribute name
     * @return the attribute value, or {@code null}
     */
    public Object getAttribute(final String key) {
        if (defines.containsKey(key)) {
            return defines.get(key);
        }
        return config.getAttribute(key);
    }

    /**
     * Returns all defined attributes in effect: the configuration-level defaults overlaid with any
     * per-execution defines.
     *
     * @return the defined attributes
     */
    public Map<String, Object> getAttributes() {
        if (defines.isEmpty()) {
            return config.getAttributes();
        }
        final Map<String, Object> merged = new HashMap<>(config.getAttributes());
        merged.putAll(defines);
        return merged;
    }
}
