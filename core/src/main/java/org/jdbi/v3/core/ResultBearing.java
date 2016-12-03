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

import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jdbi.v3.core.util.StreamCallback;
import org.jdbi.v3.core.util.StreamConsumer;

public interface ResultBearing<T> extends Iterable<T>
{
    /**
     * Execute the statement.  The given {@code StatementExecutor}
     * decides the execution strategy and return type.
     *
     * Most users will not use this method directly.
     *
     * @param executor the StatementExecutor to use
     * @return the produced results
     */
    <R> R execute(ResultProducer<T, R> executor);

    /**
     * Stream all the rows of the result set out
     * with an {@code Iterator}.  The {@code Iterator} must be
     * closed to release database resources.
     * @return the results as a streaming Iterator
     */
    @Override
    ResultIterator<T> iterator();

    /**
     * @return the current statement context
     */
    StatementContext getContext();

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
     * @return the first row in the result set, if present.
     */
    default Optional<T> findFirst() {
        try (ResultIterator<T> iter = iterator()) {
            return iter.hasNext() ? Optional.ofNullable(iter.next()) : Optional.empty();
        }
    }

    /**
     * Executes the query and returns the stream of results.
     *
     * <p>
     * Note: the returned stream owns database resources, and must be closed via a call to {@link Stream#close()}, or
     * by using the stream in a try-with-resources block:
     * </p>
     *
     * <pre>
     * try (Stream&lt;T&gt; stream = query.stream()) {
     *   // do stuff with stream
     * }
     * </pre>
     *
     * @return the stream of results.
     *
     * @see #useStream(StreamConsumer)
     * @see #withStream(StreamCallback)
     */
    default Stream<T> stream() {
        ResultIterator<T> iterator = iterator();
        return StreamSupport.stream(spliteratorUnknownSize(iterator, 0), false)
                .onClose(iterator::close);
    }

    /**
     * Executes the query, and passes the stream of results to the consumer. Database resources owned by the query are
     * released before this method returns.
     *
     * @param consumer a consumer which receives the stream of results.
     * @param <X> the exception type thrown by the callback, if any
     *
     * @throws X any exception thrown by the callback
     */
    default <X extends Exception> void useStream(StreamConsumer<T, X> consumer) throws X {
        withStream(stream -> {
            consumer.useStream(stream);
            return null;
        });
    }

    /**
     * Executes the query, and passes the stream of results to the callback. Database resources owned by the query are
     * released before this method returns.
     *
     * @param callback a callback which receives the stream of results, and returns some result.
     * @param <R> the type returned by the callback
     * @param <X> the exception type thrown by the callback, if any
     *
     * @return the value returned by the callback.
     *
     * @throws X any exception thrown by the callback
     */
    default <R, X extends Exception> R withStream(StreamCallback<T, R, X> callback) throws X {
        try (Stream<T> stream = stream()) {
            return callback.withStream(stream);
        }
    }

    /**
     * @return the list of query results.
     */
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

    /**
     * Reduce the results.  Using a {@code BiFunction<U, T, U>}, repeatedly
     * combine query results until only a single value remains.
     *
     * @param identity the {@code U} to combine with the first result
     * @param accumulator the function to apply repeatedly
     * @return the final {@code U}
     */
    default <U> U reduce(U identity, BiFunction<U, T, U> accumulator) {
        try (Stream<T> stream = stream()) {
            return stream.reduce(identity, accumulator,
                (u, v) -> {
                    throw new UnsupportedOperationException("parallel operation not supported");
                });
        }
    }

    /**
     * Reduce the results.  Using a {@code BiFunction<U, RowView, U>}, repeatedly
     * combine query results until only a single value remains.
     *
     * @param seed the {@code U} to combine with the first result
     * @param accumulator the function to apply repeatedly
     * @return the final {@code U}
     */
    default <U> U reduceRows(U seed, BiFunction<U, RowView, U> accumulator) {
        return execute((stmt, rs, ctx) -> {
            RowView rv = new RowView(rs, ctx);
            U result = seed;
            while (rs.next()) {
                result = accumulator.apply(result, rv);
            }
            return result;
        });
    }

    /**
     * Reduce the results.  Using a {@code ResultSetAccumulator}, repeatedly
     * combine query results until only a single value remains.
     *
     * @param seed the {@code U} to combine with the first result
     * @param accumulator the function to apply repeatedly
     * @return the final {@code U}
     */
    default <U> U reduceResultSet(U seed, ResultSetAccumulator<U> accumulator) {
        return execute((stmt, rs, ctx) -> {
            U result = seed;
            while (rs.next()) {
                result = accumulator.apply(result, rs, ctx);
            }
            return result;
        });
    }
}
