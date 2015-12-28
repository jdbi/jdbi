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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface ResultBearing<T> extends Iterable<T>
{
    @Override
    ResultIterator<T> iterator();

    /**
     * Get the only row in the result set.
     * @throws IllegalStateException if zero or multiple rows are returned
     * @return the object mapped from the singular row in the results
     */
    default T findOnly() {
        try (ResultIterator<T> iter = iterator()) {
            if (!iter.hasNext()) {
                throw new IllegalStateException("No element found in 'only'");
            }

            final T r = iter.next();

            if (iter.hasNext()) {
                throw new IllegalStateException("Multiple elements found in 'only'");
            }

            return r;
        }
    }

    /**
     * Get the first row in the result set, if present.
     */
    default Optional<T> findFirst() {
        try (ResultIterator<T> iter = iterator()) {
            return iter.hasNext() ? Optional.of(iter.next()) : Optional.empty();
        }
    }

    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default List<T> list() {
        return collect(Collectors.toList());
    }

    /**
     * Collect the query results into a container specified by a collector.
     *
     * @param collector       the collector
     * @param <R>             the generic type of the container
     * @param <A>             the generic type of the container builder
     * @return the container with the query result
     */
    default <R> R collect(Collector<T, ?, R> collector) {
        try (Stream<T> stream = stream()) {
            return stream.collect(collector);
        }
    }

    /**
     * Collect the query result into a container of the type specified by the given class.
     * A factory for building the container should be registered in the query's
     * collector factory registry.
     *
     * @param containerType the generic type that represents the container
     * @param <R> the generic type of the container
     * @return the container with the query result
     */
    <R> R collectInto(GenericType<R> containerType);

    /**
     * Collect the query result into a container of the type specified by the given class.
     * A factory for building the container should be registered in the query's
     * collector factory registry.
     *
     * @param containerType   the class that represents the container
     * @param <R> the generic type of the container
     * @return the container with the query result
     */
    <R> R collectInto(Class<R> containerType);

}
