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

import static java.util.stream.Collectors.toList;
import static org.jdbi.v3.core.internal.JdbiOptionals.findFirstPresent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.JdbiConfig;
import org.jdbi.v3.core.extension.NoSuchExtensionException;

public class ExtensionRegistry implements JdbiConfig<ExtensionRegistry> {
    private static class Entry<C extends JdbiConfig<C>> {
        final ExtensionFactory<C> factory;
        final C config;

        Entry(ExtensionFactory<C> factory, C config) {
            this.factory = factory;
            this.config = config;
        }

        <E> E attach(Class<E> extensionType, HandleSupplier handle) {
            return factory.attach(extensionType, config.createChild(), handle);
        }
    }

    private final Optional<ExtensionRegistry> parent;
    private final List<Entry<? extends JdbiConfig<?>>> factories = new CopyOnWriteArrayList<>();

    public ExtensionRegistry() {
        this.parent = Optional.empty();
    }

    private ExtensionRegistry(ExtensionRegistry that) {
        this.parent = Optional.of(that);
    }

    public <C extends JdbiConfig<C>> void register(ExtensionFactory<C> factory) {
        factories.add(0, new Entry<>(factory, factory.createConfig()));
    }

    public boolean hasExtensionFor(Class<?> extensionType) {
        return findEntryFor(extensionType).isPresent();
    }

    public <E> Optional<E> findExtensionFor(Class<E> extensionType, HandleSupplier handle) {
        return findEntryFor(extensionType)
                .map(entry -> extensionType.cast(entry.attach(extensionType, handle)));
    }

    private Optional<ExtensionRegistry.Entry<?>> findEntryFor(Class<?> extensionType) {
        return findFirstPresent(
                () -> factories.stream()
                        .filter(entry -> entry.factory.accepts(extensionType))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findEntryFor(extensionType)));
    }

    public <C extends JdbiConfig<C>> void configure(Class<C> configClass, Consumer<C> consumer) {
        List<C> configs = factories.stream()
                .map(entry -> entry.config)
                .filter(configClass::isInstance)
                .map(configClass::cast)
                .collect(toList());

        if (configs.isEmpty()) {
            throw new NoSuchExtensionException("No extension found with config class " + configClass);
        }

        configs.forEach(consumer::accept);
    }

    @Override
    public ExtensionRegistry createChild() {
        return new ExtensionRegistry(this);
    }
}
