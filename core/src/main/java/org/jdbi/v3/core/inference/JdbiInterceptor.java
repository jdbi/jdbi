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
package org.jdbi.v3.core.inference;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Generic interface to allow transformation operation interception. Used to manage the various inferred data types that may need special treatment.
 *
 * @param <S> A generic source type.
 * @param <T> A generic destination type.
 */
@FunctionalInterface
public interface JdbiInterceptor<S, T> {

    /**
     * Intercept the source type and return the destination type.
     *
     * @param source A source object.
     * @return The destination type.
     */
    @CheckForNull
    T intercept(@Nullable S source);

    /**
     * Returns true if this interceptor can handle the source object.
     *
     * @param source A source object.
     * @return True when the interceptor can handle the source object.
     */
    default boolean accept(@Nullable S source) {
        return true;
    }
}
