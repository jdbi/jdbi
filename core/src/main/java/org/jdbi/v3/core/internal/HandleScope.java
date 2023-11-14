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
package org.jdbi.v3.core.internal;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.HandleSupplier;

/**
 * Jdbi manages Handles to allow transaction nesting and extension
 * objects to share the same handle as long as they are within a specific scope.
 * <br>
 * The default scope is "per thread", which is managed by the default implementation
 * of this interface.
 * <br>
 * It is possible to provide a different implementation by calling {@link Jdbi#setHandleScope(HandleScope)}
 * to support e.g. structured concurrency in modern Java or Kotlin coroutines.
 */
public interface HandleScope {

    static HandleScope threadLocal() {
        return new ThreadLocalHandleScope();
    }

    /**
     * Returns a {@link HandleSupplier} that provides a {@link org.jdbi.v3.core.Handle} in the given context.
     * @return A handle object or null.
     */
    HandleSupplier get();

    /**
     * Associate a {@link HandleSupplier} with the current scope.
     * @param handleSupplier A {@link HandleSupplier} object. Must not be null.
     */
    void set(HandleSupplier handleSupplier);

    /**
     * Remove a current {@link HandleSupplier association}. The {@link #get()} method will
     * return {@code null} after calling this method.
     */
    void clear();
}
