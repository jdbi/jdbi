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
package org.jdbi.core;

import java.util.function.Consumer;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.ExtensionContext;
import org.jdbi.core.internal.MemoizingSupplier;
import org.jdbi.core.internal.OnDemandHandleSupplier;

final class LazyHandleSupplier extends AbstractHandleSupplier implements OnDemandHandleSupplier {

    private final Jdbi jdbi;
    private final MemoizingSupplier<Handle> handleHolder = MemoizingSupplier.of(this::createHandle);

    LazyHandleSupplier(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public ConfigRegistry getConfig() {
        ExtensionContext extensionContext = currentExtensionContext();
        return extensionContext != null ? extensionContext.getConfig() : jdbi.getConfig();
    }

    @Override
    public Jdbi getJdbi() {
        return jdbi;
    }

    @Override
    public Handle getHandle() {
        return handleHolder.get();
    }

    private Handle createHandle() {
        // push the current top context into the new Jdbi
        return jdbi.open().acceptExtensionContext(currentExtensionContext());
    }

    @Override
    protected void withHandle(Consumer<Handle> handleConsumer) {
        handleHolder.ifInitialized(handleConsumer);
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            handleHolder.ifInitialized(Handle::close);
        }
    }
}
