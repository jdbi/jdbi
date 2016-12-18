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
package org.jdbi.v3.core.extension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.config.JdbiConfig;

public class Extensions implements JdbiConfig<Extensions> {
    private final List<ExtensionFactory> factories = new CopyOnWriteArrayList<>();

    public Extensions() {
    }

    private Extensions(Extensions that) {
        factories.addAll(that.factories);
    }

    public Extensions register(ExtensionFactory factory) {
        factories.add(0, factory);
        return this;
    }

    public boolean hasExtensionFor(Class<?> extensionType) {
        return findFactoryFor(extensionType).isPresent();
    }

    public <E> Optional<E> findFor(Class<E> extensionType, HandleSupplier handle) {
        return findFactoryFor(extensionType)
                .map(factory -> factory.attach(extensionType, handle));
    }

    private Optional<ExtensionFactory> findFactoryFor(Class<?> extensionType) {
        return factories.stream()
                .filter(factory -> factory.accepts(extensionType))
                .findFirst();
    }

    public <F extends ExtensionFactory> Optional<F> findFactory(Class<F> factoryType) {
        return factories.stream()
                .filter(factoryType::isInstance)
                .map(factoryType::cast)
                .findFirst();
    }

    @Override
    public Extensions createCopy() {
        return new Extensions(this);
    }
}
