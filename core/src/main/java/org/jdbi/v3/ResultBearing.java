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

import static java.util.Spliterators.spliteratorUnknownSize;

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

    /**
     * Executes the SQL statement and returns the stream of results.
     *
     * <p>
     * Note: the returned stream owns database resources, and must be closed via a call to {@link Stream#close()}, or
     * by using the stream in a try-with-resources block:
     * </p>
     *
     * <pre>
     * try (Stream&lt;T> stream = query.stream()) {
     *   // do stuff with stream
     * }
     * </pre>
     */
    default Stream<T> stream() {
        ResultIterator<T> iterator = iterator();
        return StreamSupport.stream(spliteratorUnknownSize(iterator, 0), false)
                .onClose(iterator::close);
    }

    default List<T> list() {
        return collect(Collectors.toList());
    }

    /**
     * Collect the query results into a container specified by a collector.
     *
     * @param collector       the collector
     * @param <R>             the generic type of the container
     * @return the container with the query result
     */
    default <R> R collect(Collector<T, ?, R> collector) {
        try (Stream<T> stream = stream()) {
            return stream.collect(collector);
        }
    }

}
