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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jdbi.v3.core.config.ConfigCustomizer;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.internal.ConfigCustomizerChain;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.meta.Alpha;

/**
 * Metadata that was detected when analyzing an extension class before attaching.
 * Represents a resolved extension type with all config customizers and method handlers.
 *
 * @since 3.38.0
 */
@Alpha
public final class ExtensionMetadata {

    private final Class<?> extensionType;
    private final ConfigCustomizer instanceConfigCustomizer;
    private final Map<Method, ? extends ConfigCustomizer> methodConfigCustomizers;
    private final Map<Method, ExtensionHandler> methodHandlers;

    /**
     * Returns a new {@link ExtensionMetadata.Builder} instance.
     * @param extensionType The extension type for which metadata is collected
     * @return A new {@link ExtensionMetadata.Builder} instance
     */
    public static ExtensionMetadata.Builder builder(Class<?> extensionType) {
        return new Builder(extensionType);
    }

    private ExtensionMetadata(
            Class<?> extensionType,
            ConfigCustomizer instanceConfigCustomizer,
            Map<Method, ? extends ConfigCustomizer> methodConfigCustomizers,
            Map<Method, ExtensionHandler> methodHandlers) {
        this.extensionType = extensionType;
        this.instanceConfigCustomizer = instanceConfigCustomizer;
        this.methodConfigCustomizers = Collections.unmodifiableMap(methodConfigCustomizers);
        this.methodHandlers = Collections.unmodifiableMap(methodHandlers);
    }

    public Class<?> extensionType() {
        return extensionType;
    }

    /**
     * Create an instance specific configuration based on all instance customizers. The instance configuration holds all
     * custom configuration that was applied e.g. through instance annotations.
     *
     * @param config A configuration object. The object is not changed
     * @return A new configuration object with all changes applied
     */
    public ConfigRegistry createInstanceConfiguration(ConfigRegistry config) {
        ConfigRegistry instanceConfiguration = config.createCopy();
        instanceConfigCustomizer.customize(instanceConfiguration);
        return instanceConfiguration;
    }

    /**
     * Create an method specific configuration based on all method customizers. The method configuration holds all
     * custom configuration that was applied e.g. through method annotations.
     *
     * @param method The method that is about to be called
     * @param config A configuration object. The object is not changed
     * @return A new configuration object with all changes applied
     */
    public ConfigRegistry createMethodConfiguration(Method method, ConfigRegistry config) {
        ConfigRegistry methodConfiguration = config.createCopy();
        ConfigCustomizer methodConfigCustomizer = methodConfigCustomizers.get(method);
        if (methodConfigCustomizer != null) {
            methodConfigCustomizer.customize(methodConfiguration);
        }
        return methodConfiguration;
    }

    /**
     * Returns a set of all Methods that have {@link ExtensionHandler} objects associated with them.
     */
    public Set<Method> getExtensionMethods() {
        return methodHandlers.keySet();
    }

    /**
     * Creates an {@link ExtensionHandlerInvoker} instance for a specific method.
     * @param target The target object on which the invoker should work
     * @param method The method which will trigger the invocation
     * @param handleSupplier A {@link HandleSupplier} that will provide the handle object for the extension method
     * @param config The configuration object which should be used as base for the method specific configuration
     * @return A {@link ExtensionHandlerInvoker} object that is linked to the method
     * @param <E> THe type of the target object
     */
    public <E> ExtensionHandlerInvoker createExtensionHandlerInvoker(E target, Method method,
            HandleSupplier handleSupplier, ConfigRegistry config) {
        return new ExtensionHandlerInvoker(target, method, methodHandlers.get(method), handleSupplier, config);
    }

    /**
     * Builder class for the {@link ExtensionMetadata} object.
     * See {@link ExtensionMetadata#builder(Class)}.
     */
    public static final class Builder {

        private final Class<?> extensionType;
        private final Collection<ExtensionHandlerFactory> extensionHandlerFactories = new ArrayList<>();
        private final Collection<ExtensionHandlerCustomizer> extensionHandlerCustomizers = new ArrayList<>();
        private final Collection<ConfigCustomizerFactory> configCustomizerFactories = new ArrayList<>();

        private final ConfigCustomizerChain instanceConfigCustomizer = new ConfigCustomizerChain();
        private final Map<Method, ConfigCustomizerChain> methodConfigCustomizers = new HashMap<>();
        private final Map<Method, ExtensionHandler> methodHandlers = new HashMap<>();

