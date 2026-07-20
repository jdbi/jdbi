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
package org.jdbi.core.config;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jdbi.core.argument.Arguments;
import org.jdbi.core.collector.JdbiCollectors;
import org.jdbi.core.config.internal.ConfigCaches;
import org.jdbi.core.internal.JdbiClassUtils;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.mapper.RowMappers;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.meta.Alpha;

/**
 * A registry of {@link JdbiConfig} instances by type.
 *
 * @see Configurable
 */
public final class ConfigRegistry implements ConfigView {

    private static final Class<?>[] JDBI_CONFIG_TYPES = {ConfigRegistry.class};

    // Lazily allocated: an un-forked copy-on-write child (parent != null) never touches these maps -- reads
    // delegate to the parent (see get()/readAs()) -- so it holds neither. They are allocated exactly when a
    // registry has no parent: the root and full-copy constructors, and fork() when a child detaches. Thus the
    // invariant parent == null <=> configs != null && views != null holds, and createChild() allocates only the
    // registry shell. A parent-less registry is either the (safely published, then frozen) root or a
    // thread-confined copy/fork, so these fields need no further synchronization.
    private Map<Class<? extends JdbiConfig<?>>, JdbiConfig<?>> configs;
    private final Map<Class<? extends JdbiConfig<?>>, Function<ConfigRegistry, JdbiConfig<?>>> configFactories;

    // Per-registry memoized read-only views of this registry (e.g. resolvers). Deliberately NOT copied by
    // the copy constructor, and cleared by install(): a change to any config value invalidates memoized
    // resolvers, so a registry never serves a view built against a superseded config.
    private Map<Class<?>, Object> views;

    // Non-null only for an un-forked copy-on-write child (see createChild()): reads delegate to this parent
    // until the child's first install(), at which point the child materialises its own configs and detaches.
    private ConfigRegistry parent;

    // Cached read-only view of this registry, handed to readAs create-functions (e.g. resolvers) so a resolver
    // passes a ConfigView -- never this mutable registry -- to the factory SPIs it invokes. Lazily created; the
    // wrapper is stateless (it forwards to this), so a benign race that builds two equivalent wrappers is harmless.
    private ConfigView readOnlyView;

    /**
     * Creates a new config registry.
     */
    public ConfigRegistry() {
        configFactories = new ConcurrentHashMap<>();
        configs = new ConcurrentHashMap<>(32);
        views = new ConcurrentHashMap<>(4);
        get(ConfigCaches.class);
        get(SqlStatements.class);
        get(Arguments.class);
        get(RowMappers.class);
        get(ColumnMappers.class);
        get(JdbiCollectors.class);
    }

    private ConfigRegistry(ConfigRegistry source, boolean asChild) {
        configFactories = source.configFactories;
        if (asChild) {
            // Copy-on-write child: share the parent's config values (and warm resolver views) by delegation
            // until the first install(). Holds no maps of its own until fork().
            parent = source;
        } else {
            // Full snapshot: copy the effective config set, walking any copy-on-write parent chain so that a
            // nearer (child) value wins. Values are immutable, so createCopy() returns the same instance.
            configs = new ConcurrentHashMap<>(32);
            views = new ConcurrentHashMap<>(4);
            for (ConfigRegistry r = source; r != null; r = r.parent) {
                if (r.configs != null) {
                    r.configs.forEach((type, config) -> configs.putIfAbsent(type, config.createCopy()));
                }
            }
        }
    }

    /**
     * Returns this registry's instance of the given config class. Creates an instance on-demand if this registry does
     * not have one of the given type yet.
     *
     * @param configClass the config class type.
     * @param <C>         the config class type.
     * @return the given config class instance that belongs to this registry.
     */
    @Override
    public <C extends JdbiConfig<C>> C get(Class<C> configClass) {
        // we would computeIfAbsent if not for JDK-8062841 >:(
        if (configs != null) {
            final JdbiConfig<?> lookup = configs.get(configClass);
            if (lookup != null) {
                return configClass.cast(lookup);
            }
        }
        if (parent != null) {
            // Un-forked child: read (and lazily create the shared default) through the parent.
            return parent.get(configClass);
        }
        // parent == null, so this registry owns its configs map (see the field comment).
        C config = configClass.cast(configFactory(configClass).apply(this));
        return Optional.ofNullable(configClass.cast(configs.putIfAbsent(configClass, config))).orElse(config);
    }

    /**
     * Installs the given configuration value for its type, replacing any current value. This is the write half
     * of {@link Configurable#configure(Class, java.util.function.UnaryOperator)}: a config value is derived
     * (config values are immutable) and the derived instance replaces the previous one.
     *
     * @param configClass the config type
     * @param config      the value to install
     * @param <C>         the config type
     */
    <C extends JdbiConfig<C>> void install(final Class<C> configClass, final C config) {
        if (parent != null) {
            fork();
        }
        // parent == null here, so configs and views are allocated (root/full-copy ctor, or fork() just now).
        // A changed config value invalidates any memoized resolver built against the previous value.
        views.clear();
        configs.put(configClass, config);
    }

