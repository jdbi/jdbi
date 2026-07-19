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

import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.core.statement.ConfigReader;
import org.jdbi.meta.Alpha;

/**
 * A read-only view of a {@link ConfigRegistry}. It can read configuration values and derive new registries, but it
 * cannot mutate a registry in place. {@link ConfigReader#getConfig()} returns a {@code ConfigView} so that read-only
 * contexts (a {@link org.jdbi.core.Jdbi Jdbi} or a {@link org.jdbi.core.Handle Handle}) do not expose configuration
 * mutation: the in-place mutation surface ({@link ConfigRegistry#configure}) lives only on {@link ConfigRegistry}.
 * <p>
 * A {@code ConfigView} obtained from a read-only context is a distinct read-only delegate, not the underlying
 * {@code ConfigRegistry}, so it cannot be cast back to one to reach {@code configure}. To apply configuration, build
 * the {@code Jdbi} with {@link org.jdbi.core.Jdbi#builder}, open a handle with a config scope
 * ({@link org.jdbi.core.Jdbi#open(java.util.function.Consumer)}), or configure a statement (which is copy-on-write).
 */
public interface ConfigView extends ConfigReader {

    /**
     * Returns this registry's instance of the given config class, creating it on demand if absent.
     *
     * @param configClass the config class type
     * @param <C>         the config class type
     * @return the config value of the given type
     */
    <C extends JdbiConfig<C>> C get(Class<C> configClass);

    /**
     * Returns a memoized, read-only view of this registry of the given type (for example a resolver that carries
     * resolution caches), creating it on first request. See {@link ConfigRegistry#readAs} for the memoization and
     * scoping semantics. This is an internal building block for resolvers, which need the mutable
     * {@link ConfigRegistry} to pass to the factory SPIs; the {@code create} function must not retain or mutate the
     * registry it is given.
     *
     * @param asType the view type, which also keys the memo
     * @param create builds the view from the registry, if not already present
     * @param <T>    the view type
     * @return the memoized view of the given type for this registry
     */
    @Alpha
    <T> T readAs(Class<T> asType, Function<ConfigRegistry, T> create);

    /**
     * Returns a copy-on-write child of this registry (see {@link ConfigRegistry#createChild()}). The returned child
     * is a new registry; mutating it does not affect this view's registry.
     *
     * @return a copy-on-write child registry
     */
    @Alpha
    ConfigRegistry createChild();

    /**
     * Returns an isolated copy of this registry (see {@link ConfigRegistry#createCopy()}). Mutating the copy does not
     * affect this view's registry.
     *
     * @return a copy of this registry
     */
    ConfigRegistry createCopy();

    /**
     * Returns a read-only view that forwards reads to the registry supplied on each access. The returned view is not
     * a {@link ConfigRegistry} and cannot be cast to one, so it never exposes in-place mutation. The supplier is read
     * on each call, so a caller whose effective registry changes over time (for example a handle switching extension
     * contexts) always reads through to the current one.
     *
     * @param registry supplies the registry to read through to
     * @return a read-only view over the supplied registry
     */
    static ConfigView readOnly(final Supplier<ConfigRegistry> registry) {
        return new ReadOnlyConfigView(registry);
    }
}
