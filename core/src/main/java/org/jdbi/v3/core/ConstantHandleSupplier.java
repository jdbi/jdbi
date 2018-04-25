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

import java.util.concurrent.Callable;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.extension.HandleSupplier;

class ConstantHandleSupplier implements HandleSupplier {
    private final Handle handle;

    static HandleSupplier of(Handle handle) {
        return new ConstantHandleSupplier(handle);
    }

    ConstantHandleSupplier(Handle handle) {
        this.handle = handle;
    }

    @Override
    public ConfigRegistry getConfig() {
        return handle.getConfig();
    }

    @Override
    public Handle getHandle() {
        return handle;
    }

    @Override
    public <V> V invokeInContext(ExtensionMethod extensionMethod, ConfigRegistry config, Callable<V> task) throws Exception {
        ExtensionMethod oldExtensionMethod = handle.getExtensionMethod();
        try {
            handle.setExtensionMethod(extensionMethod);

            ConfigRegistry oldConfig = handle.getConfig();
            try {
                handle.setConfig(config);
                return task.call();
            } finally {
                handle.setConfig(oldConfig);
            }
        } finally {
            handle.setExtensionMethod(oldExtensionMethod);
        }
    }
}
