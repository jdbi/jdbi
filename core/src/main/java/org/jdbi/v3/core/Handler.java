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

public interface Handler {
    Handler STANDARD_HANDLER = new Handler() {
        @Override
        public <R, X extends Exception> HandleCallback<R, X> decorate(HandleCallback<R, X> callback) {
            return callback;
        }
    };

    /**
     * Decorate the given Handle callback
     *
     * @param callback a callback which will receive the open handle
     *
     * @return the callback decorated as needed
     *
     * @see Jdbi#withHandle(HandleCallback)
     */
    <R, X extends Exception> HandleCallback<R, X> decorate(HandleCallback<R, X> callback);
}
