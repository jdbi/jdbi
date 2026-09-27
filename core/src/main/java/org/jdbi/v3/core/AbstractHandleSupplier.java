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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jdbi.v3.core.extension.ExtensionContext;
import org.jdbi.v3.core.extension.HandleSupplier;

abstract class AbstractHandleSupplier implements HandleSupplier {

    private final AtomicBoolean closed = new AtomicBoolean();

    // Per thread, because an attached extension can be called from several threads at once. Each call restores the
    // context that was current on its thread when it started.
    private final ThreadLocal<ExtensionContext> threadExtensionContext = new ThreadLocal<>();

    protected AbstractHandleSupplier() {}

    @Override
    public <V> V invokeInContext(ExtensionContext extensionContext, Callable<V> task) throws Exception {
        final ExtensionContext previousExtensionContext = threadExtensionContext.get();
        try {
            setExtensionContext(extensionContext);
            return task.call();
        } finally {
            setExtensionContext(previousExtensionContext);
        }
    }

    /** Returns the extension context of the current thread or null if none exists. */
    protected ExtensionContext currentExtensionContext() {
        return threadExtensionContext.get();
    }

    protected abstract void withHandle(Consumer<Handle> handleConsumer);

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            throw new IllegalStateException("Handle is closed");
        }
    }

    private void setExtensionContext(ExtensionContext extensionContext) {
        if (extensionContext == null) {
            threadExtensionContext.remove();
        } else {
            threadExtensionContext.set(extensionContext);
        }
        // a null context resets the handle to its default context
        withHandle(handle -> handle.acceptExtensionContext(extensionContext));
    }
}
