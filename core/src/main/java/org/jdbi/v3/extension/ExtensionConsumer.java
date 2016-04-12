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

/**
 * Callback for use with {@link org.jdbi.v3.DBI#withExtension}.
 *
 * @param <E> extension type
 * @param <X> optional exception type thrown by the callback
 */
@FunctionalInterface
public interface ExtensionConsumer<E, X extends Exception> {
    /**
     * Will be invoked with a live extension. The associated handle will be closed when this callback returns.
     *
     * @param extension extension to be used only within the scope of this callback.
     * @throws X optional exception thrown by this callback.
     */
    void useExtension(E extension) throws X;
}
