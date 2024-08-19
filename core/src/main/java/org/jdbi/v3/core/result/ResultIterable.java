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
package org.jdbi.v3.core.result;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.internal.ResultSetResultIterable;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.meta.Alpha;

import static java.util.Spliterators.spliteratorUnknownSize;

/**
 * An {@link Iterable} of values, usually mapped from a {@link java.sql.ResultSet}. Generally, ResultIterables may only
 * be traversed once.
 *
 * @param <T> iterable element type
 */
@FunctionalInterface
public interface ResultIterable<T> extends Iterable<T> {

    /**
     * Returns a ResultIterable backed by the given result set supplier, mapper, and context.
     *
     * @param resultSetSupplier result set supplier
     * @param mapper   row mapper
     * @param ctx      statement context
     * @param <T>      the mapped type
     * @return the result iterable
     */
    static <T> ResultIterable<T> of(Supplier<ResultSet> resultSetSupplier, RowMapper<T> mapper, StatementContext ctx) {
        return new ResultSetResultIterable<>(mapper, ctx, resultSetSupplier);
    }

    /**
     * Returns a ResultIterable backed by the given iterator.
     * @param iterator the result iterator
     * @param <T> iterator element type
     * @return a ResultIterable
     */
    static <T> ResultIterable<T> of(ResultIterator<T> iterator) {
        return () -> iterator;
    }

    /**
     * Stream all the rows of the result set out
     * with an {@code Iterator}.  The {@code Iterator} must be
     * closed to release database resources.
     * @return the results as a streaming Iterator
     */
    @Override
    ResultIterator<T> iterator();

    /**
     * Passes the iterator of results to the consumer. Database resources owned by the query are
     * released before this method returns.
     *
     * @param consumer a consumer which receives the iterator of results.
     * @throws X any exception thrown by the callback
     */
    default <X extends Exception> void useIterator(IteratorConsumer<T, X> consumer) throws X {
        withIterator(iterator -> {
            consumer.useIterator(iterator);
            return null;
        });
    }

    /**
     * Passes the iterator of results to the callback. Database resources owned by the query are
     * released before this method returns.
     *
     * @param callback a callback which receives the iterator of results, and returns some result.
     * @param <R> the type returned by the callback
     *
     * @return the value returned by the callback.
     * @throws X any exception thrown by the callback
     */
    default <R, X extends Exception> R withIterator(IteratorCallback<T, R, X> callback) throws X {
        try (ResultIterator<T> iterator = iterator()) {
            return callback.withIterator(iterator);
        }
    }

    /**
     * Returns a {@code ResultIterable<U>} derived from this {@code ResultIterable<T>}, by
     * transforming elements using the given mapper function.
     *
     * @param mapper function to apply to elements of this ResultIterable
     * @param <R>    Element type of the returned ResultIterable
     * @return the new ResultIterable
     */
    default <R> ResultIterable<R> map(Function<? super T, ? extends R> mapper) {
        return () -> new ResultIteratorDelegate<T, R>(iterator()) {
            @Override
            public R next() {
                return mapper.apply(getDelegate().next());
            }
        };
    }

    @Override
    default void forEach(Consumer<? super T> action) {
        forEachWithCount(action);
    }

    /**
     * Performs the specified action on each remaining element and returns the iteration i.e. record count.<br>
     * It is often useful (e.g. for logging) to know the record count while processing result sets.
     * <pre>
     * {@code
         int cnt = h.createQuery("select * from something").mapTo(String.class)
                    .forEachWithCount(System.out::println);
         System.out.println(cnt + " records selected");
       }
     *  </pre>
     *
     * @param action action to apply (required)
     * @return iteration count
     *
     * @since 3.31
     */
    default int forEachWithCount(Consumer<? super T> action) {
        Objects.requireNonNull(action, "Action required");
        try (ResultIterator<T> iter = iterator()) {
            int count = 0;
            while (iter.hasNext()) {
                count++;
                action.accept(iter.next());
            }
            return count;
        }
    }

    /**
     * Returns the only row in the result set. Returns {@code null} if the row itself is
     * {@code null}.
     * @throws IllegalStateException if the result set contains zero or multiple rows
     * @return the only row in the result set.
     */
    default T one() {
        try (ResultIterator<T> iter = iterator()) {
            if (!iter.hasNext()) {
                throw new IllegalStateException("Expected one element, but found none");
            }

            final T r = iter.next();

            if (iter.hasNext()) {
                throw new IllegalStateException("Expected one element, but found multiple");
            }

            return r;
        }
    }

