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

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A read-only {@link ConfigView} that forwards reads to a {@link ConfigRegistry} supplied on each access. It is not a
 * {@code ConfigRegistry}, so a caller cannot cast it back to reach the in-place mutation surface
 * ({@link ConfigRegistry#configure}). See {@link ConfigView#readOnly}.
 */
final class ReadOnlyConfigView implements ConfigView {

    private final Supplier<ConfigRegistry> registry;

    ReadOnlyConfigView(final Supplier<ConfigRegistry> registry) {
        this.registry = Objects.requireNonNull(registry, "null registry supplier");
    }

    @Override
    public ConfigView getConfig() {
        return this;
    }

    @Override
    public <C extends JdbiConfig<C>> C get(final Class<C> configClass) {
        return registry.get().get(configClass);
    }

    @Override
    public <T> T readAs(final Class<T> asType, final Function<ConfigView, T> create) {
        return registry.get().readAs(asType, create);
    }

    @Override
    public ConfigRegistry createChild() {
        return registry.get().createChild();
    }

    @Override
    public ConfigRegistry createCopy() {
        return registry.get().createCopy();
    }
}