        private final Collection<Method> extensionTypeMethods = new HashSet<>();

        Builder(Class<?> extensionType) {
            this.extensionType = extensionType;

            this.extensionTypeMethods.addAll(Arrays.asList(extensionType.getMethods()));
            this.extensionTypeMethods.addAll(Arrays.asList(extensionType.getDeclaredMethods()));

            this.extensionTypeMethods.stream()
                    .filter(m -> !m.isSynthetic())
                    .collect(Collectors.groupingBy(m -> Arrays.asList(m.getName(), Arrays.asList(m.getParameterTypes()))))
                    .values()
                    .stream()
                    .filter(methodCount -> methodCount.size() > 1)
                    .findAny()
                    .ifPresent(methods -> {
                        throw new UnableToCreateExtensionException("%s has ambiguous methods (%s) found, please resolve with an explicit override",
                                extensionType, methods);
                    });
        }

        /**
         * Adds an {@link ExtensionHandlerFactory} that will be used to find extension handlers when the {@link Builder#build()}} method is called.
         * @param extensionHandlerFactory An {@link ExtensionHandlerFactory} instance
         * @return The builder instance
         */
        public Builder addExtensionHandlerFactory(ExtensionHandlerFactory extensionHandlerFactory) {
            this.extensionHandlerFactories.add(extensionHandlerFactory);
            return this;
        }

        /**
         * Adds an {@link ExtensionHandlerCustomizer} that will be used to customize extension handlers when the {@link Builder#build()}} method is called.
         * @param extensionHandlerCustomizer An {@link ExtensionHandlerCustomizer} instance
         * @return The builder instance
         */
        public Builder addExtensionHandlerCustomizer(ExtensionHandlerCustomizer extensionHandlerCustomizer) {
            this.extensionHandlerCustomizers.add(extensionHandlerCustomizer);
            return this;
        }

        /**
         * Adds an {@link ConfigCustomizerFactory} that will be used to find configuration customizers when the {@link Builder#build()}} method is called.
         * @param configCustomizerFactory An {@link ConfigCustomizerFactory} instance
         * @return The builder instance
         */
        public Builder addConfigCustomizerFactory(ConfigCustomizerFactory configCustomizerFactory) {
            this.configCustomizerFactories.add(configCustomizerFactory);
            return this;
        }

        /**
         * Add an instance specific configuration customizer. This customizer will be applied to all methods on the extension type.
         * @param configCustomizer A {@link ConfigCustomizer}
         * @return The builder instance
         */
        public Builder addInstanceConfigCustomizer(ConfigCustomizer configCustomizer) {
            instanceConfigCustomizer.addCustomizer(configCustomizer);
            return this;
        }

        /**
         * Add a method specific configuration customizer. This customizer will be applied only to the method given here.
         * @param method A method object
         * @param configCustomizer A {@link ConfigCustomizer}
         * @return The builder instance
         */
        public Builder addMethodConfigCustomizer(Method method, ConfigCustomizer configCustomizer) {
            ConfigCustomizerChain methodConfigCustomizer = methodConfigCustomizers.computeIfAbsent(method, m -> new ConfigCustomizerChain());
            methodConfigCustomizer.addCustomizer(configCustomizer);
            return this;
        }

        /**
         * Adds a new extension handler for a method.
         *
         * @param method The method for which an extension handler should be registered.
         * @param handler An {@link ExtensionHandler} instance
         * @return The builder instance
         */
        public Builder addMethodHandler(Method method, ExtensionHandler handler) {
            methodHandlers.put(method, handler);
            return this;
        }

        /**
         * Returns the extension type from the builder.
         * @return The extension type
         */
        public Class<?> getExtensionType() {
            return extensionType;
        }

