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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.extension.annotation.UseExtensionHandler;
import org.jdbi.core.extension.annotation.UseExtensionHandlerCustomizer;
import org.jdbi.core.internal.RegistrationLists;
import org.jdbi.meta.Alpha;
import org.jdbi.meta.Beta;

/**
 * Configuration class for defining {@code Jdbi} extensions via {@link ExtensionFactory}
 * instances. Holds only registration data and policy; building and caching the {@link ExtensionMetadata}
 * for an extension type is done per configuration registry by {@link ExtensionMetadataResolver}.
 * <p>
 * This configuration is immutable: the {@code register} methods and the policy withers return a new instance,
 * leaving the receiver unchanged.
 */
public final class Extensions implements JdbiConfig<Extensions> {

    private final List<ExtensionFactoryDelegate> extensionFactories;
    private final List<ExtensionHandlerCustomizer> extensionHandlerCustomizers;
    private final List<ExtensionHandlerFactory> extensionHandlerFactories;
    private final List<ConfigCustomizerFactory> configCustomizerFactories;

    private final boolean allowProxy;
    private final boolean failFast;

    /**
     * Creates a new instance.
     * <ul>
     * <li>registers extension handlers factories for bridge and interface default methods and for the {@link UseExtensionHandler} annotation.</li>
     * <li>registers extension handler customizers for the {@link UseExtensionHandlerCustomizer} annotation.</li>
     * <li>registers extension configurer factories for {@link org.jdbi.core.extension.annotation.UseExtensionConfigurer} annotation.</li>
     * </ul>
     */
    public Extensions() {
        this(List.of(),
                buildDefaultHandlerFactories(),
                List.of(UseAnnotationExtensionHandlerCustomizer.INSTANCE),
                List.of(UseAnnotationConfigCustomizerFactory.INSTANCE),
                true, false);
    }

    private Extensions(List<ExtensionFactoryDelegate> extensionFactories,
            List<ExtensionHandlerFactory> extensionHandlerFactories,
            List<ExtensionHandlerCustomizer> extensionHandlerCustomizers,
            List<ConfigCustomizerFactory> configCustomizerFactories,
            boolean allowProxy,
            boolean failFast) {
        this.extensionFactories = extensionFactories;
        this.extensionHandlerCustomizers = extensionHandlerCustomizers;
        this.extensionHandlerFactories = extensionHandlerFactories;
        this.configCustomizerFactories = configCustomizerFactories;
        this.allowProxy = allowProxy;
        this.failFast = failFast;
    }

    private static List<ExtensionHandlerFactory> buildDefaultHandlerFactories() {
        // Registration prepends, so the effective consultation order is the reverse of registration order.
        final List<ExtensionHandlerFactory> factories = new ArrayList<>();
        // default handler factories for bridge and default methods
        factories.add(0, DefaultMethodExtensionHandlerFactory.INSTANCE);
        factories.add(0, BridgeMethodExtensionHandlerFactory.INSTANCE);
        // default handler factory for the UseExtensionHandler annotation (filtered like registerHandlerFactory).
        factories.add(0, FilteringExtensionHandlerFactory.forDelegate(UseAnnotationExtensionHandlerFactory.INSTANCE));
        return List.copyOf(factories);
    }

    /**
     * Register a {@link ExtensionFactory} instance with the extension framework.
     *
     * @param factory the factory to register
     * @return a copy of this configuration with the factory registered
     */
    @CheckReturnValue
    public Extensions register(ExtensionFactory factory) {
        return new Extensions(RegistrationLists.prepend(extensionFactories, new ExtensionFactoryDelegate(factory)),
                extensionHandlerFactories, extensionHandlerCustomizers, configCustomizerFactories, allowProxy, failFast);
    }

    /**
     * Registers a global {@link ExtensionHandlerFactory} instance. This factory is registered globally and will be used
     * with all registered {@link ExtensionFactory} instances.
     * @param extensionHandlerFactory The {@link ExtensionHandlerFactory} to register
     * @return a copy of this configuration with the handler factory registered
     *
     * @since 3.38.0
     */
    @CheckReturnValue
    @Alpha
    public Extensions registerHandlerFactory(ExtensionHandlerFactory extensionHandlerFactory) {
        return internalRegisterHandlerFactory(FilteringExtensionHandlerFactory.forDelegate(extensionHandlerFactory));
    }

