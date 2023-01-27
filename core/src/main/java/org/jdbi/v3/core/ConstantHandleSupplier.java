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

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionContext;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.extension.HandleSupplier;

class ConstantHandleSupplier implements HandleSupplier {

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Handle handle;
    private final Deque<ExtensionContext> extensionContexts = new LinkedList<>();

    static HandleSupplier of(Handle handle) {
        return new ConstantHandleSupplier(handle);
    }

    ConstantHandleSupplier(Handle handle) {
        this.handle = handle;
    }

    @Override
    public Jdbi getJdbi() {
        return handle.getJdbi();
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
        return invokeInContext(new ExtensionContext(config, extensionMethod), task);
    }

    @Override
    public <V> V invokeInContext(ExtensionContext extensionContext, Callable<V> task) throws Exception {
        try {
            pushExtensionContext(extensionContext);
            return task.call();
        } finally {
            popExtensionContext();
        }
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            throw new IllegalStateException("Handle is closed");
        }
        extensionContexts.clear();
    }

    private void pushExtensionContext(ExtensionContext extensionContext) {
        extensionContexts.addFirst(extensionContext);
        handle.acceptExtensionContext(extensionContext);
    }

    private void popExtensionContext() {
        extensionContexts.pollFirst();
        handle.acceptExtensionContext(extensionContexts.peek());
    }
}
