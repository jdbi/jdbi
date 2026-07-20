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
package org.jdbi.core.config;

/**
 * Interface for classes that hold configuration. Implementations of this interface must have a public
 * constructor that optionally takes the {@link ConfigRegistry}.
 * <p>
 * Implementations must be immutable and thread-safe. A config value is shared across registries -- an unforked
 * copy-on-write child registry reads its parent's values by reference -- so it may be read concurrently from many
 * threads and must never be mutated in place: in-place mutation would corrupt the shared ancestor. Reconfiguration
 * produces a new instance (a "wither" returns a derived value instead of modifying the receiver), which
 * {@link Configurable#configure} installs into the target registry. Because values are immutable, a registry
 * shares them by reference rather than copying them, so there is no per-value copy hook.
 *
 * @param <This> A "This" type. Should always be the configuration class.
 */
public interface JdbiConfig<This extends JdbiConfig<This>> {}
