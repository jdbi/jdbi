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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.extension.ExtensionHandler.ExtensionHandlerFactory;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.meta.Alpha;
import org.jdbi.v3.meta.Beta;

import static org.jdbi.v3.core.extension.ExtensionFactory.FactoryFlag.VIRTUAL_FACTORY;

/**
 * Configuration class for defining {@code Jdbi} extensions via {@link ExtensionFactory}
 * instances.
 */
public class Extensions implements JdbiConfig<Extensions> {

    private final List<ExtensionFactoryDelegate> extensionFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<Class<?>, ExtensionMetadata> extensionMetadataCache = new ConcurrentHashMap<>();

    private final List<ExtensionHandlerCustomizer> extensionHandlerCustomizers = new CopyOnWriteArrayList<>();
    private final List<ExtensionHandlerFactory> extensionHandlerFactories = new CopyOnWriteArrayList<>();
    private final List<ConfigCustomizerFactory> configCustomizerFactories = new CopyOnWriteArrayList<>();

    private boolean allowProxy = true;

    /**
     * Creates a new instance.
     * <ul>
     * <li>registers extension handlers factories for bridge and interface default methods and for the {@link UseExtensionHandler} annotation.</li>
     * <li>registers extension handler customizers for the {@link org.jdbi.v3.core.extension.annotation.UseExtensionCustomizer} annotation.</li>
     * <li>registers extension configurer factories for {@link org.jdbi.v3.core.extension.annotation.UseExtensionConfigurer} annotation.</li>
     * </ul>
     */
    public Extensions() {
        // default handler factories for bridge and default methods
        registerHandlerFactory(DefaultMethodExtensionHandlerFactory.INSTANCE);
        registerHandlerFactory(BridgeMethodExtensionHandlerFactory.INSTANCE);

        // default handler factory for the UseExtensionHandler annotation.
        registerHandlerFactory(UseExtensionAnnotationHandlerFactory.FACTORY);

        // default handler customizer for the UseExtensionCustomizer annotation.
        registerHandlerCustomizer(UseExtensionAnnotationHandlerCustomizer.HANDLER);

        registerConfigCustomizerFactory(UseExtensionAnnotationConfigCustomizerFactory.FACTORY);
    }

    /**
     * Create an extension configuration by cloning another.
     *
     * @param that the configuration to clone
     */
    private Extensions(Extensions that) {
        allowProxy = that.allowProxy;
        extensionFactories.addAll(that.extensionFactories);
        extensionMetadataCache.putAll(that.extensionMetadataCache);
        extensionHandlerCustomizers.addAll(that.extensionHandlerCustomizers);
        extensionHandlerFactories.addAll(that.extensionHandlerFactories);
        configCustomizerFactories.addAll(that.configCustomizerFactories);
    }

    /**
     * Register a {@link ExtensionFactory} instance with the extension framework.
     *
     * @param factory the factory to register
     * @return This instance
     */
    public Extensions register(ExtensionFactory factory) {
        extensionFactories.add(0, new ExtensionFactoryDelegate(factory));
        return this;
    }

    /**
     * Registers a global {@link ExtensionHandlerFactory} instance. This factory is registered globally and will be used
     * with all registered {@link ExtensionFactory} instances.
     * @param extensionHandlerFactory The {@link ExtensionHandlerFactory} to register
     * @return This instance
     *
     * @since 3.38.0
     */
    @Alpha
    public Extensions registerHandlerFactory(ExtensionHandlerFactory extensionHandlerFactory) {
        extensionHandlerFactories.add(0, extensionHandlerFactory);
        return this;
    }

    /**
     * Registers a global {@link ExtensionHandlerCustomizer} instance. This customizer is registered globally and will be used
     * with all registered {@link ExtensionFactory} instances.
     * @param extensionHandlerCustomizer The {@link ExtensionHandlerCustomizer} to register
     * @return This instance
     *
     * @since 3.38.0
     */
    @Alpha
    public Extensions registerHandlerCustomizer(ExtensionHandlerCustomizer extensionHandlerCustomizer) {
        extensionHandlerCustomizers.add(0, extensionHandlerCustomizer);
        return this;
    }

    /**
     * Registers a global {@link ConfigCustomizerFactory} instance. This factory is registered globally and will be used
     * with all registered {@link ExtensionFactory} instances.
     * @param configCustomizerFactory The {@link ConfigCustomizerFactory} to register
     * @return This instance
     *
     * @since 3.38.0
     */
    @Alpha
    public Extensions registerConfigCustomizerFactory(ConfigCustomizerFactory configCustomizerFactory) {
        configCustomizerFactories.add(0, configCustomizerFactory);
        return this;
    }

