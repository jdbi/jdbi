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
package org.jdbi.v3.core.extension.internal;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Per-{@link ConfigRegistry} cache of the derived configurations produced when an extension is
 * attached: the instance configuration and the per-method configurations.
 *
 * <p>Deriving those configurations performs a full {@link ConfigRegistry#createCopy()} — one
 * for the instance configuration plus one per extension method — on every {@code attach}.
 * {@link org.jdbi.v3.core.Jdbi#onDemand(Class)} re-attaches on <em>every</em> method call against
 * the same, stable {@code Jdbi}-level configuration, so without caching it repeats
 * {@code 1 + methodCount} registry copies per call. For on-demand-heavy workloads those copies
 * dominate allocation.
 *
 * <p>The derived configuration depends on the identity of the source registry, so this cache is
 * deliberately <b>not</b> shared across copies: {@link #createCopy()} returns a fresh, empty
 * instance. Consequences:
 * <ul>
 *   <li>A registry only ever used as an attach source once — e.g. the per-{@link
 *       org.jdbi.v3.core.Handle} configuration in {@link org.jdbi.v3.core.Handle#attach(Class)}
 *       — caches nothing reusable and behaves exactly as before (no regression).</li>
 *   <li>A long-lived registry reused as an attach source — the {@code Jdbi} root
 *       configuration used by on-demand — amortizes the copies to a one-time cost.</li>
 * </ul>
 * Entries live and die with their owning registry, so there is no unbounded growth.
 *
 * <p><b>Reconfiguration semantics:</b> this cache assumes that a registry reused as an attach
 * source is effectively immutable once it has been attached against. Reconfiguring the {@code Jdbi}
 * root configuration after an on-demand extension has been used will not retroactively change that
 * extension's already-derived configuration. {@code Jdbi} configuration is expected to be finalized
 * before use, so this matches the existing contract. (The shared, Jdbi-level
 * {@link org.jdbi.v3.core.config.internal.ConfigCaches} makes the same "do not respect
 * reconfiguration" assumption, though it differs in that it is shared across copies rather than
 * derived per registry.)
 */
public final class ExtensionConfigCache implements JdbiConfig<ExtensionConfigCache> {

    private final Map<Class<?>, ConfigRegistry> instanceConfigurations = new ConcurrentHashMap<>();
    private final Map<Method, ConfigRegistry> methodConfigurations = new ConcurrentHashMap<>();

    /**
     * Returns the cached instance configuration for the given extension type, computing and caching
     * it on first request.
     */
    public ConfigRegistry instanceConfiguration(Class<?> extensionType, Supplier<ConfigRegistry> factory) {
        return instanceConfigurations.computeIfAbsent(extensionType, type -> factory.get());
    }

    /**
     * Returns the cached method configuration for the given method, computing and caching it on first
     * request.
     */
    public ConfigRegistry methodConfiguration(Method method, Supplier<ConfigRegistry> factory) {
        return methodConfigurations.computeIfAbsent(method, m -> factory.get());
    }

    @Override
    public ExtensionConfigCache createCopy() {
        return new ExtensionConfigCache();
    }
}
