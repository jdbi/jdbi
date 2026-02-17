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
package org.jdbi.core.extension;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.jdbi.core.config.ConfigCustomizer;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.meta.Alpha;

/**
 * Factory interface used to produce Jdbi extension objects. A factory can provide additional
 * pieces of information by overriding the various default methods.
 */
@FunctionalInterface
public interface ExtensionFactory {

    /**
     * Flags that the factory can return to control aspects of the extension framework.
     *
     * @since 3.38.0
     */
    @Alpha
    enum FactoryFlag {
        /**
         * The factory provides a concrete instance to back the extension type.
         * <br>
         * Unless this flag is present, factories do not
         * create an object to attach to but register method handlers for every method on an extension type.
         * <br>
         * E.g. the SQLObject handler is an example of a virtual factory that processes every method in an interface class without requiring
         * an implementation of the extension type.
         * The extension framework will execute the method handlers and pass in a proxy object instead of an underlying instance.
         * <br>
         * When this flag is present, the {@link ExtensionFactory#attach(Class, HandleSupplier)} method will never be called.
         */
        NON_VIRTUAL_FACTORY,
        /**
         * Do not wrap the backing object methods into {@link ExtensionHandler} instances and return a
         * {@link java.lang.reflect.Proxy} instance but return it as is. This allows the factory to
         * suport class objects as well as interfaces.
         * <br>
         * This is a corner use case and should normally not be used by any standard extension.
         * <br>
         * Legacy extension factories that need every method on an interface forwarded to the underlying implementation class
         * can set this flag to bypass the proxy logic of the extension framework.
         */
        DONT_USE_PROXY
    }

    /**
     * Returns true if the factory can process the given extension type.
     *
     * @param extensionType the extension type
     * @return whether the factory can produce an extension of the given type
     */
    boolean accepts(Class<?> extensionType);

    /**
     * Attaches an extension type. This method is not called if {@link #getFactoryFlags()} contains {@link FactoryFlag#NON_VIRTUAL_FACTORY}.
     *
     * @param extensionType  The extension type
     * @param handleSupplier Supplies the database handle. This supplier may lazily open a Handle on the first
     *                       invocation. Extension implementors should take care not to fetch the handle before it is
     *                       needed, to avoid opening handles unnecessarily
     * @param <E>            the extension type
     * @return An extension of the given type, attached to the given handle
     * @throws IllegalArgumentException if the extension type is not supported by this factory
     * @see org.jdbi.core.Jdbi#onDemand(Class)
     */
    default <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
        throw new UnsupportedOperationException("Virtual factories do not support attach()");
    }

    /**
     * Returns a collection of {@link ExtensionHandlerFactory} objects. These factories are used in
     * addition to the factories that have been registered with {@link Extensions#registerHandlerFactory}.
     * <br>
     * Handler factories returned here can customize the behavior of the Extension factory itself.
     *
     * @param config A Configuration registry object that can be used to look up additional information
     * @return A collection of {@link ExtensionHandlerFactory} objects. Can be empty, must not be null
     *
     * @since 3.38.0
     */
    @Alpha
    default Collection<ExtensionHandlerFactory> getExtensionHandlerFactories(ConfigRegistry config) {
        return Collections.emptySet();
    }

    /**
     * Returns a collection of {@link ExtensionHandlerCustomizer} objects. These customizers are used
     * in addition to the customizers that have been registered with {@link Extensions#registerHandlerCustomizer}.
     * <br>
     * Handler customizers returned here can customize the behavior of the Handlers returned by the handler factories.
     *
     * @param config A Configuration registry object that can be used to look up additional information
     * @return A collection of {@link ExtensionHandlerCustomizer} objects. Can be empty, must not be null
     *
     * @since 3.38.0
     */
    @Alpha
    default Collection<ExtensionHandlerCustomizer> getExtensionHandlerCustomizers(ConfigRegistry config) {
        return Collections.emptySet();
    }

    /**
     * Returns a collection of {@link ConfigCustomizerFactory} objects.
     * <br>
     * Each factory is called once for every type that is attached by the factory and once for each method in the
     * type. They can return {@link ConfigCustomizer} instances that will affect the specific configuration for
     * each method and extension type.
     *
     * @param config A Configuration registry object that can be used to look up additional information
     * @return A collection of {@link ConfigCustomizerFactory} objects. Can be empty, must not be null
     *
     * @since 3.38.0
     */
    @Alpha
    default Collection<ConfigCustomizerFactory> getConfigCustomizerFactories(ConfigRegistry config) {
        return Collections.emptySet();
    }

    /**
     * Receives the {@link ExtensionMetadata.Builder} when the {@link ExtensionMetadata} object for this extension
     * is created. The factory can add additional method handlers or specific instance and method customizers as needed.
     * <br/>
     * Code here can call methods on the builder to configure the metadata object.
     *
     * @param builder The builder object that is used to create the {@link ExtensionMetadata} object
     *
     * @since 3.38.0
     */
    @Alpha
    default void buildExtensionMetadata(ExtensionMetadata.Builder builder) {}

    /**
     * Returns a set of {@link FactoryFlag}s that describe the extension factory.
     *
     * @return A set of {@link FactoryFlag} elements. Default is the empty set
     *
     * @since 3.38.0
     */
    @Alpha
    default Set<FactoryFlag> getFactoryFlags() {
        return Collections.emptySet();
    }
}
