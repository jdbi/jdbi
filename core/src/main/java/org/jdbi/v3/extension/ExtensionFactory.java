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
package org.jdbi.v3.extension;

import java.util.Optional;

import org.jdbi.v3.Handle;

/**
 * Factory interface used to produce JDBI extension objects.
 *
 * @param <C> extension config class
 */
public interface ExtensionFactory<C extends ExtensionConfig<C>> {
    /**
     * Returns a new default configuration for this extension factory.
     */
    C createConfig();

    /**
     * Returns an instance of the given extension type, if the factory supports it; empty otherwise.
     *
     * @param extensionType the type of the extension. Depending on the situation this may be a generic type such as
     *                      {@link java.lang.reflect.ParameterizedType} or {@link Class}.
     * @param config        the extension configuration.
     * @param handle        the database handle.
     */
    <T> Optional<T> attach(Class<T> extensionType, C config, Handle handle);
}
