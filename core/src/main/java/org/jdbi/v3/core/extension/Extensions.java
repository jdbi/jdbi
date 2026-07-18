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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandlerCustomizer;
import org.jdbi.v3.core.internal.CopyOnWriteHashMap;
import org.jdbi.v3.meta.Alpha;
import org.jdbi.v3.meta.Beta;

import static org.jdbi.v3.core.extension.ExtensionFactory.FactoryFlag.NON_VIRTUAL_FACTORY;

/**
 * Configuration class for defining {@code Jdbi} extensions via {@link ExtensionFactory}
 * instances.
 */
public class Extensions implements JdbiConfig<Extensions> {

    private final List<ExtensionFactoryDelegate> extensionFactories;

    // Shared with the config this instance was copied from, so metadata computed inside a forked config (e.g.
    // a per-request Handle via handle.attach()) is reused by every other config forked from the same root.
    // Deep-copying it, as every other field is, left an attach()-only workload recomputing metadata on every
    // fresh Handle (jdbi/jdbi#2991). Metadata depends on the registrations here and on other config types read
    // through the registry while building it (e.g. Handlers, HandlerDecorators in sqlobject); any change to
    // such an input must invalidateMetadataCache() first so stale metadata is not served afterwards.
    // Annotation-driven config (e.g. @RegisterRowMapper) is exempt: it is a config customizer replayed per
    // instance at attach time, not baked into cached metadata.
    private volatile Map<Class<?>, ExtensionMetadata> extensionMetadataCache;

    private final List<ExtensionHandlerCustomizer> extensionHandlerCustomizers;
    private final List<ExtensionHandlerFactory> extensionHandlerFactories;
    private final List<ConfigCustomizerFactory> configCustomizerFactories;

    // Read while computing metadata (see ExtensionMetadata.isFailFast) on handle threads other than the one
    // that configured this instance, now that findMetadata runs against the shared cache. AtomicBoolean gives
    // the needed safe publication (a plain field trips SpotBugs AT_STALE_THREAD_WRITE_OF_PRIMITIVE). They are
    // final so the reference publishes safely via the copy constructor.
    private final AtomicBoolean allowProxy = new AtomicBoolean();
    private final AtomicBoolean failFast = new AtomicBoolean();

    private ConfigRegistry registry;

    /**
     * Creates a new instance.
     * <ul>
     * <li>registers extension handlers factories for bridge and interface default methods and for the {@link UseExtensionHandler} annotation.</li>
     * <li>registers extension handler customizers for the {@link UseExtensionHandlerCustomizer} annotation.</li>
     * <li>registers extension configurer factories for {@link org.jdbi.v3.core.extension.annotation.UseExtensionConfigurer} annotation.</li>
     * </ul>
     */
    public Extensions() {
        extensionFactories = new CopyOnWriteArrayList<>();
        extensionMetadataCache = new CopyOnWriteHashMap<>();
        extensionHandlerCustomizers = new CopyOnWriteArrayList<>();
        extensionHandlerFactories = new CopyOnWriteArrayList<>();
        configCustomizerFactories = new CopyOnWriteArrayList<>();

        allowProxy.set(true);
        failFast.set(false);

        // default handler factories for bridge and default methods
        internalRegisterHandlerFactory(DefaultMethodExtensionHandlerFactory.INSTANCE);
        internalRegisterHandlerFactory(BridgeMethodExtensionHandlerFactory.INSTANCE);

        // default handler factory for the UseExtensionHandler annotation.
        registerHandlerFactory(UseAnnotationExtensionHandlerFactory.INSTANCE);

        // default handler customizer for the UseExtensionHandlerCustomizer annotation.
        registerHandlerCustomizer(UseAnnotationExtensionHandlerCustomizer.INSTANCE);

        registerConfigCustomizerFactory(UseAnnotationConfigCustomizerFactory.INSTANCE);
    }

