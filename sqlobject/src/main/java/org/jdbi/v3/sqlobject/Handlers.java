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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.JdbiOptionals;

/**
 * Registry for {@link HandlerFactory handler factories}, which produce {@link Handler handlers} for SQL object methods.
 * By default, a factory is registered for default methods ({@link DefaultMethodHandlerFactory}) and for methods annotated
 * with SQL annotations such as {@code @SqlUpdate} or {@code SqlQuery}. Clients may register additional factories to provide
 * support for other use cases. In the case that two or more registered factories would support a particular SQL object
 * method, the last-registered factory takes precedence.
 */
public class Handlers implements JdbiConfig<Handlers> {
    private final List<HandlerFactory> factories = new CopyOnWriteArrayList<>();

    public Handlers() {
        register(new DefaultMethodHandlerFactory());
        register(new SqlMethodHandlerFactory());
        register(new BridgeMethodHandlerFactory());
    }

    private Handlers(Handlers that) {
        factories.addAll(that.factories);
    }

    /**
     * Registers the given handler factory with the registry.
     * @param factory the factory to register
     * @return this
     */
    public Handlers register(HandlerFactory factory) {
        factories.add(0, factory);
        return this;
    }

    public Optional<Handler> findFor(Class<?> sqlObjectType, Method method) {
        return factories.stream()
                .flatMap(factory -> JdbiOptionals.stream(factory.buildHandler(sqlObjectType, method)))
                .findFirst();
    }

    @Override
    public Handlers createCopy() {
        return new Handlers(this);
    }
}
