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
package org.jdbi.sqlobject;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.extension.ExtensionHandlerFactory;
import org.jdbi.core.internal.RegistrationLists;

/**
 * Registry for {@link HandlerFactory handler factories}, which produce {@link Handler handlers} for SQL object methods.
 * By default, a factory is registered for methods annotated
 * with SQL annotations such as {@code @SqlUpdate} or {@code SqlQuery}. Clients may register additional factories to provide
 * support for other use cases. In the case that two or more registered factories would support a particular SQL object
 * method, the last-registered factory takes precedence.
 *
 * @deprecated Use {@link ExtensionHandlerFactory} instances that are returned
 * from the {@link org.jdbi.core.extension.ExtensionFactory#getExtensionHandlerFactories(ConfigRegistry)} method.
 */
@Deprecated(since = "3.38.0", forRemoval = true)
public final class Handlers implements JdbiConfig<Handlers> {
    private final List<HandlerFactory> factories;

    public Handlers() {
        this(List.of());
    }

    private Handlers(List<HandlerFactory> factories) {
        this.factories = factories;
    }

    List<HandlerFactory> getFactories() {
        return factories;
    }

    /**
     * Registers the given handler factory with the registry.
     * @param factory the factory to register
     * @return a copy of this configuration with the factory registered
     */
    public Handlers register(HandlerFactory factory) {
        return new Handlers(RegistrationLists.prepend(factories, factory));
    }

    public Optional<Handler> findFor(Class<?> sqlObjectType, Method method) {
        return factories.stream()
                .flatMap(factory -> factory.buildHandler(sqlObjectType, method).stream())
                .findFirst();
    }

    @Override
    public Handlers createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}
