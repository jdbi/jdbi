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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jdbi.v3.core.extension.ExtensionContext;
import org.jdbi.v3.core.extension.HandleSupplier;

abstract class AbstractHandleSupplier implements HandleSupplier {

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Deque<ExtensionContext> extensionContexts = new ArrayDeque<>();

    protected AbstractHandleSupplier() {}

    @Override
    public <V> V invokeInContext(ExtensionContext extensionContext, Callable<V> task) throws Exception {
        try {
            pushExtensionContext(extensionContext);
            return task.call();
        } finally {
            popExtensionContext();
        }
    }

    /** Returns the current extension context or null if none exists. */
    protected ExtensionContext currentExtensionContext() {
        return extensionContexts.peek();
    }

    protected abstract void withHandle(Consumer<Handle> handleConsumer);

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            throw new IllegalStateException("Handle is closed");
        }
        extensionContexts.clear();
    }

    private void pushExtensionContext(ExtensionContext extensionContext) {
        extensionContexts.addFirst(extensionContext);
        withHandle(handle -> handle.acceptExtensionContext(extensionContext));
    }

    private void popExtensionContext() {
        // pop the current context
        extensionContexts.pollFirst();
        // set to the previous context (or the default if no previous context exists)
        withHandle(h -> h.acceptExtensionContext(currentExtensionContext()));
    }
}
