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
package org.jdbi.v3.core.config.internal;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Mark a Jdbi object which should have a {@code ConfigRegistry} injected before use.
 */
public interface Injected {
    /**
     * The registry will inject itself into the configuration object.
     * This can be useful if you need to look up dependencies.
     * You will get a new registry after being copied.
     * @param registry the registry that owns this configuration object
     */
    default void setRegistry(ConfigRegistry registry) {}
}
