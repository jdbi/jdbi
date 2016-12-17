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

/**
 * Factory interface used to produce JDBI extension objects.
 */
public interface ExtensionFactory {
    /**
     * @param extensionType the extension type
     *
     * @return whether the factory can produce an extension of the given type
     */
    boolean accepts(Class<?> extensionType);

    /**
     * @param extensionType the extension type.
     * @param handle        Supplies the database handle. This supplier may lazily open a Handle on the first
     *                      invocation. Extension implementors should take care not to fetch the handle before it is
     *                      needed, to avoid opening handles unnecessarily.
     * @param <E> the extension type
     *
     * @return an extension of the given type, attached to the given handle.
     * @throws IllegalArgumentException if the extension type is not supported by this factory.
     * @see org.jdbi.v3.core.Jdbi#onDemand(Class)
     */
    <E> E attach(Class<E> extensionType, HandleSupplier handle);
}