    /**
     * Returns the only row in the result set, if any. Returns {@code Optional.empty()} if zero
     * rows are returned, or if the row itself is {@code null}.
     * @throws IllegalStateException if the result set contains multiple rows
     * @return the only row in the result set, if any.
     */
    default Optional<T> findOne() {
        try (ResultIterator<T> iter = iterator()) {
            if (!iter.hasNext()) {
                return Optional.empty();
            }

            final T r = iter.next();

            if (iter.hasNext()) {
                throw new IllegalStateException("Expected zero to one elements, but found multiple");
            }

            return Optional.ofNullable(r);
        }
    }

    /**
     * Get the only row in the result set.
     * @throws IllegalStateException if zero or multiple rows are returned
     * @return the object mapped from the singular row in the results
     * @deprecated use {@link #one()} or {@link #findOne()} instead.
     */
    @Deprecated
    default T findOnly() {
        return one();
    }

    /**
     * Returns the first row in the result set. Returns {@code null} if the row itself is
     * {@code null}.
     * @throws IllegalStateException if zero rows are returned
     * @return the first row in the result set.
     */
    default T first() {
        try (ResultIterator<T> iter = iterator()) {
            if (!iter.hasNext()) {
                throw new IllegalStateException("Expected at least one element, but found none");
            }

            return iter.next();
        }
    }

    /**
     * Returns the first row in the result set, if present. Returns {@code Optional.empty()} if
     * zero rows are returned or the first row is {@code null}.
     * @return the first row in the result set, if present.
     */
    default Optional<T> findFirst() {
        try (ResultIterator<T> iter = iterator()) {
            return iter.hasNext() ? Optional.ofNullable(iter.next()) : Optional.empty();
        }
    }

    /**
     * Returns the stream of results.
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
     * Passes the stream of results to the consumer. Database resources owned by the query are
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
     * Passes the stream of results to the callback. Database resources owned by the query are
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
     * Returns results in a {@link List} using the JDK specific default implementation of {@link List} as provided
     * by {@link Collectors#toList()}. The same limitations apply to the list instance returned from this method.
     * If more control over the list type is required, use {@link #toCollection(Supplier)} or see the {@link #collectIntoList()} method.
     *
     * @return results in a {@link List}
     * @see ResultIterable#collectIntoList()
     * @see ResultIterable#toCollection(Supplier)
     */
    default List<T> list() {
        return collect(Collectors.toList());
    }

    /**
     * Returns results in a {@link Set} using the JDK specific default implementation of {@link Set} as provided
     * by {@link Collectors#toSet()}. The same limitations apply to the set instance returned from this method.
     * If more control over the set type is required, use {@link #toCollection(Supplier)} or see the {@link #collectIntoSet()} method.
     *
     * @return results in a {@link Set}
     * @see ResultIterable#collectIntoSet()
     * @see ResultIterable#toCollection(Supplier)
     * @since 3.38.0
     */
    default Set<T> set() {
        return collect(Collectors.toSet());
    }

    /**
     * Collect the results into a container specified by a collector.
     *
     * @param collector       the collector
     * @param <R>             the generic type of the container
     * @return the container with the query result
     */
    default <R> R collect(Collector<? super T, ?, R> collector) {
        try (Stream<T> stream = stream()) {
            return stream.collect(collector);
        }
    }

    /**
     * Collect the results into a Map, using the given functions to compute keys and values.
     * @param <K> the key type
     * @param <V> the value type
     * @param keyFunction a function that transforms the query result to a map key
     * @param valueFunction a function that transforms the query result to a map value
     * @return the collected Map
     * @since 3.38.0
     */
    default <K, V> Map<K, V> collectToMap(Function<? super T, ? extends K> keyFunction, Function<? super T, ? extends V> valueFunction) {
        return collect(Collectors.toMap(keyFunction, valueFunction));
    }

    /**
     * Collect the results into a collection object similar to {@link Collectors#toCollection(Supplier)}.
     *
     * @param supplier a supplier providing a new empty Collection into which the results will be inserted
     * @return A new collection with all results inserted
     * @since 3.38.0
     */
    @Alpha
    default <C extends Collection<T>> C toCollection(Supplier<C> supplier) {
        return collect(Collectors.toCollection(supplier));
    }

    /**
     * Collect the results into a container type.
     *
     * @param containerType A {@link Type} object that must describe a container type
     * @return A new collection implementing the container type with all results inserted
     * @throws UnsupportedOperationException if the implementation does not support this operation
     * @since 3.38.0
     */
    @Alpha
    default <R extends Collection<? super T>> R collectInto(Type containerType) {
        throw new UnsupportedOperationException();
    }

