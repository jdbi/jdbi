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

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jdbi.v3.core.extension.ExtensionConfig;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.NoSuchExtensionException;

class ExtensionRegistry {
    static ExtensionRegistry copyOf(ExtensionRegistry registry) {
        return new ExtensionRegistry(registry.factories);
    }

    private static class Entry<C extends ExtensionConfig<C>> {
        static <C extends ExtensionConfig<C>> Entry<C> copyOf(Entry<C> entry) {
            return new Entry<>(entry.factory, entry.config.createCopy());
        }

        final ExtensionFactory<C> factory;
        final C config;

        Entry(ExtensionFactory<C> factory, C config) {
            this.factory = factory;
            this.config = config;
        }

        <E> E attach(Class<E> extensionType, Supplier<Handle> handle) {
            return factory.attach(extensionType, config.createCopy(), handle);
        }
    }

    private final List<Entry<? extends ExtensionConfig<?>>> factories;

    ExtensionRegistry() {
        this.factories = new CopyOnWriteArrayList<>();
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // working around https://bugs.eclipse.org/bugs/show_bug.cgi?id=499197
    ExtensionRegistry(List<Entry<? extends ExtensionConfig<?>>> factories) {
        this.factories = (List) factories.stream()
                .map(e -> Entry.copyOf((Entry)e))
                .collect(toCollection(CopyOnWriteArrayList::new));
    }

    <C extends ExtensionConfig<C>> void register(ExtensionFactory<C> factory) {
        factories.add(0, new Entry<>(factory, factory.createConfig()));
    }

    boolean hasExtensionFor(Class<?> extensionType) {
        return factories.stream()
                .anyMatch(entry -> entry.factory.accepts(extensionType));
    }

    <E> Optional<E> findExtensionFor(Class<E> extensionType, Supplier<Handle> handle) {
        return factories.stream()
                .filter(entry -> entry.factory.accepts(extensionType))
                .map(entry -> extensionType.cast(entry.attach(extensionType, handle)))
                .findFirst();
    }

    <C extends ExtensionConfig<C>> void configure(Class<C> configClass, Consumer<C> consumer) {
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
}
