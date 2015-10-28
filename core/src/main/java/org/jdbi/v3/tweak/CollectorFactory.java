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
package org.jdbi.v3.tweak;

import java.util.stream.Collector;

/**
 * Factory for building containers of elements.
 * The collector produces only objects of the type of the container elements.
 *
 * @param <T> the type of the container elements
 * @param <R> the type of the container
 */
public interface CollectorFactory<T, R> {

    /**
     * Whether the corresponding collector produces results of the given type.
     *
     * @param type the object type
     * @return {@code true}, if accepts, otherwise {@code false}
     */
    boolean accepts(Class<?> type);

    /**
     * Builds a new {@link Collector}.
     *
     * @param type the actual type of the container
     * @return the {@link Collector}
     */
    Collector<T, ?, R> newCollector(Class<R> type);
}
