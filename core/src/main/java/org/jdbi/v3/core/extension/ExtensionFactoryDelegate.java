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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionHandler.ExtensionHandlerFactory;
import org.jdbi.v3.core.extension.ExtensionMetadata.Builder;
import org.jdbi.v3.core.extension.ExtensionMetadata.ExtensionHandlerInvoker;
import org.jdbi.v3.core.internal.JdbiClassUtils;

import static java.lang.String.format;

import static org.jdbi.v3.core.extension.ExtensionFactory.FactoryFlag.CLASSES_ARE_SUPPORTED;
import static org.jdbi.v3.core.extension.ExtensionFactory.FactoryFlag.VIRTUAL_FACTORY;
import static org.jdbi.v3.core.extension.ExtensionHandler.EQUALS_HANDLER;
import static org.jdbi.v3.core.extension.ExtensionHandler.HASHCODE_HANDLER;
import static org.jdbi.v3.core.extension.ExtensionHandler.NULL_HANDLER;

final class ExtensionFactoryDelegate implements ExtensionFactory {

    private final ExtensionFactory delegatedFactory;

    ExtensionFactoryDelegate(ExtensionFactory delegatedFactory) {
        this.delegatedFactory = delegatedFactory;
    }

    @Override
    public boolean accepts(Class<?> extensionType) {
        return delegatedFactory.accepts(extensionType);
    }

    ExtensionFactory getDelegatedFactory() {
        return delegatedFactory;
    }

    @Override
    public Collection<ExtensionHandlerFactory> getExtensionHandlerFactories(ConfigRegistry config) {
        return delegatedFactory.getExtensionHandlerFactories(config);
    }

    @Override
    public Collection<ExtensionHandlerCustomizer> getExtensionHandlerCustomizers(ConfigRegistry config) {
        return delegatedFactory.getExtensionHandlerCustomizers(config);
    }

    @Override
    public Collection<ConfigCustomizerFactory> getConfigCustomizerFactories(ConfigRegistry config) {
        return delegatedFactory.getConfigCustomizerFactories(config);
    }

    @Override
    public void buildExtensionInitData(Builder builder) {
        delegatedFactory.buildExtensionInitData(builder);
    }

    @Override
    public Set<FactoryFlag> getFactoryFlags() {
        return delegatedFactory.getFactoryFlags();
    }

    @Override
    public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {

        Set<FactoryFlag> factoryFlags = getFactoryFlags();

        // If the extension declares that it supports classes, then the proxy logic
        // in the delegate is bypassed. This code uses the Java proxy class which does not
        // work for Classes (when extending a class, the assumption is that the returned
        // object can be cast to the class itself, something that can not be done with a
        // java proxy object).
        //
        // The extension factory is now responsible for managing the method invocations itself.
        //
        if (factoryFlags.contains(CLASSES_ARE_SUPPORTED)) {
            return delegatedFactory.attach(extensionType, handleSupplier);
        }

        if (extensionType == null || !extensionType.isInterface()) {
            throw new IllegalArgumentException(format("Can not attach %s as an extension with %s",
                    extensionType, delegatedFactory.getClass().getSimpleName()));
        }

        final ConfigRegistry config = handleSupplier.getConfig();
        final Extensions extensions = config.get(Extensions.class);

        extensions.onCreateProxy();

        final ExtensionMetadata extensionMetaData = extensions.findMetadata(extensionType, config, delegatedFactory);
        final ConfigRegistry instanceConfig = extensionMetaData.createInstanceConfiguration(config);

        Map<Method, ExtensionHandlerInvoker> handlers = new HashMap<>();
        final Object proxy = Proxy.newProxyInstance(
                extensionType.getClassLoader(),
                new Class[] {extensionType},
                (proxyInstance, method, args) -> handlers.get(method).invoke(args));

        // if the object created by the delegated factory has actual methods (it is not delegating), attach the
        // delegate and pass it to the handlers. Otherwise assume that there is no backing object and do not call
        // attach.
        final Object delegatedInstance = factoryFlags.contains(VIRTUAL_FACTORY) ? proxy : delegatedFactory.attach(extensionType, handleSupplier);

        // add proxy specific methods (toString, equals, hashCode, finalize)
        // those will only be added if they don't already exist in the method handler map.

        // If these methods are added, they are special because they operate on the proxy object itself, not the underlying object
        checkMethodPresent(extensionType, Object.class, "toString").ifPresent(method -> {
            ExtensionHandler toStringHandler = (h, target, args) ->
                    "Jdbi extension proxy for " + extensionType.getName() + "@" + Integer.toHexString(proxy.hashCode());
            handlers.put(method, extensionMetaData.new ExtensionHandlerInvoker(proxy, method, toStringHandler, handleSupplier, instanceConfig));
        });

        checkMethodPresent(extensionType, Object.class, "equals", Object.class).ifPresent(method -> handlers.put(method,
                extensionMetaData.new ExtensionHandlerInvoker(proxy, method, EQUALS_HANDLER, handleSupplier, instanceConfig)));
        checkMethodPresent(extensionType, Object.class, "hashCode").ifPresent(method -> handlers.put(method,
                extensionMetaData.new ExtensionHandlerInvoker(proxy, method, HASHCODE_HANDLER, handleSupplier, instanceConfig)));

        // add all methods that are delegated to the underlying object / existing handlers
        extensionMetaData.getExtensionMethods().forEach(method ->
                handlers.put(method, extensionMetaData.createExtensionHandlerInvoker(delegatedInstance, method, handleSupplier, instanceConfig)));

        // finalize is double special. Add this unconditionally, even if subclasses try to override it.
        JdbiClassUtils.safeMethodLookup(extensionType, "finalize").ifPresent(method -> handlers.put(method,
                extensionMetaData.new ExtensionHandlerInvoker(proxy, method, NULL_HANDLER, handleSupplier, instanceConfig)));


        return extensionType.cast(proxy);
    }

    /** returns Optional.empty() if the method exists in the extension type, otherwise a fallback method. */
    private Optional<Method> checkMethodPresent(Class<?> extensionType, Class<?> klass, String methodName, Class<?>... parameterTypes) {
        Optional<Method> method = JdbiClassUtils.safeMethodLookup(extensionType, methodName, parameterTypes);
        if (method.isPresent()) {
            // does the method actually exist in the type itself (e.g. overridden by the implementation class?)
            // if yes, return absent, so the default hander is not added.
            return Optional.empty();
        } else {
            return JdbiClassUtils.safeMethodLookup(klass, methodName, parameterTypes);
        }
    }

    @Override
    public String toString() {
        return "ExtensionFactoryDelegate for " + delegatedFactory.toString();
    }
}
