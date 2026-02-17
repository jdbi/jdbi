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

import org.jdbi.core.config.ConfigRegistry;

enum BuiltInExtensionHandler implements ExtensionHandler {
    /** Implementation for the {@link Object#equals(Object)} method. Each object using this handler is only equal to itself. */
    EQUALS_HANDLER {
        @SuppressWarnings("PMD.CompareObjectsWithEquals")
        @Override
        public AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
            return (handleSupplier, args) -> target == args[0];
        }
    },
    /** Implementation for the {@link Object#hashCode()} method. */
    HASHCODE_HANDLER {
        @Override
        public AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
            return (handleSupplier, args) -> System.identityHashCode(target);
        }
    },
    /** Handler that only returns null independent of any input parameters. */
    NULL_HANDLER {
        @Override
        public AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
            return (handleSupplier, args) -> null;
        }
    }
}