    /**
     * Materialises a copy-on-write child ({@link #createChild()}) on its first write: snapshot the effective
     * config set from the parent chain into this registry, then detach so subsequent reads and writes are local
     * and never touch the (shared) parent.
     */
    private void fork() {
        configs = new ConcurrentHashMap<>(32);
        views = new ConcurrentHashMap<>(4);
        for (ConfigRegistry r = parent; r != null; r = r.parent) {
            if (r.configs != null) {
                r.configs.forEach(configs::putIfAbsent);
            }
        }
        parent = null;
    }

    /**
     * Derives a new configuration value by applying the operator to the current value of the given type and
     * installs it into this registry, replacing the previous value. This mutates this registry in place, so it
     * is for a registry that is not yet shared (during setup / extension-config derivation) or one whose
     * in-place mutation is intentional; {@link Configurable#configure(Class, UnaryOperator)} routes through here.
     *
     * @param configClass the config type
     * @param configurer  operator applied to the current value, returning the value to install
     * @param <C>         the config type
     * @return this registry
     */
    public <C extends JdbiConfig<C>> ConfigRegistry configure(final Class<C> configClass, final UnaryOperator<C> configurer) {
        install(configClass, configurer.apply(get(configClass)));
        return this;
    }

    /**
     * Returns a memoized, read-only view of this registry of the given type, creating it on first request.
     * Views (for example resolvers that carry resolution caches) are scoped to this registry: a copy of the
     * registry does not inherit them, so a view built against one registry is never observed by another.
     * <p>
     * A view is safe to hold a reference back to this registry, because it is never shared across registry
     * copies. The {@code create} function must not call {@code readAs} re-entrantly for the same type.
     *
     * @param asType the view type, which also keys the memo
     * @param create builds the view from this registry, if not already present
     * @param <T>    the view type
     * @return the memoized view of the given type for this registry
     */
    @Alpha
    @Override
    public <T> T readAs(final Class<T> asType, final Function<ConfigView, T> create) {
        if (parent != null) {
            // Un-forked child: its effective config equals the parent's, so reuse the parent's (warm) view.
            return parent.readAs(asType, create);
        }
        // get/putIfAbsent rather than computeIfAbsent to avoid re-entrancy issues (see JDK-8062841).
        final T existing = asType.cast(views.get(asType));
        if (existing != null) {
            return existing;
        }
        // Hand the create-function a read-only view, not this registry, so a view (e.g. a resolver) cannot reach
        // in-place mutation, and readAs cannot be abused to extract the mutable registry from a read-only context.
        final T created = create.apply(asReadOnlyView());
        final T previous = asType.cast(views.putIfAbsent(asType, created));
        return previous != null ? previous : created;
    }

    // Package-private: the read-only view handed to readAs create-functions. Exposed to same-package tests
    // that assert readAs binds a view to the intended registry.
    ConfigView asReadOnlyView() {
        if (readOnlyView == null) {
            readOnlyView = ConfigView.readOnly(() -> this);
        }
        return readOnlyView;
    }

    private Function<ConfigRegistry, JdbiConfig<?>> configFactory(Class<? extends JdbiConfig<?>> configClass) {
        return configFactories.computeIfAbsent(configClass, klass -> {
            var handleHolder = JdbiClassUtils.findConstructor(klass, JDBI_CONFIG_TYPES);
            return registry -> handleHolder.invoke(handle -> handle.invokeExact(registry));
        });
    }

    /**
     * Returns a copy of this config registry.
     *
     * @return a copy of this config registry
     * @see JdbiConfig#createCopy() config objects in the returned registry are copies of the corresponding
     * config objects from this registry.
     */
    @Override
    public ConfigRegistry createCopy() {
        return new ConfigRegistry(this, false);
    }

    /**
     * Returns a copy-on-write child of this registry, for a short-lived scope (e.g. a single statement) that may
     * add a few config values but usually adds none. Until its first {@link Configurable#configure} call the
     * child holds no config of its own: reads delegate to this registry and reuse its memoized resolvers, so an
     * unmodified scope pays no per-use copy. The first write materialises a private snapshot and detaches, so the
     * change never affects this registry. Unlike {@link #createCopy()}, an unmodified child is nearly free.
     * <p>
     * Until it forks, the child is a live view of this registry rather than a frozen snapshot: a later change
     * to this registry is visible through the child. Use {@link #createCopy()} when an isolated snapshot is
     * required. Like any registry the child is not safe under concurrent mutation, so a child and its parent
     * should be confined to a single thread.
     *
     * @return a copy-on-write child of this registry
     */
    @Alpha
    @Override
    public ConfigRegistry createChild() {
        return new ConfigRegistry(this, true);
    }

    /**
     * Reports whether this registry is an un-forked copy-on-write child (see {@link #createChild()}): one that
     * has not yet been mutated and whose reads still delegate to its parent. A child forks (returning {@code
     * false} thereafter) on its first {@link Configurable#configure} call. Intended for internal use by
     * statement execution to decide whether a snapshot rendered against the parent is still valid.
     *
     * @return {@code true} if this is an un-forked child, {@code false} for a root, a full copy, or a forked child
     */
    @Alpha
    public boolean isUnforked() {
        return parent != null;
    }

    @Override
    public ConfigRegistry getConfig() {
        return this;
    }
}
