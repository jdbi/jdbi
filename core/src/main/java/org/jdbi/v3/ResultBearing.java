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

import com.fasterxml.classmate.GenericType;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface ResultBearing<ResultType> extends Iterable<ResultType>
{
    @Override
    ResultIterator<ResultType> iterator();

    /**
     * Get the only row in the result set.
     * @throws IllegalStateException if zero or multiple rows are returned
     * @return the object mapped from the singular row in the results
     */
    default ResultType findOnly() {
        try (ResultIterator<ResultType> iter = iterator()) {
            if (!iter.hasNext()) {
                throw new IllegalStateException("No element found in 'only'");
            }

            final ResultType r = iter.next();

            if (iter.hasNext()) {
                throw new IllegalStateException("Multiple elements found in 'only'");
            }

            return r;
        }
    }

    /**
     * Get the first row in the result set, if present.
     */
    default Optional<ResultType> findFirst() {
        try (ResultIterator<ResultType> iter = iterator()) {
            return iter.hasNext() ? Optional.of(iter.next()) : Optional.empty();
        }
    }

    default Stream<ResultType> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default List<ResultType> list() {
        return collect(Collectors.toList());
    }

    /**
     * Collect the query results into a container specified by a collector.
     *
     * @param collector       the collector
     * @param <ContainerType> the generic type of the container
     * @param <A>             the generic type of the container builder
     * @return the container with the query result
     */
    default <ContainerType, A> ContainerType collect(Collector<ResultType, A, ContainerType> collector) {
        try (Stream<ResultType> stream = stream()) {
            return stream.collect(collector);
        }
    }

    /**
     * Collect the query result into a container of the type specified by the given class.
     * A factory for building the container should be registered in the query's
     * collector factory registry.
     *
     * @param containerType   the class that represents the container
     * @param <ContainerType> the generic type of the container
     * @return the container with the query result
     */
    default <ContainerType> ContainerType collectInto(GenericType<ContainerType> containerType) {
        return collectInto(new TypeResolver().resolve(containerType));
    }

    /**
     * Collect the query result into a container of the type specified by the given class.
     * A factory for building the container should be registered in the query's
     * collector factory registry.
     *
     * @param containerType   the class that represents the container
     * @param <ContainerType> the generic type of the container
     * @return the container with the query result
     */
    <ContainerType> ContainerType collectInto(ResolvedType containerType);

}