        /**
         * Creates a new {@link ExtensionMetadata} object.
         *
         * @return A {@link ExtensionMetadata} object
         */
        public ExtensionMetadata build() {
            // add all methods that are declared on the extension type and
            // are not static and don't already have a handler

            final Set<Method> seen = new HashSet<>(methodHandlers.keySet());
            for (Method method : extensionTypeMethods) {
                // skip static methods and methods that already have method handlers
                if (Modifier.isStatic(method.getModifiers()) || !seen.add(method)) {
                    continue;
                }

                // look through the registered extension handler factories to find extension handlers
                ExtensionHandler handler = findExtensionHandlerFor(extensionType, method)
                        .orElseGet(() -> ExtensionHandler.missingExtensionHandler(method));


                // apply extension handler customizers
                for (ExtensionHandlerCustomizer extensionHandlerCustomizer : extensionHandlerCustomizers) {
                    handler = extensionHandlerCustomizer.customize(handler, extensionType, method);
                }

                methodHandlers.put(method, handler);
            }

            configCustomizerFactories.forEach(configCustomizerFactory -> configCustomizerFactory.forExtensionType(extensionType)
                    .forEach(this::addInstanceConfigCustomizer));

            for (Method method : methodHandlers.keySet()) {
                // call all method configurer factories.
                configCustomizerFactories.forEach(configCustomizerFactory ->
                        configCustomizerFactory.forExtensionMethod(extensionType, method)
                                .forEach(configCustomizer -> this.addMethodConfigCustomizer(method, configCustomizer)));
            }

            return new ExtensionMetadata(extensionType, instanceConfigCustomizer, methodConfigCustomizers, methodHandlers);
        }

        private Optional<ExtensionHandler> findExtensionHandlerFor(Class<?> extensionType, Method method) {
            for (ExtensionHandlerFactory extensionHandlerFactory : extensionHandlerFactories) {
                if (extensionHandlerFactory.accepts(extensionType, method)) {
                    Optional<ExtensionHandler> result = extensionHandlerFactory.createExtensionHandler(extensionType, method);
                    if (result.isPresent()) {
                        return result;
                    }
                }
            }
            return Optional.empty();
        }
    }

    /**
     * Wraps all config customizers and the handler for a specific method execution.
     * An invoker is created using {@link ExtensionMetadata#createExtensionHandlerInvoker(Object, Method, HandleSupplier, ConfigRegistry)}.
     */
    public final class ExtensionHandlerInvoker {

        private final Object target;
        private final HandleSupplier handleSupplier;
        private final ExtensionContext extensionContext;
        private final ExtensionHandler extensionHandler;

        ExtensionHandlerInvoker(Object target, Method method, ExtensionHandler extensionHandler, HandleSupplier handleSupplier, ConfigRegistry config) {
            this.target = target;
            this.handleSupplier = handleSupplier;
            ConfigRegistry methodConfig = createMethodConfiguration(method, config);
            this.extensionContext = ExtensionContext.forExtensionMethod(methodConfig, extensionType, method);

            this.extensionHandler = extensionHandler;
            this.extensionHandler.warm(methodConfig);
        }

        /**
         * Invoke the registered extension handler code in the extension context. The
         * extension context wraps the method that gets executed and a full customized configuration
         * (both instance and method specific configuration customizers have been applied). The
         * extension context is registered with the underlying handle to configure the handle when
         * executing the registered {@link ExtensionHandler}.
         *
         * @param args The arguments to pass into the extension handler
         * @return The result of the extension handler invocation
         */
        public Object invoke(Object... args) {
            final Object[] handlerArgs = JdbiClassUtils.safeVarargs(args);
            final Callable<Object> callable = () -> extensionHandler.invoke(handleSupplier, target, handlerArgs);
            return call(callable);
        }

        /**
         * Invoke a callable in the extension context. The extension context wraps the method that
         * gets executed and a full customized configuration (both instance and method specific
         * configuration customizers have been applied). The extension context is registered with
         * the underlying handle to configure the handle when calling the {@link Callable#call()} method.
         * <br>
         * This method is used by the generated classes from the <code>jdbi3-generator</code> annotation
         * processor to execute predefined {@link ExtensionHandler} instances.
         *
         * @param callable The callable to use
         * @return The result of the extension handler invocation
         */
        public Object call(Callable<?> callable) {
            try {
                return handleSupplier.invokeInContext(extensionContext, callable);
            } catch (Exception x) {
                throw Sneaky.throwAnyway(x);
            }
        }

        /**
         * Invoke a runnable in the extension context. The extension context wraps the method that
         * gets executed and a full customized configuration (both instance and method specific
         * configuration customizers have been applied). The extension context is registered with
         * the underlying handle to configure the handle when calling the {@link Runnable#run()} method.
         * <br>
         * This method is used by the generated classes from the <code>jdbi3-generator</code> annotation
         * processor to execute predefined {@link ExtensionHandler} instances.
         *
         * @param runnable The runnable to use
         */
        public void call(Runnable runnable) {
            call(() -> {
                runnable.run();
                return null;
            });
        }
    }
}
