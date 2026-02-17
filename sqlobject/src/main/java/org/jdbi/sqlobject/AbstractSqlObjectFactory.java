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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jdbi.core.HandleCallback;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.ConfigCustomizerFactory;
import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.extension.ExtensionHandler;
import org.jdbi.core.extension.ExtensionHandlerCustomizer;
import org.jdbi.core.extension.ExtensionHandlerFactory;
import org.jdbi.core.extension.ExtensionMetadata;
import org.jdbi.core.internal.JdbiClassUtils;

abstract class AbstractSqlObjectFactory implements ExtensionFactory {

    @SuppressWarnings("unchecked")
    private static final ExtensionHandler WITH_HANDLE_HANDLER = (ExtensionHandler.Simple) (handleSupplier, args) ->
            ((HandleCallback<?, RuntimeException>) args[0]).withHandle(handleSupplier.getHandle());
    private static final ExtensionHandler GET_HANDLE_HANDLER = (ExtensionHandler.Simple) (handleSupplier, args) ->
            handleSupplier.getHandle();
    private static final Method GET_HANDLE_METHOD = JdbiClassUtils.methodLookup(SqlObject.class, "getHandle");
    private static final Method WITH_HANDLE_METHOD = JdbiClassUtils.methodLookup(SqlObject.class, "withHandle", HandleCallback.class);

    @Override
    public void buildExtensionMetadata(ExtensionMetadata.Builder builder) {
        final Class<?> extensionType = builder.getExtensionType();

        ExtensionHandler toStringHandler = (config, target) -> (handlerSupplier, args) ->
                "Jdbi sqlobject proxy for " + extensionType.getName() + "@" + Integer.toHexString(target.hashCode());
        builder.addMethodHandler(JdbiClassUtils.TOSTRING_METHOD, toStringHandler);
        builder.addMethodHandler(GET_HANDLE_METHOD, GET_HANDLE_HANDLER);
        builder.addMethodHandler(WITH_HANDLE_METHOD, WITH_HANDLE_HANDLER);

        DefinitionsFactory.configureDefinitions(extensionType, builder);
    }

    @Override
    public Collection<ExtensionHandlerCustomizer> getExtensionHandlerCustomizers(ConfigRegistry config) {
        final HandlerDecorators handlerDecorators = config.get(HandlerDecorators.class);
        return Collections.singleton(handlerDecorators::customize);
    }

    @Override
    public Collection<ExtensionHandlerFactory> getExtensionHandlerFactories(ConfigRegistry config) {
        final Handlers handlers = config.get(Handlers.class);
        List<ExtensionHandlerFactory> factories = new ArrayList<>();

        factories.add(new SqlMethodHandlerFactory());
        factories.addAll(handlers.getFactories());
        return Collections.unmodifiableList(factories);
    }

    @Override
    public Collection<ConfigCustomizerFactory> getConfigCustomizerFactories(ConfigRegistry config) {
        return Collections.singleton(SqlObjectCustomizerFactory.FACTORY);
    }

    static boolean isConcrete(Class<?> extensionTypeClass) {
        return extensionTypeClass.getAnnotation(GenerateSqlObject.class) != null;
    }
}
