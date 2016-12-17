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

class LazyHandleSupplier implements HandleSupplier, AutoCloseable {
    private final Jdbi jdbi;
    private final ThreadLocal<ConfigRegistry> config;
    private final ThreadLocal<ExtensionMethod> extensionMethod = new ThreadLocal<>();

    private volatile Handle handle;
    private volatile boolean closed = false;

    LazyHandleSupplier(Jdbi jdbi, ConfigRegistry config) {
        this.jdbi = jdbi;
        this.config = ThreadLocal.withInitial(() -> config);
    }

    @Override
    public ConfigRegistry getConfig() {
        return config.get();
    }

    public Handle getHandle() {
        if (handle == null) {
            initHandle();
        }
        return handle;
    }

    private synchronized void initHandle() {
        if (handle == null) {
            if (closed) {
                throw new IllegalStateException("Handle is closed");
            }

            Handle handle = jdbi.open();
            // share extension method thread local with handle,
            // so extension methods set in other threads are preserved
            handle.setExtensionMethodThreadLocal(extensionMethod);
            handle.setConfigThreadLocal(config);

            this.handle = handle;
        }
    }

    @Override
    public <V> V invokeInContext(ExtensionMethod extensionMethod, ConfigRegistry config, Callable<V> task) throws Exception {
        ExtensionMethod oldExtensionMethod = this.extensionMethod.get();
        try {
            this.extensionMethod.set(extensionMethod);

            ConfigRegistry oldConfig = this.config.get();
            try {
                this.config.set(config);
                return task.call();
            }
            finally {
                this.config.set(oldConfig);
            }
        }
        finally {
            this.extensionMethod.set(oldExtensionMethod);
        }
    }

    public synchronized void close() {
        closed = true;
        if (handle != null) {
            handle.close();
        }
        config.remove();
        extensionMethod.remove();
    }
}
