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
package org.jdbi.core.result;

/**
 * Callback for use with {@link ResultIterable#useIterator(IteratorConsumer)}
 */
@FunctionalInterface
public interface IteratorConsumer<T, X extends Exception> {
    /**
     * Will be invoked with result iterator. The iterator will be closed when this callback returns.
     *
     * @param iterator iterator to be used only within scope of this callback
     * @throws X optional exception thrown by the callback
     */
    void useIterator(ResultIterator<T> iterator) throws X;
}
