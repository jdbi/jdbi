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
package org.jdbi.v3.core.mapper.reflect.internal;

import org.jdbi.v3.core.mapper.Nested;
import java.util.Optional;

/**
 * A function that can post-process a row mapper field.
 * This is specifically used to post process fields annotated with {@link Nested}.
 *
 * @param <T> the type of the object being processed
 * @param <R> the type of the result
 */
public interface RowMapperFieldPostProcessor<T, R> {
    /**
     * Process the object
     * @param object the object to process
     */
    R process(T object);

    /**
     * Keep the object as is
     */
    static <T> RowMapperFieldPostProcessor<T, T> noPostProcessing() {
        return (object) -> object;
    }

    /**
     * Wrap the object in an {@link Optional}, returning {@link Optional#empty()} if all parameters are null or empty
     */
    static <T> RowMapperFieldPostProcessor<T, Optional<T>> wrapNestedOptional() {
        return (object) -> Optional.ofNullable(object);
    }
}
