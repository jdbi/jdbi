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
package org.jdbi.v3.core.config;

/**
 * Interface for classes that hold configuration. Implementations of this interface must have a public
 * constructor that takes the {@link ConfigRegistry}.
 *
 * Implementors should ensure that implementations are thread-safe for access and caching purposes, but not
 * necessarily for reconfiguration.
 *
 * @param <This> A "This" type. Should always be the configuration class.
 */
public interface JdbiConfig<This extends JdbiConfig<This>> {
    /**
     * Returns a copy of this configuration object.
     * Changes to the copy should not modify the original, and vice-versa.
     *
     * @return a copy of this configuration object.
     */
    This createCopy();

    /**
     * The registry will inject itself into the configuration object.
     * This can be useful if you need to look up dependencies.
     * You will get a new registry after being copied.
     * @param registry the registry that owns this configuration object
     */
    default void setRegistry(ConfigRegistry registry) {}
}