    /**
     * Collect the results into a collection type.
     *
     * @param containerType A {@link GenericType} object that describes a collection type
     * @return A new collection implementing the container type with all results inserted
     * @throws UnsupportedOperationException if the implementation does not support this operation
     * @since 3.38.0
     */
    @Alpha
    default <R extends Collection<? super T>> R collectInto(GenericType<R> containerType) {
        return collectInto(containerType.getType());
    }

    /**
     * Returns results in a {@link List}. The implementation of the list can be changed by registering a {@link Collector}:
     * <p>
     * <pre>{@code
     *     jdbi.getConfig(JdbiCollectors.class).registerCollector(List.class, Collectors.toCollection(LinkedList::new));
     * }</pre>
     * or
     * <pre>{@code
     *     handle.registerCollector(List.class, Collectors.toCollection(LinkedList::new));
     * }</pre>
     * <br>
     * If no collector is registered, then this method behaves like {@link #list()}.
     *
     * @return results in a {@link List}
     * @since 3.38.0
     * @see ResultIterable#list()
     * @see ResultIterable#toCollection(Supplier)
     */
    @Alpha
    default List<T> collectIntoList() {
        return list();
    }

    /**
     * Returns results in a {@link Set}. The implementation of the set can be changed by registering a {@link Collector}:
     * <p>
     * <pre>{@code
     *     jdbi.getConfig(JdbiCollectors.class).registerCollector(Set.class, Collectors.toCollection(LinkedHashSet::new));
     * }</pre>
     * or
     * <pre>{@code
     *     handle.registerCollector(Set.class, Collectors.toCollection(LinkedHashSet::new));
     * }</pre>
     * <br>
     * If no collector is registered, then this method behaves like {@link #set()}.
     *
     * @return results in a {@link Set}
     * @since 3.38.0
     * @see ResultIterable#set()
     * @see ResultIterable#toCollection(Supplier)
     */
    @Alpha
    default Set<T> collectIntoSet() {
        return set();
    }

    /**
     * Reduce the results.  Using a {@code BiFunction<U, T, U>}, repeatedly
     * combine query results until only a single value remains.
     *
     * @param <U> the accumulator type
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
     * Convenience method to filter the {@link ResultIterable} by applying the specified {@link Predicate}.<br>
     * This method has the look and feel of {@link Stream#filter(Predicate)} without making use of streams.<p>
     *
     * Please note that filtering takes place in Java i.e. your client code, <b>not in the database</b>.<br>
     * Filtering inside the database will most likely be of higher performance than filtering outside,
     * as intermediate results are loaded into Java and then discarded. Moreover, indexes that may exist
     * in the database will not be utilized here.
     *
     * @param predicate a non-null predicate to apply to each element
     *                  to determine whether it should be included in the result
     * @return the new result iterable
     *
     * @since 3.31
     */
    default ResultIterable<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "Filter required");
        return () -> new ResultIteratorDelegate<T, T>(iterator()) {
            /** The next result (initially {@code null}). */
            private T next;

            /**
             * Returns {@code true} if the resultset has a {@code next} element.<br>
             * Repeatedly calling this method will move the resultset forward at most once.
             *
             * @return {@code true} if the resultset has another element
             */
            @Override
            public boolean hasNext() {
                return next != null || findNext();
            }

            /**
             * Returns the next result in the resultset.
             *
             * @return the next element in the resultset that passes the filter
             * @throws NoSuchElementException if the resultset has no more suitable elements
             */
            @Override
            public T next() {
                if (next == null && !findNext()) {
                    throw new NoSuchElementException("No more filtered results");
                }
                T n = next;
                next = null;
                return n;
            }

            /**
             * Forwards the resultset to the next result that passes the filter (i.e. {@link Predicate} tests {@code true}).<br>
             * @return true if another such result exists, false otherwise
             */
            @SuppressWarnings("PMD.UnusedAssignment")
            private boolean findNext() {
                next = null;
                while (getDelegate().hasNext()) {
                    T n = getDelegate().next();
                    if (predicate.test(n)) {
                        next = n;
                        return true;
                    }
                }
                return false;
            }
        };

    }

    /**
     * An implementation of {@link ResultIterator} that delegates calls
     * to the iterator provided in the constructor.
     *
     * @param <T> iterable element type of delegate
     * @param <R> returned iterable element type, may be same as delegate's ({@code T})
     */
    abstract class ResultIteratorDelegate<T, R> implements ResultIterator<R> {
        private final ResultIterator<T> delegate;

        ResultIteratorDelegate(ResultIterator<T> del) {
            delegate = Objects.requireNonNull(del, "Delegate required");
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public final void close() {
            delegate.close();
        }

        @Override
        public final StatementContext getContext() {
            return delegate.getContext();
        }

        protected final ResultIterator<T> getDelegate() {
            return delegate;
        }
    }
}
