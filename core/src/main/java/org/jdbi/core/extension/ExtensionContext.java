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
package org.jdbi.core.extension;

import java.lang.reflect.Method;

import org.jdbi.core.config.ConfigRegistry;

/**
 * Accepted by a handle when executing extension code. This temporarily reconfigures the handle to execute
 * extension code. Reconfigurations are e.g. annotations from the sql objects that define mappers or bindings.
 */
public final class ExtensionContext {
    private final ConfigRegistry config;
    private final ExtensionMethod extensionMethod;

    /**
     * Create an extension context for a configuration only. No extension method information is set.
     * @param config A {@link ConfigRegistry} object.
     * @return An ExtensionContext.
     */
    public static ExtensionContext forConfig(ConfigRegistry config) {
        return new ExtensionContext(config, null);
    }

    public static ExtensionContext forExtensionMethod(ConfigRegistry config, Class<?> type, Method method) {
        return new ExtensionContext(config, new ExtensionMethod(type, method));
    }

    public ExtensionContext(ConfigRegistry config, ExtensionMethod extensionMethod) {
        this.config = config;
        this.extensionMethod = extensionMethod;
    }

    public ConfigRegistry getConfig() {
        return config;
    }

    public ExtensionMethod getExtensionMethod() {
        return extensionMethod;
    }
}
