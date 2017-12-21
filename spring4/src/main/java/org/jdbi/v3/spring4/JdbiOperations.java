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
package org.jdbi.v3.spring4;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;

/**
 * similar to {@link JdbiOperations} this class defines the basic Jdbi operations that should be used in a Spring environment.
 * <p>
 * implemented by {@link JdbiTemplate}
 */
public interface JdbiOperations {

    /**
     * like {@link Jdbi#withHandle(HandleCallback)} but respects springs transactions
     */
    <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X;

    /**
     * like {@link Jdbi#useHandle(HandleConsumer)} but respects springs transactions
     */
    default <X extends Exception> void useHandle(HandleConsumer<X> callback) throws X {
        withHandle(h -> {
            callback.useHandle(h);
            return null;
        });
    }

}
