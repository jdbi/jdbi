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
import java.util.Collection;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionMetadata;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.meta.Alpha;

/**
 * Connects a {@link GenerateSqlObject} type to the classes that the Jdbi generator created for it.
 * <p>
 * The generator writes an implementation of this interface next to each generated class and registers it
 * through {@link java.util.ServiceLoader}. Jdbi uses the provider to create instances and to learn the
 * extension methods without reflection, so generated SQL objects work in a GraalVM native image without
 * reachability metadata. Application code does not implement this interface.
 *
 * @since 3.55.0
 */
@Alpha
public interface GeneratedSqlObjectProvider {

    /**
     * Returns the extension type that this provider serves.
     *
     * @return The extension type, annotated with {@link GenerateSqlObject}
     */
    Class<?> extensionType();

    /**
     * Returns the methods of the extension type that the generated class implements.
     *
     * @return The extension methods
     */
    Collection<Method> extensionMethods();

    /**
     * Creates a new instance of the generated class, bound to a handle.
     *
     * @param extensionMetadata The metadata for the extension type
     * @param handleSupplier    The handle supplier for the new instance
     * @param config            The configuration for the new instance
     * @return A new instance of the extension type
     */
    Object createInstance(ExtensionMetadata extensionMetadata, HandleSupplier handleSupplier, ConfigRegistry config);

    /**
     * Creates a new on-demand instance of the extension type.
     *
     * @param jdbi The {@link Jdbi} instance that supplies handles
     * @return A new on-demand instance of the extension type
     */
    Object createOnDemand(Jdbi jdbi);
}
