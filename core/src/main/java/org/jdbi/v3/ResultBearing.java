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
    default ResultType only() {
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
    default Optional<ResultType> first() {
        try (ResultIterator<ResultType> iter = iterator()) {
            return iter.hasNext() ? Optional.of(iter.next()) : Optional.empty();
        }
    }

    default Stream<ResultType> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default List<ResultType> list() {
        try (Stream<ResultType> stream = stream()) {
            return stream.collect(Collectors.<ResultType>toList());
        }
    }
}
