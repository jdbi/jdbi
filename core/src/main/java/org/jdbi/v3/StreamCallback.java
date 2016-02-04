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
package org.jdbi.v3;

import java.util.stream.Stream;

/**
 * Callback for use with {@link org.jdbi.v3.ResultBearing#withStream(StreamCallback)}
 */
@FunctionalInterface
public interface StreamCallback<T, R> {
    /**
     * Will be invoked with a Stream&lt;T>. The stream will be closed when this callback returns. Any exception
     * thrown will be wrapped in a {@link org.jdbi.v3.exceptions.CallbackFailedException}
     *
     * @param stream stream to be used only within scope of this callback
     * @return The return value of the callback
     * @throws Exception will result in a {@link org.jdbi.v3.exceptions.CallbackFailedException} wrapping the exception
     *                   being thrown
     */
    R withStream(Stream<T> stream) throws Exception;
}
