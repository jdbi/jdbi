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
import org.jdbi.core.extension.HandleSupplier;

final class ConstantHandleSupplier extends AbstractHandleSupplier {

    private final Handle handle;

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
    protected void withHandle(Consumer<Handle> handleConsumer) {
        handleConsumer.accept(handle);
    }
}
