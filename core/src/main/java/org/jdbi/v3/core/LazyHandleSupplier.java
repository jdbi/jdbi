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
import org.jdbi.v3.core.internal.JdbiThreadLocals;

class LazyHandleSupplier implements HandleSupplier, AutoCloseable {
    private final Object[] lock = new Object[0];

    private final Jdbi db;
    private final ThreadLocal<ConfigRegistry> config;
    private final ThreadLocal<ExtensionMethod> extensionMethod = new ThreadLocal<>();

    private volatile Handle handle;
    private volatile boolean closed = false;

    LazyHandleSupplier(Jdbi db, ConfigRegistry config) {
        this.db = db;
        this.config = ThreadLocal.withInitial(() -> config);
    }

    @Override
    public ConfigRegistry getConfig() {
        return config.get();
    }

    @Override
    public Handle getHandle() {
        if (handle == null) {
            initHandle();
        }
        return handle;
    }

    private void initHandle() {
        synchronized (lock) {
            if (handle == null) {
                if (closed) {
                    throw new IllegalStateException("Handle is closed");
                }

                Handle handle = db.open();
                // share extension method thread local with handle,
                // so extension methods set in other threads are preserved
                handle.setExtensionMethodThreadLocal(extensionMethod);
                handle.setConfigThreadLocal(config);

                this.handle = handle;
            }
        }
    }

    @Override
    public <V> V invokeInContext(ExtensionMethod extensionMethod, ConfigRegistry config, Callable<V> task) throws Exception {
        return JdbiThreadLocals.invokeInContext(this.extensionMethod, extensionMethod,
                () -> JdbiThreadLocals.invokeInContext(this.config, config, task));
    }

    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            // once created, the handle owns cleanup of the threadlocals
            if (handle == null) {
                config.remove();
                extensionMethod.remove();
            } else {
                handle.close();
            }
        }
    }
}
