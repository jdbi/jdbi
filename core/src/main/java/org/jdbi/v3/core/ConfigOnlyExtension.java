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
package org.jdbi.v3.core;

import org.jdbi.v3.core.extension.ExtensionConfig;
import org.jdbi.v3.core.extension.ExtensionFactory;

class ConfigOnlyExtension<C extends ExtensionConfig<C>> implements ExtensionFactory<C>
{
    private final Class<C> configClass;

    ConfigOnlyExtension(Class<C> configClass)
    {
        this.configClass = configClass;
    }

    @Override
    public C createConfig() {
        try {
            return configClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to instantiate " + configClass, e);
        }
    }

    @Override
    public boolean accepts(Class<?> extensionType) {
        return extensionType == configClass;
    }

    @Override
    public <E> E attach(Class<E> extensionType, C config, HandleSupplier handle) {
        return extensionType.cast(config);
    }
}
