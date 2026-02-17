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
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.extension.ExtensionHandler;

/**
 * Registry for {@link HandlerDecorator handler decorators}. Decorators may modify or augment the behavior of a method
 * Handler in some way. Out of the box, a decorator is registered which applies decorations for any
 * {@link SqlMethodDecoratingAnnotation decorating annotations} present on a SQL method. For example, using the
 * {@link org.jdbi.sqlobject.transaction.Transaction} annotation will cause a SQL method to be executed within a
 * transaction. Decorators are applied in the order registered, from innermost to outermost: the last registered
 * decorator will be the outermost decorator around the method handler.
 *
 * @deprecated Use {@link org.jdbi.core.extension.ExtensionHandlerCustomizer} instances which are returned directly
 * from the {@link org.jdbi.core.extension.ExtensionFactory#getExtensionHandlerCustomizers(ConfigRegistry)}.
 */
@Deprecated(since = "3.38.0", forRemoval = true)
public class HandlerDecorators implements JdbiConfig<HandlerDecorators> {
    private final List<HandlerDecorator> decorators;

    public HandlerDecorators() {
        decorators = new CopyOnWriteArrayList<>();
        register(new SqlMethodAnnotatedHandlerDecorator());
    }

    private HandlerDecorators(HandlerDecorators that) {
        decorators = new CopyOnWriteArrayList<>(that.decorators);
    }

    /**
     * Registers the given handler decorator with the registry.
     *
     * @param decorator the decorator to register
     * @return this
     */
    public HandlerDecorators register(HandlerDecorator decorator) {
        decorators.add(decorator);
        return this;
    }

    /**
     * Applies all registered decorators to the given handler
     *
     * @param base          the base handler
     * @param sqlObjectType the SQL object type
     * @param method        the SQL method to be decorated
     * @return the decorated handler
     */
    public Handler applyDecorators(Handler base, Class<?> sqlObjectType, Method method) {
        Handler handler = base;
        for (HandlerDecorator decorator : decorators) {
            handler = decorator.decorateHandler(handler, sqlObjectType, method);
        }
        return handler;
    }

    ExtensionHandler customize(ExtensionHandler base, Class<?> sqlObjectType, Method method) {
        ExtensionHandler handler = base;
        for (HandlerDecorator decorator : decorators) {
            handler = decorator.customize(handler, sqlObjectType, method);
        }
        return handler;
    }

    @Override
    public HandlerDecorators createCopy() {
        return new HandlerDecorators(this);
    }
}