    /**
     * Registers a global {@link ExtensionHandlerCustomizer} instance. This customizer is registered globally and will be used
     * with all registered {@link ExtensionFactory} instances.
     * @param extensionHandlerCustomizer The {@link ExtensionHandlerCustomizer} to register
     * @return a copy of this configuration with the handler customizer registered
     *
     * @since 3.38.0
     */
    @CheckReturnValue
    @Alpha
    public Extensions registerHandlerCustomizer(ExtensionHandlerCustomizer extensionHandlerCustomizer) {
        return new Extensions(extensionFactories, extensionHandlerFactories,
                RegistrationLists.prepend(extensionHandlerCustomizers, extensionHandlerCustomizer),
                configCustomizerFactories, allowProxy, failFast);
    }

    /**
     * Registers a global {@link ConfigCustomizerFactory} instance. This factory is registered globally and will be used
     * with all registered {@link ExtensionFactory} instances.
     * @param configCustomizerFactory The {@link ConfigCustomizerFactory} to register
     * @return a copy of this configuration with the config customizer factory registered
     *
     * @since 3.38.0
     */
    @CheckReturnValue
    @Alpha
    public Extensions registerConfigCustomizerFactory(ConfigCustomizerFactory configCustomizerFactory) {
        return new Extensions(extensionFactories, extensionHandlerFactories, extensionHandlerCustomizers,
                RegistrationLists.prepend(configCustomizerFactories, configCustomizerFactory), allowProxy, failFast);
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
     * <br>
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

    private Extensions internalRegisterHandlerFactory(ExtensionHandlerFactory extensionHandlerFactory) {
        return new Extensions(extensionFactories,
                RegistrationLists.prepend(extensionHandlerFactories, extensionHandlerFactory),
                extensionHandlerCustomizers, configCustomizerFactories, allowProxy, failFast);
    }

    /**
     * Returns the globally registered extension handler factories, most-recently-registered first.
     * Consumed by {@link ExtensionMetadataResolver}.
     *
     * @return the registered extension handler factories
     */
    List<ExtensionHandlerFactory> getExtensionHandlerFactories() {
        return extensionHandlerFactories;
    }

    /**
     * Returns the globally registered extension handler customizers, most-recently-registered first.
     * Consumed by {@link ExtensionMetadataResolver}.
     *
     * @return the registered extension handler customizers
     */
    List<ExtensionHandlerCustomizer> getExtensionHandlerCustomizers() {
        return extensionHandlerCustomizers;
    }

    /**
     * Returns the globally registered config customizer factories, most-recently-registered first.
     * Consumed by {@link ExtensionMetadataResolver}.
     *
     * @return the registered config customizer factories
     */
    List<ConfigCustomizerFactory> getConfigCustomizerFactories() {
        return configCustomizerFactories;
    }

    /**
     * Returns a copy of this configuration that allows or disallows using {@link java.lang.reflect.Proxy} to
     * implement extensions.
     *
     * @param allowProxy whether to allow use of Proxy types
     * @return the derived configuration
     */
    @CheckReturnValue
    @Beta
    public Extensions allowProxy(boolean allowProxy) {
        return new Extensions(extensionFactories, extensionHandlerFactories, extensionHandlerCustomizers,
                configCustomizerFactories, allowProxy, failFast);
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

    /**
     * Fail fast if any method in an Extension object is misconfigured, for example if its result type has
     * no registered mapper. The error surfaces when the extension is attached rather than when a method is
     * used for the first time, which is the default.
     *
     * @return a copy of this configuration that fails fast
     * @since 3.39.0
     */
    @CheckReturnValue
    @Alpha
    public Extensions failFast() {
        return new Extensions(extensionFactories, extensionHandlerFactories, extensionHandlerCustomizers,
                configCustomizerFactories, allowProxy, true);
    }

    /**
     * Returns true if misconfigured extension objects fail fast.
     *
     * @return True if misconfigured extension objects fail fast.
     * @since 3.39.0
     */
    @Alpha
    public boolean isFailFast() {
        return failFast;
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