    /**
     * Create an extension configuration by cloning another.
     *
     * @param that the configuration to clone
     */
    private Extensions(Extensions that) {
        extensionFactories = new CopyOnWriteArrayList<>(that.extensionFactories);
        // Share the parent's cache reference rather than copying it. See the field comment.
        extensionMetadataCache = that.extensionMetadataCache;
        extensionHandlerCustomizers = new CopyOnWriteArrayList<>(that.extensionHandlerCustomizers);
        extensionHandlerFactories = new CopyOnWriteArrayList<>(that.extensionHandlerFactories);
        configCustomizerFactories = new CopyOnWriteArrayList<>(that.configCustomizerFactories);

        allowProxy.set(that.allowProxy.get());
        failFast.set(that.failFast.get());
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Register a {@link ExtensionFactory} instance with the extension framework.
     *
     * @param factory the factory to register
     * @return This instance
     */
    public Extensions register(ExtensionFactory factory) {
        invalidateMetadataCache();
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
        invalidateMetadataCache();
        return internalRegisterHandlerFactory(FilteringExtensionHandlerFactory.forDelegate(extensionHandlerFactory));
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
        invalidateMetadataCache();
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
        invalidateMetadataCache();
        configCustomizerFactories.add(0, configCustomizerFactory);
        return this;
    }

    /**
     * Invalidates this configuration's extension metadata cache. Call this before mutating any state that
     * {@linkplain #findMetadata computed metadata} depends on, so that metadata computed under the previous
     * configuration is not served afterwards.
     * <p>
     * The cache is shared across copies of this configuration (see {@link #findMetadata}). Copies made before
     * this call keep their existing cache — a valid snapshot as of their creation — so a handle still sees
     * configuration as of the moment it was opened; this configuration and later copies recompute.
     * <p>
     * The {@code register*} methods here call this for their own registrations. A configuration type in
     * another module whose contents feed metadata computation must call it from its own mutators, via
     * {@code registry.get(Extensions.class).invalidateMetadataCache()} (the sqlobject {@code Handlers} and
     * {@code HandlerDecorators} do so).
     *
     * @since 3.55.0
     */
    @Alpha
    public void invalidateMetadataCache() {
        // Replace, don't clear: copies share this map reference and must keep their snapshot, so it must not
        // be mutated in place.
        extensionMetadataCache = new CopyOnWriteHashMap<>();
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

    /**
     * Retrieves all extension metadata for a specific extension type.
     *
     * @param extensionType    The extension type
     * @param extensionFactory The extension factory for this extension type
     * @return A {@link ExtensionMetadata} object describing the extension handlers and customizers for this extension type
     *
     * @since 3.38.0
     */
    @Alpha
    public ExtensionMetadata findMetadata(Class<?> extensionType, ExtensionFactory extensionFactory) {
        // Racing first-computations may build metadata more than once (computeIfAbsent runs the mapping
        // function under a retry loop), which is harmless because metadata is a pure function of the config.
        return extensionMetadataCache.computeIfAbsent(extensionType, createMetadata(extensionFactory));
    }

    private Extensions internalRegisterHandlerFactory(ExtensionHandlerFactory extensionHandlerFactory) {
        extensionHandlerFactories.add(0, extensionHandlerFactory);
        return this;
    }

    private Function<Class<?>, ExtensionMetadata> createMetadata(ExtensionFactory extensionFactory) {
        return extensionType -> {

            ExtensionMetadata.Builder builder = ExtensionMetadata.builder(extensionType);

            // prep the extension handler set for this factory
            extensionFactory.getExtensionHandlerFactories(registry).forEach(builder::addExtensionHandlerFactory);
            extensionHandlerFactories.forEach(builder::addExtensionHandlerFactory);

            // InstanceExtensionHandlerFactory for non-virtual factories. These have a backing object and can invoke methods on those objects.
            if (extensionFactory.getFactoryFlags().contains(NON_VIRTUAL_FACTORY)) {
                builder.addExtensionHandlerFactory(InstanceExtensionHandlerFactory.INSTANCE);
            }

            // prep the extension customizer set for this factory
            extensionFactory.getExtensionHandlerCustomizers(registry).forEach(builder::addExtensionHandlerCustomizer);
            extensionHandlerCustomizers.forEach(builder::addExtensionHandlerCustomizer);

            // prep the extension configurer set for this factory
            extensionFactory.getConfigCustomizerFactories(registry).forEach(builder::addConfigCustomizerFactory);
            configCustomizerFactories.forEach(builder::addConfigCustomizerFactory);

            // build metadata
            extensionFactory.buildExtensionMetadata(builder);

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
        this.allowProxy.set(allowProxy);
        return this;
    }

    /**
     * Returns whether Proxy classes are allowed to be used.
     *
     * @return whether Proxy classes are allowed to be used.
     */
    @Beta
    public boolean isAllowProxy() {
        return allowProxy.get();
    }

    /**
     * Fail fast if any method in an Extension object is misconfigured and can not be warmed. Default is to
     * fail when a method is used for the first time.
     *
     * @return this
     * @since 3.39.0
     */
    @Alpha
    public Extensions failFast() {
        this.failFast.set(true);

        return this;
    }

    /**
     * Returns true if misconfigured extension objects fail fast.
     *
     * @return True if misconfigured extension objects fail fast.
     * @since 3.39.0
     */
    @Alpha
    public boolean isFailFast() {
        return failFast.get();
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
