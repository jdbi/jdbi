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

import static org.jdbi.v3.core.internal.JdbiOptionals.findFirstPresent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.extension.ExtensionFactory;

public class ExtensionRegistry implements JdbiConfig<ExtensionRegistry> {
    private final Optional<ExtensionRegistry> parent;
    private final List<ExtensionFactory> factories = new CopyOnWriteArrayList<>();

    public ExtensionRegistry() {
        this.parent = Optional.empty();
    }

    private ExtensionRegistry(ExtensionRegistry that) {
        this.parent = Optional.of(that);
    }

    public void register(ExtensionFactory factory) {
        factories.add(0, factory);
    }

    public boolean hasExtensionFor(Class<?> extensionType) {
        return findFactoryFor(extensionType).isPresent();
    }

    public <E> Optional<E> findExtensionFor(Class<E> extensionType, HandleSupplier handle) {
        return findFactoryFor(extensionType)
                .map(factory -> factory.attach(extensionType, handle));
    }

    private Optional<ExtensionFactory> findFactoryFor(Class<?> extensionType) {
        return findFirstPresent(
                () -> factories.stream()
                        .filter(factory -> factory.accepts(extensionType))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findFactoryFor(extensionType)));
    }

    @Override
    public ExtensionRegistry createChild() {
        return new ExtensionRegistry(this);
    }
}