    /**
     * Returns true if an extension is registered for the given extension type.
     *
     * @param extensionType the type to query. Must not be null
     * @return true if a registered extension factory handles the type
     */
    public boolean hasExtensionFor(Class<?> extensionType) {
        return findFactoryFor(extensionType).isPresent();
    }

    /**
     * Create an extension instance if a factory accepts the extension type.
     *
     * <b>This method requires access to a {@link HandleSupplier}, which is only useful either from
     * within an extension implementation of inside the Jdbi code. It should rarely be called by
     * user code.</b>
     *
     * @param <E>            the extension type to create
     * @param extensionType  the extension type to create
     * @param handleSupplier A handle supplier object
     * @return an attached extension instance if a factory is found, {@link Optional#empty()} otherwise
     */
    public <E> Optional<E> findFor(Class<E> extensionType, HandleSupplier handleSupplier) {
        return findFactoryFor(extensionType)
                .map(factory -> factory.attach(extensionType, handleSupplier));
    }

    private Optional<ExtensionFactory> findFactoryFor(Class<?> extensionType) {
        for (ExtensionFactory factory : extensionFactories) {
            if (factory.accepts(extensionType)) {
                return Optional.of(factory);
            }
        }

        return Optional.empty();
    }

    /**
     * Find the registered factory of the given type. The factory returned from this call
     * may not be the same instance that was registered with {@link Extensions#register(ExtensionFactory)}.
     *
     * @param factoryType the factory's type to find
     * @return the found factory, if any or {@link Optional#empty()} otherwise
     */
    public Optional<ExtensionFactory> findFactory(Class<? extends ExtensionFactory> factoryType) {
        for (ExtensionFactoryDelegate factory : extensionFactories) {
            if (factoryType.isInstance(factory.getDelegatedFactory())) {
                return Optional.of(factory);
            }
        }

        return Optional.empty();
    }

    /**
     * Retrieves all extension metadata for a specific extension type.
     *
     * @param extensionType The extension type
     * @param config A config registry that can be used to look up other configuration elements
     * @param extensionFactory The extension factory for this extension type
     * @return A {@link ExtensionMetadata} object describing the extension handlers and customizers for this extension type
     *
     * @since 3.38.0
     */
    @Alpha
    public ExtensionMetadata findMetadata(Class<?> extensionType, ConfigRegistry config, ExtensionFactory extensionFactory) {
        return extensionMetadataCache.computeIfAbsent(extensionType, createMetadata(config, extensionFactory));
    }

    private Function<Class<?>, ExtensionMetadata> createMetadata(ConfigRegistry config, ExtensionFactory extensionFactory) {
        return extensionType -> {

            ExtensionMetadata.Builder builder = ExtensionMetadata.builder(extensionType);

            // prep the extension handler set for this factory
            extensionFactory.getExtensionHandlerFactories(config).forEach(builder::addExtensionHandlerFactory);
            extensionHandlerFactories.forEach(builder::addExtensionHandlerFactory);

            // InstanceExtensionHandlerFactory for non-virtual factories. These have a backing object and can invoke methods on those objects.
            if (!extensionFactory.getFactoryFlags().contains(VIRTUAL_FACTORY)) {
                builder.addExtensionHandlerFactory(InstanceExtensionHandlerFactory.INSTANCE);
            }

            // prep the extension customizer set for this factory
            extensionFactory.getExtensionHandlerCustomizers(config).forEach(builder::addExtensionHandlerCustomizer);
            extensionHandlerCustomizers.forEach(builder::addExtensionHandlerCustomizer);

            // prep the extension configurer set for this factory
            extensionFactory.getConfigCustomizerFactories(config).forEach(builder::addConfigCustomizerFactory);
            configCustomizerFactories.forEach(builder::addConfigCustomizerFactory);

            // build metadata
            extensionFactory.buildExtensionInitData(builder);

            return builder.build();
        };
    }

    /**
     * Allow using {@link java.lang.reflect.Proxy} to implement extensions.
     *
     * @param allowProxy whether to allow use of Proxy types
     * @return this
     */
    @Beta
    public Extensions setAllowProxy(boolean allowProxy) {
        this.allowProxy = allowProxy;
        return this;
    }

    /**
     * Returns whether Proxy classes are allowed to be used.
     *
     * @return whether Proxy classes are allowed to be used.
     */
    @Beta
    public boolean isAllowProxy() {
        return allowProxy;
    }

    @Override
    public Extensions createCopy() {
        return new Extensions(this);
    }

    /**
     * Throw if proxy creation is disallowed.
     */
    @Beta
    public void onCreateProxy() {
        if (!isAllowProxy()) {
            throw new IllegalStateException(
                    "Creating onDemand proxy disallowed. Ensure @GenerateSqlObject annotation is being processed by `jdbi3-generator` annotation processor.");
        }
    }
}
