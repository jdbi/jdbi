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
package org.jdbi.v3.core;

/**
 * Allows listening to events on the {@link Handle} lifecycle.
 * <br>
 * {@link HandleListener} objects are stored in a collection class. To ensure correct operation, they should implement {@link Object#equals} and
 * {@link Object#hashCode()} to allow correct addition and deletion from the collection.
 */
public interface HandleListener {

    /**
     * A handle was created.
     *
     * @param handle The {@link Handle} object.
     */
    default void handleCreated(Handle handle) {}

    /**
     * A handle was closed. This method is only called once, even if the handle is closed multiple times.
     *
     * @param handle The {@link Handle} object.
     */
    default void handleClosed(Handle handle) {}
}
