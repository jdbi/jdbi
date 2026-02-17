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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.jdbi.core.config.ConfigCustomizer;
import org.jdbi.meta.Alpha;

/**
 * Factory interface to create collections of {@link ConfigCustomizer} instances.
 *
 * @since 3.38.0
 */
@Alpha
public interface ConfigCustomizerFactory {

    /**
     * Creates a collection of {@link ConfigCustomizer} instances for an extension type.
     *
     * @param extensionType The extension type
     * @return A {@link Collection} of {@link ConfigCustomizer} objects. Must not be null
     */
    default Collection<ConfigCustomizer> forExtensionType(Class<?> extensionType) {
        return Collections.emptyList();
    }

    /**
     * Creates a collection of {@link ConfigCustomizer} instances for an extension type method.
     *
     * @param extensionType The extension type
     * @param method        The method on the extension type
     * @return A {@link Collection} of {@link ConfigCustomizer} objects. Must not be null
     */
    default Collection<ConfigCustomizer> forExtensionMethod(Class<?> extensionType, Method method) {
        return Collections.emptyList();
    }
}
