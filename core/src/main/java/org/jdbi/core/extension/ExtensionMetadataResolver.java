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
package org.jdbi.core.extension;

import java.util.Map;
import java.util.function.Function;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.internal.CopyOnWriteHashMap;

import static org.jdbi.core.extension.ExtensionFactory.FactoryFlag.NON_VIRTUAL_FACTORY;

/**
 * Resolves and caches {@link ExtensionMetadata} for a specific {@link ConfigRegistry}.
 * <p>
 * A resolver builds the metadata for an extension type from the passed {@link ExtensionFactory} and the
 * global handler/customizer factories registered on the registry's {@link Extensions} (which holds only
 * registration data), memoizing the outcome. It is obtained per registry via {@link #forRegistry(ConfigRegistry)}
 * and is scoped to that registry: its cache is warm across the extensions attached against a shared registry,
 * yet a forked registry starts with an empty cache and re-resolves against its own registration data.
 * <p>
 * The cache needs no staleness check of its own: any configuration change installs a new immutable config value,
 * which {@link ConfigRegistry#install} responds to by dropping the registry's memoized views, so the next lookup
 * builds a fresh resolver against the new configuration.
 */
public final class ExtensionMetadataResolver {

    /**
     * Returns the extension-metadata resolver for the given registry, creating it on first use.
     *
     * @param config the configuration registry to resolve against
     * @return the registry's memoized extension-metadata resolver
     */
    public static ExtensionMetadataResolver forRegistry(final ConfigRegistry config) {
        return config.readAs(ExtensionMetadataResolver.class, ExtensionMetadataResolver::new);
    }

    private final ConfigRegistry registry;
    private final Map<Class<?>, ExtensionMetadata> metadataCache = new CopyOnWriteHashMap<>();

    private ExtensionMetadataResolver(final ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Retrieves all extension metadata for a specific extension type.
     *
     * @param extensionType    The extension type
     * @param extensionFactory The extension factory for this extension type
     * @return A {@link ExtensionMetadata} object describing the extension handlers and customizers for this extension type
     */
    public ExtensionMetadata findMetadata(final Class<?> extensionType, final ExtensionFactory extensionFactory) {
        final Extensions extensions = registry.get(Extensions.class);
        return metadataCache.computeIfAbsent(extensionType, createMetadata(extensions, extensionFactory));
    }

    private Function<Class<?>, ExtensionMetadata> createMetadata(final Extensions extensions, final ExtensionFactory extensionFactory) {
        return extensionType -> {

            final ExtensionMetadata.Builder builder = ExtensionMetadata.builder(extensionType);

            // prep the extension handler set for this factory
            extensionFactory.getExtensionHandlerFactories(registry).forEach(builder::addExtensionHandlerFactory);
            extensions.getExtensionHandlerFactories().forEach(builder::addExtensionHandlerFactory);

            // InstanceExtensionHandlerFactory for non-virtual factories. These have a backing object and can invoke methods on those objects.
            if (extensionFactory.getFactoryFlags().contains(NON_VIRTUAL_FACTORY)) {
                builder.addExtensionHandlerFactory(InstanceExtensionHandlerFactory.INSTANCE);
            }

            // prep the extension customizer set for this factory
            extensionFactory.getExtensionHandlerCustomizers(registry).forEach(builder::addExtensionHandlerCustomizer);
            extensions.getExtensionHandlerCustomizers().forEach(builder::addExtensionHandlerCustomizer);

            // prep the extension configurer set for this factory
            extensionFactory.getConfigCustomizerFactories(registry).forEach(builder::addConfigCustomizerFactory);
            extensions.getConfigCustomizerFactories().forEach(builder::addConfigCustomizerFactory);

            // build metadata
            extensionFactory.buildExtensionMetadata(builder);

            return builder.build();
        };
    }
}
