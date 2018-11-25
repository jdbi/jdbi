/**
 * Copyright (c), Data Geekery GmbH, contact@datageekery.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14;

import static java.util.Comparator.comparing;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;

import static org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.SeqUtils.sneakyThrow;
import static org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple.tuple;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.exception.TooManyElementsException;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function10;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function11;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function12;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function13;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function14;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function15;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function16;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function3;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function4;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function5;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function6;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function7;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function8;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function9;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple1;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple10;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple11;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple12;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple13;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple14;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple15;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple16;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple2;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple3;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple4;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple5;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple6;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple7;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple8;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple9;


/**
 * A sequential, ordered {@link Stream} that adds all sorts of useful methods that work only because
 * it is sequential and ordered.
 *
 * @author Lukas Eder
 * @author Roman Tkalenko
 * @author Tomasz Linkowski
 */
public interface Seq<T> extends Stream<T>, Iterable<T>, Collectable<T> {

    /**
     * The underlying {@link Stream} implementation.
     */
    Stream<T> stream();

    /**
     * Transform this stream into a new type.
     * <p>
     * If certain operations are re-applied frequently to streams, this
     * transform operation is very useful for such operations to be applied in a
     * fluent style:
     * <p>
     * <code><pre>
     * Function&ltSeq&lt;Integer>, Seq&lt;String>> toString = s -> s.map(Objects::toString);
     * Seq&lt;String> strings =
     * Seq.of(1, 2, 3)
     *    .transform(toString);
     * </pre></code>
     */
    default <U> U transform(Function<? super Seq<T>, ? extends U> transformer) {
        return transformer.apply(this);
    }

    /**
     * Cross apply a function to this stream.
     * <p>
     * This works like {@link #flatMap(java.util.function.Function)}, except
     * that the result retains the original <code>T</code> values.
     * <p>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </p>
     */
    default <U> Seq<Tuple2<T, U>> crossApply(Function<? super T, ? extends Iterable<? extends U>> function) {
        return crossApply(this, t -> seq(function.apply(t)));
    }

    /**
     * Outer apply a function to this stream.
     * <p>
     * This works like {@link #flatMap(java.util.function.Function)}, except
     * that the result retains the original <code>T</code> values.
     * <p>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </p>
     */
    default <U> Seq<Tuple2<T, U>> outerApply(Function<? super T, ? extends Iterable<? extends U>> function) {
        return outerApply(this, t -> seq(function.apply(t)));
    }

    /**
     * Cross join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> crossJoin(Stream<? extends U> other) {
        return Seq.crossJoin(this, other);
    }

    /**
     * Cross join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> crossJoin(Iterable<? extends U> other) {
        return Seq.crossJoin(this, other);
    }

    /**
     * Cross join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> crossJoin(Seq<? extends U> other) {
        return Seq.crossJoin(this, other);
    }

    /**
     * Cross join stream with itself into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(1, 2), tuple(2, 1), tuple(2, 2))
     * Seq.of(1, 2).crossSelfJoin()
     * </pre></code>
     */
    default Seq<Tuple2<T, T>> crossSelfJoin() {
        SeqBuffer<T> buffer = SeqBuffer.of(this);
        return crossJoin(buffer.seq(), buffer.seq());
    }

    /**
     * Inner join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2))
     * Seq.of(1, 2, 3).innerJoin(Seq.of(1, 2), (t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> innerJoin(Stream<? extends U> other, BiPredicate<? super T, ? super U> predicate) {
        return innerJoin(seq(other), predicate);
    }

    /**
     * Inner join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2))
     * Seq.of(1, 2, 3).innerJoin(Seq.of(1, 2), (t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> innerJoin(Iterable<? extends U> other, BiPredicate<? super T, ? super U> predicate) {
        return innerJoin(seq(other), predicate);
    }

    /**
     * Inner join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2))
     * Seq.of(1, 2, 3).innerJoin(Seq.of(1, 2), (t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> innerJoin(Seq<? extends U> other, BiPredicate<? super T, ? super U> predicate) {

        // This algorithm has substantial complexity for large argument streams!
        SeqBuffer<? extends U> buffer = SeqBuffer.of(other);

        return flatMap(t -> buffer.seq()
                           .filter(u -> predicate.test(t, u))
                           .map(u -> Tuple.<T, U>tuple(t, u)))
              .onClose(other::close);
    }

    /**
     * Inner join stream with itself.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2))
     * Seq.of(1, 2).innerSelfJoin((t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default Seq<Tuple2<T, T>> innerSelfJoin(BiPredicate<? super T, ? super T> predicate) {
        SeqBuffer<T> buffer = SeqBuffer.of(this);
        return buffer.seq().innerJoin(buffer.seq(), predicate);
    }

    /**
     * Left outer join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2), tuple(3, null))
     * Seq.of(1, 2, 3).leftOuterJoin(Seq.of(1, 2), (t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> leftOuterJoin(Stream<? extends U> other, BiPredicate<? super T, ? super U> predicate) {
        return leftOuterJoin(seq(other), predicate);
    }

    /**
     * Left outer join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2), tuple(3, null))
     * Seq.of(1, 2, 3).leftOuterJoin(Seq.of(1, 2), (t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> leftOuterJoin(Iterable<? extends U> other, BiPredicate<? super T, ? super U> predicate) {
        return leftOuterJoin(seq(other), predicate);
    }

    /**
     * Left outer join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2), tuple(3, null))
     * Seq.of(1, 2, 3).leftOuterJoin(Seq.of(1, 2), (t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> leftOuterJoin(Seq<? extends U> other, BiPredicate<? super T, ? super U> predicate) {

        // This algorithm has substantial complexity for large argument streams!
        SeqBuffer<? extends U> buffer = SeqBuffer.of(other);

        return flatMap(t -> buffer.seq()
                           .filter(u -> predicate.test(t, u))
                           .onEmpty(null)
                           .map(u -> Tuple.<T, U>tuple(t, u)))
              .onClose(other::close);
    }

    /**
     * Left outer join one streams into itself.
     * <p>
     * <code><pre>
     * // (tuple(tuple(1, 0), NULL), tuple(tuple(2, 1), tuple(1, 0)))
     * Seq.of(new Tuple2<Integer, Integer>(1, 0), new Tuple2<Integer, Integer>(2, 1)).leftOuterSelfJoin((t, u) -> Objects.equals(t.v2, u.v1))
     * </pre></code>
     */
    default Seq<Tuple2<T, T>> leftOuterSelfJoin(BiPredicate<? super T, ? super T> predicate) {
        SeqBuffer<T> buffer = SeqBuffer.of(this);
        return buffer.seq().leftOuterJoin(buffer.seq(), predicate);
    }

    /**
     * Right outer join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2), tuple(null, 3))
     * Seq.of(1, 2).rightOuterJoin(Seq.of(1, 2, 3), (t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> rightOuterJoin(Stream<? extends U> other, BiPredicate<? super T, ? super U> predicate) {
        return rightOuterJoin(seq(other), predicate);
    }

    /**
     * Right outer join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2), tuple(null, 3))
     * Seq.of(1, 2).rightOuterJoin(Seq.of(1, 2, 3), (t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> rightOuterJoin(Iterable<? extends U> other, BiPredicate<? super T, ? super U> predicate) {
        return rightOuterJoin(seq(other), predicate);
    }

    /**
     * Right outer join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, 1), tuple(2, 2), tuple(null, 3))
     * Seq.of(1, 2).rightOuterJoin(Seq.of(1, 2, 3), (t, u) -> Objects.equals(t, u))
     * </pre></code>
     */
    default <U> Seq<Tuple2<T, U>> rightOuterJoin(Seq<? extends U> other, BiPredicate<? super T, ? super U> predicate) {
        return other
              .leftOuterJoin(this, (u, t) -> predicate.test(t, u))
              .map(t -> Tuple.<T, U>tuple(t.v2, t.v1))
              .onClose(other::close);
    }

    /**
     * Right outer join stream into itself.
     * <p>
     * <code><pre>
     * // (tuple(NULL, tuple(1, 0)), tuple(tuple(1, 0), tuple(2, 1)))
     * Seq.of(new Tuple2<Integer, Integer>(1, 0), new Tuple2<Integer, Integer>(2, 1)).rightOuterSelfJoin((t, u) -> Objects.equals(t.v2, u.v1))
     * </pre></code>
     */
    default Seq<Tuple2<T, T>> rightOuterSelfJoin(BiPredicate<? super T, ? super T> predicate) {
        return leftOuterSelfJoin((u, t) -> predicate.test(t, u))
              .map(t -> tuple(t.v2, t.v1));
    }

    /**
     * Produce this stream, or an alternative stream with the
     * <code>value</code>, in case this stream is empty.
     */
    default Seq<T> onEmpty(T value) {
        return onEmptyGet(() -> value);
    }

    /**
     * Produce this stream, or an alternative stream with a value from the
     * <code>supplier</code>, in case this stream is empty.
     */
    default Seq<T> onEmptyGet(Supplier<? extends T> supplier) {
        boolean[] first = { true };

        return SeqUtils.transform(this, (delegate, action) -> {
            if (first[0]) {
                first[0] = false;

                if (!delegate.tryAdvance(action))
                    action.accept(supplier.get());

                return true;
            } else {
                return delegate.tryAdvance(action);
            }
        });
    }

    /**
     * Produce this stream, or throw a throwable from the
     * <code>supplier</code>, in case this stream is empty.
     */
    default <X extends Throwable> Seq<T> onEmptyThrow(Supplier<? extends X> supplier) {
        boolean[] first = { true };

        return SeqUtils.transform(this, (delegate, action) -> {
            if (first[0]) {
                first[0] = false;

                if (!delegate.tryAdvance(action))
                    sneakyThrow(supplier.get());

                return true;
            } else {
                return delegate.tryAdvance(action);
            }
        });
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).concat(Seq.of(4, 5, 6))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    default Seq<T> concat(Stream<? extends T> other) {
        return concat(seq(other));
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).concat(Seq.of(4, 5, 6))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    default Seq<T> concat(Iterable<? extends T> other) {
        return concat(seq(other));
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).concat(Seq.of(4, 5, 6))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    @SuppressWarnings({ "unchecked" })
    default Seq<T> concat(Seq<? extends T> other) {
        return Seq.concat(new Seq[]{this, other});
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4)
     * Seq.of(1, 2, 3).concat(4)
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    default Seq<T> concat(T other) {
        return concat(Seq.of(other));
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).concat(4, 5, 6)
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    @SuppressWarnings({ "unchecked" })
    default Seq<T> concat(T... other) {
        return concat(Seq.of(other));
    }

    /**
     * Concatenate an optional value.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4)
     * Seq.of(1, 2, 3).concat(Optional.of(4))
     *
     * // (1, 2, 3)
     * Seq.of(1, 2, 3).concat(Optional.empty())
     * </pre></code>
     */
    default Seq<T> concat(Optional<? extends T> other) {
        return concat(Seq.seq(other));
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).append(Seq.of(4, 5, 6))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    default Seq<T> append(Stream<? extends T> other) {
        return concat(other);
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).append(Seq.of(4, 5, 6))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    default Seq<T> append(Iterable<? extends T> other) {
        return concat(other);
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).append(Seq.of(4, 5, 6))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    @SuppressWarnings({ "unchecked" })
    default Seq<T> append(Seq<? extends T> other) {
        return concat(other);
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4)
     * Seq.of(1, 2, 3).append(4)
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    default Seq<T> append(T other) {
        return concat(other);
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).append(4, 5, 6)
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    @SuppressWarnings({ "unchecked" })
    default Seq<T> append(T... other) {
        return concat(other);
    }

    /**
     * Concatenate an optional value.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4)
     * Seq.of(1, 2, 3).append(Optional.of(4))
     *
     * // (1, 2, 3)
     * Seq.of(1, 2, 3).append(Optional.empty())
     * </pre></code>
     */
    default Seq<T> append(Optional<? extends T> other) {
        return concat(other);
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(4, 5, 6).prepend(Seq.of(1, 2, 3))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    default Seq<T> prepend(Stream<? extends T> other) {
        return Seq.<T>seq(other).concat(this);
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(4, 5, 6).prepend(Seq.of(1, 2, 3))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    default Seq<T> prepend(Iterable<? extends T> other) {
        return Seq.<T>seq(other).concat(this);
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(4, 5, 6).prepend(Seq.of(1, 2, 3))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    @SuppressWarnings({ "unchecked" })
    default Seq<T> prepend(Seq<? extends T> other) {
        return concat(other, this);
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4)
     * Seq.of(2, 3, 4).prepend(1)
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    default Seq<T> prepend(T other) {
        return Seq.of(other).concat(this);
    }

    /**
     * Concatenate two streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(4, 5, 6).prepend(Seq.of(1, 2, 3))
     * </pre></code>
     *
     * @see #concat(Stream[])
     */
    @SuppressWarnings({ "unchecked" })
    default Seq<T> prepend(T... other) {
        return Seq.of(other).concat(this);
    }

    /**
     * Concatenate an optional value.
     * <p>
     * <code><pre>
     * // (0, 1, 2, 3)
     * Seq.of(1, 2, 3).prepend(Optional.of(0))
     *
     * // (1, 2, 3)
     * Seq.of(1, 2, 3).prepend(Optional.empty())
     * </pre></code>
     */
    default Seq<T> prepend(Optional<? extends T> other) {
        return Seq.<T>seq(other).concat(this);
    }

    /**
     * Check whether this stream contains a given value.
     * <p>
     * <code><pre>
     * // true
     * Seq.of(1, 2, 3).contains(2)
     * </pre><code>
     */
    default boolean contains(T other) {
        return anyMatch(Predicate.isEqual(other));
    }

    /**
     * Check whether this stream contains all given values.
     * <p>
     * <code><pre>
     * // true
     * Seq.of(1, 2, 3).containsAll(2, 3)
     * </pre><code>
     */
    default boolean containsAll(T... other) {
        return containsAll(of(other));
    }

    /**
     * Check whether this stream contains all given values.
     * <p>
     * <code><pre>
     * // true
     * Seq.of(1, 2, 3).containsAll(2, 3)
     * </pre><code>
     */
    default boolean containsAll(Stream<? extends T> other) {
        return containsAll(seq(other));
    }

    /**
     * Check whether this stream contains all given values.
     * <p>
     * <code><pre>
     * // true
     * Seq.of(1, 2, 3).containsAll(2, 3)
     * </pre><code>
     */
    default boolean containsAll(Iterable<? extends T> other) {
        return containsAll(seq(other));
    }

    /**
     * Check whether this stream contains all given values.
     * <p>
     * <code><pre>
     * // true
     * Seq.of(1, 2, 3).containsAll(2, 3)
     * </pre><code>
     */
    default boolean containsAll(Seq<? extends T> other) {
        Set<? extends T> set = other.toSet(HashSet::new);
        return set.isEmpty() ? true : filter(t -> set.remove(t)).anyMatch(t -> set.isEmpty());
    }

    /**
     * Check whether this stream contains any of the given values.
     * <p>
     * <code><pre>
     * // true
     * Seq.of(1, 2, 3).containsAny(2, 4)
     * </pre><code>
     */
    default boolean containsAny(T... other) {
        return containsAny(of(other));
    }

    /**
     * Check whether this stream contains any of the given values.
     * <p>
     * <code><pre>
     * // true
     * Seq.of(1, 2, 3).containsAny(2, 4)
     * </pre><code>
     */
    default boolean containsAny(Stream<? extends T> other) {
        return containsAny(seq(other));
    }

    /**
     * Check whether this stream contains any of the given values.
     * <p>
     * <code><pre>
     * // true
     * Seq.of(1, 2, 3).containsAny(2, 4)
     * </pre><code>
     */
    default boolean containsAny(Iterable<? extends T> other) {
        return containsAny(seq(other));
    }

    /**
     * Check whether this stream contains any of the given values.
     * <p>
     * <code><pre>
     * // true
     * Seq.of(1, 2, 3).containsAny(2, 4)
     * </pre><code>
     */
    default boolean containsAny(Seq<? extends T> other) {
        Set<? extends T> set = other.toSet(HashSet::new);
        return set.isEmpty() ? false : anyMatch(set::contains);
    }

    /**
     * Get a single element from the stream at a given index.
     */
    default Optional<T> get(long index) {
        if (index < 0L)
            return Optional.empty();
        else if (index == 0L)
            return findFirst();
        else
            return skip(index).findFirst();
    }

    /**
     * Get the single element from the stream, or throw an exception if the
     * stream holds more than one element.
     */
    default Optional<T> findSingle() throws TooManyElementsException {
        Iterator<T> it = iterator();

        if (!it.hasNext())
            return Optional.empty();

        T result = it.next();
        if (!it.hasNext())
            return Optional.of(result);

        throw new TooManyElementsException("Stream contained more than one element.");
    }

    /**
     * Get the first element from the stream given a predicate.
     */
    default Optional<T> findFirst(Predicate<? super T> predicate) {
        return filter(predicate).findFirst();
    }

    /**
     * Get the last element from the stream.
     */
    default Optional<T> findLast() {
        return reduce((a, b) -> b);
    }

    /**
     * Get a last element from the stream given a predicate.
     */
    default Optional<T> findLast(Predicate<? super T> predicate) {
        return filter(predicate).findLast();
    }

    /**
     * Get the index of the first element from the stream equal to given element.
     */
    default OptionalLong indexOf(T element) {
        return indexOf(Predicate.isEqual(element));
    }

    /**
     * Get the index of the first element from the stream matching given predicate.
     */
    default OptionalLong indexOf(Predicate<? super T> predicate) {
        return SeqUtils.indexOf(iterator(), predicate);
    }

    /**
     * Return a new stream where the first occurrence of the argument is removed.
     * <p>
     * <code><pre>
     * // 1, 3, 2, 4
     * Seq.of(1, 2, 3, 2, 4).remove(2)
     * </pre><code>
     */
    default Seq<T> remove(T other) {
        boolean[] removed = new boolean[1];
        return filter(t -> removed[0] || !(removed[0] = Objects.equals(t, other)));
    }

    /**
     * Return a new stream where all occurrences of the arguments are removed.
     * <p>
     * <code><pre>
     * // 1, 4
     * Seq.of(1, 2, 3, 2, 4).removeAll(2, 3)
     * </pre><code>
     */
    default Seq<T> removeAll(T... other) {
        return removeAll(of(other));
    }

    /**
     * Return a new stream where all occurrences of the arguments are removed.
     * <p>
     * <code><pre>
     * // 1, 4
     * Seq.of(1, 2, 3, 2, 4).removeAll(2, 3)
     * </pre><code>
     */
    default Seq<T> removeAll(Stream<? extends T> other) {
        return removeAll(seq(other));
    }

    /**
     * Return a new stream where all occurrences of the arguments are removed.
     * <p>
     * <code><pre>
     * // 1, 4
     * Seq.of(1, 2, 3, 2, 4).removeAll(2, 3)
     * </pre><code>
     */
    default Seq<T> removeAll(Iterable<? extends T> other) {
        return removeAll(seq(other));
    }

    /**
     * Return a new stream where all occurrences of the arguments are removed.
     * <p>
     * <code><pre>
     * // 1, 4
     * Seq.of(1, 2, 3, 2, 4).removeAll(2, 3)
     * </pre><code>
     */
    default Seq<T> removeAll(Seq<? extends T> other) {
        Set<? extends T> set = other.toSet(HashSet::new);
        return set.isEmpty() ? this : filter(t -> !set.contains(t)).onClose(other::close);
    }

    /**
     * Return a new stream where only occurrences of the arguments are retained.
     * <p>
     * <code><pre>
     * // 2, 3, 2
     * Seq.of(1, 2, 3, 2, 4).retainAll(2, 3)
     * </pre><code>
     */
    default Seq<T> retainAll(T... other) {
        return retainAll(of(other));
    }

    /**
     * Return a new stream where only occurrences of the arguments are retained.
     * <p>
     * <code><pre>
     * // 2, 3, 2
     * Seq.of(1, 2, 3, 2, 4).retainAll(2, 3)
     * </pre><code>
     */
    default Seq<T> retainAll(Stream<? extends T> other) {
        return retainAll(seq(other));
    }

    /**
     * Return a new stream where only occurrences of the arguments are retained.
     * <p>
     * <code><pre>
     * // 2, 3, 2
     * Seq.of(1, 2, 3, 2, 4).retainAll(2, 3)
     * </pre><code>
     */
    default Seq<T> retainAll(Iterable<? extends T> other) {
        return retainAll(seq(other));
    }

    /**
     * Return a new stream where only occurrences of the arguments are retained.
     * <p>
     * <code><pre>
     * // 2, 3, 2
     * Seq.of(1, 2, 3, 2, 4).retainAll(2, 3)
     * </pre><code>
     */
    default Seq<T> retainAll(Seq<? extends T> other) {
        Set<? extends T> set = other.toSet(HashSet::new);
        return set.isEmpty() ? empty() : filter(t -> set.contains(t)).onClose(other::close);
    }

    /**
     * Repeat a stream infinitely.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 1, 2, 3, ...)
     * Seq.of(1, 2, 3).cycle();
     * </pre></code>
     *
     * @see #cycle(Stream)
     */
    default Seq<T> cycle() {
        return cycle(this);
    }

    /**
     * Repeat a stream a certain amount of times.
     * <p>
     * <code><pre>
     * // ()
     * Seq.of(1, 2, 3).cycle(0);
     *
     * // (1, 2, 3)
     * Seq.of(1, 2, 3).cycle(1);
     *
     * // (1, 2, 3, 1, 2, 3, 1, 2, 3)
     * Seq.of(1, 2, 3).cycle(3);
     * </pre></code>
     *
     * @see #cycle(Stream, long)
     */
    default Seq<T> cycle(long times) {
        return cycle(this, times);
    }

    /**
     * Get a stream of distinct keys.
     * <p>
     * <code><pre>
     * // (1, 2, 3)
     * Seq.of(1, 1, 2, -2, 3).distinct(Math::abs)
     * </pre></code>
     */
    default <U> Seq<T> distinct(Function<? super T, ? extends U> keyExtractor) {
        final Map<U, String> seen = new ConcurrentHashMap<>();
        return filter(t -> seen.put(keyExtractor.apply(t), "") == null);
    }

    /**
     * Zip two streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     *
     * @see #zip(Stream, Stream)
     */
    default <U> Seq<Tuple2<T, U>> zip(Stream<? extends U> other) {
        return zip(seq(other));
    }

    /**
     * Zip two streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     *
     * @see #zip(Stream, Stream)
     */
    default <U> Seq<Tuple2<T, U>> zip(Iterable<? extends U> other) {
        return zip(seq(other));
    }

    /**
     * Zip two streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     *
     * @see #zip(Stream, Stream)
     */
    default <U> Seq<Tuple2<T, U>> zip(Seq<? extends U> other) {
        return zip(this, other);
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     *
     * @see #zip(Seq, BiFunction)
     */
    default <U, R> Seq<R> zip(Stream<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(seq(other), zipper);
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     *
     * @see #zip(Seq, BiFunction)
     */
    default <U, R> Seq<R> zip(Iterable<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(seq(other), zipper);
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     *
     * @see #zip(Seq, Seq, BiFunction)
     */
    default <U, R> Seq<R> zip(Seq<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(this, other, zipper);
    }

    // [jooq-tools] START [zip-all-static]

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2> Seq<Tuple2<T1, T2>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, T1 default1, T2 default2) {
        return zipAll(s1, s2, default1, default2, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, T1 default1, T2 default2, T3 default3) {
        return zipAll(s1, s2, s3, default1, default2, default3, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, T1 default1, T2 default2, T3 default3, T4 default4) {
        return zipAll(s1, s2, s3, s4, default1, default2, default3, default4, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5) {
        return zipAll(s1, s2, s3, s4, s5, default1, default2, default3, default4, default5, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6) {
        return zipAll(s1, s2, s3, s4, s5, s6, default1, default2, default3, default4, default5, default6, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, default1, default2, default3, default4, default5, default6, default7, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, default1, default2, default3, default4, default5, default6, default7, default8, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, default1, default2, default3, default4, default5, default6, default7, default8, default9, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15, Stream<? extends T16> s16, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15, T16 default16) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, default16, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2> Seq<Tuple2<T1, T2>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, T1 default1, T2 default2) {
        return zipAll(s1, s2, default1, default2, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, T1 default1, T2 default2, T3 default3) {
        return zipAll(s1, s2, s3, default1, default2, default3, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, T1 default1, T2 default2, T3 default3, T4 default4) {
        return zipAll(s1, s2, s3, s4, default1, default2, default3, default4, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5) {
        return zipAll(s1, s2, s3, s4, s5, default1, default2, default3, default4, default5, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6) {
        return zipAll(s1, s2, s3, s4, s5, s6, default1, default2, default3, default4, default5, default6, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, default1, default2, default3, default4, default5, default6, default7, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, default1, default2, default3, default4, default5, default6, default7, default8, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, default1, default2, default3, default4, default5, default6, default7, default8, default9, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, Iterable<? extends T13> s13, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, Iterable<? extends T13> s13, Iterable<? extends T14> s14, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, Iterable<? extends T13> s13, Iterable<? extends T14> s14, Iterable<? extends T15> s15, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, Iterable<? extends T13> s13, Iterable<? extends T14> s14, Iterable<? extends T15> s15, Iterable<? extends T16> s16, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15, T16 default16) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, default16, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2> Seq<Tuple2<T1, T2>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, T1 default1, T2 default2) {
        return zipAll(s1, s2, default1, default2, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, T1 default1, T2 default2, T3 default3) {
        return zipAll(s1, s2, s3, default1, default2, default3, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, T1 default1, T2 default2, T3 default3, T4 default4) {
        return zipAll(s1, s2, s3, s4, default1, default2, default3, default4, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5) {
        return zipAll(s1, s2, s3, s4, s5, default1, default2, default3, default4, default5, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6) {
        return zipAll(s1, s2, s3, s4, s5, s6, default1, default2, default3, default4, default5, default6, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, default1, default2, default3, default4, default5, default6, default7, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, default1, default2, default3, default4, default5, default6, default7, default8, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, default1, default2, default3, default4, default5, default6, default7, default8, default9, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15, Seq<? extends T16> s16, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15, T16 default16) {
        return zipAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, default16, Tuple::tuple);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, T1 default1, T2 default2, BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), default1, default2, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, T1 default1, T2 default2, T3 default3, Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), default1, default2, default3, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, T1 default1, T2 default2, T3 default3, T4 default4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), default1, default2, default3, default4, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), default1, default2, default3, default4, default5, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), default1, default2, default3, default4, default5, default6, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), default1, default2, default3, default4, default5, default6, default7, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), default1, default2, default3, default4, default5, default6, default7, default8, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), default1, default2, default3, default4, default5, default6, default7, default8, default9, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, Function10<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, Function11<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, Function12<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, Function13<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, Function14<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15, Function15<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> Seq<R> zipAll(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15, Stream<? extends T16> s16, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15, T16 default16, Function16<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? super T16, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15), seq(s16), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, default16, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, T1 default1, T2 default2, BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), default1, default2, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, T1 default1, T2 default2, T3 default3, Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), default1, default2, default3, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, T1 default1, T2 default2, T3 default3, T4 default4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), default1, default2, default3, default4, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), default1, default2, default3, default4, default5, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), default1, default2, default3, default4, default5, default6, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), default1, default2, default3, default4, default5, default6, default7, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), default1, default2, default3, default4, default5, default6, default7, default8, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), default1, default2, default3, default4, default5, default6, default7, default8, default9, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, Function10<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, Function11<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, Function12<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, Iterable<? extends T13> s13, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, Function13<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, Iterable<? extends T13> s13, Iterable<? extends T14> s14, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, Function14<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, Iterable<? extends T13> s13, Iterable<? extends T14> s14, Iterable<? extends T15> s15, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15, Function15<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, zipper);
    }

    /**
     * Zip two streams into one - by storing the corresponding elements from them in a tuple,
     * when one of streams will end - a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "x"), tuple(3, "x"))
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x")
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> Seq<R> zipAll(Iterable<? extends T1> s1, Iterable<? extends T2> s2, Iterable<? extends T3> s3, Iterable<? extends T4> s4, Iterable<? extends T5> s5, Iterable<? extends T6> s6, Iterable<? extends T7> s7, Iterable<? extends T8> s8, Iterable<? extends T9> s9, Iterable<? extends T10> s10, Iterable<? extends T11> s11, Iterable<? extends T12> s12, Iterable<? extends T13> s13, Iterable<? extends T14> s14, Iterable<? extends T15> s15, Iterable<? extends T16> s16, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15, T16 default16, Function16<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? super T16, ? extends R> zipper) {
        return zipAll(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15), seq(s16), default1, default2, default3, default4, default5, default6, default7, default8, default9, default10, default11, default12, default13, default14, default15, default16, zipper);
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, T1 default1, T2 default2, BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();

                if (!b1 && !b2)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, T1 default1, T2 default2, T3 default3, Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();

                if (!b1 && !b2 && !b3)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, T1 default1, T2 default2, T3 default3, T4 default4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();

                if (!b1 && !b2 && !b3 && !b4)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext() || it8.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();
                boolean b8 = it8.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7 && !b8)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7,
                    b8 ? it8.next() : default8
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext() || it8.hasNext() || it9.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();
                boolean b8 = it8.hasNext();
                boolean b9 = it9.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7 && !b8 && !b9)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7,
                    b8 ? it8.next() : default8,
                    b9 ? it9.next() : default9
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, Function10<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext() || it8.hasNext() || it9.hasNext() || it10.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();
                boolean b8 = it8.hasNext();
                boolean b9 = it9.hasNext();
                boolean b10 = it10.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7 && !b8 && !b9 && !b10)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7,
                    b8 ? it8.next() : default8,
                    b9 ? it9.next() : default9,
                    b10 ? it10.next() : default10
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, Function11<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext() || it8.hasNext() || it9.hasNext() || it10.hasNext() || it11.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();
                boolean b8 = it8.hasNext();
                boolean b9 = it9.hasNext();
                boolean b10 = it10.hasNext();
                boolean b11 = it11.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7 && !b8 && !b9 && !b10 && !b11)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7,
                    b8 ? it8.next() : default8,
                    b9 ? it9.next() : default9,
                    b10 ? it10.next() : default10,
                    b11 ? it11.next() : default11
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, Function12<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext() || it8.hasNext() || it9.hasNext() || it10.hasNext() || it11.hasNext() || it12.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();
                boolean b8 = it8.hasNext();
                boolean b9 = it9.hasNext();
                boolean b10 = it10.hasNext();
                boolean b11 = it11.hasNext();
                boolean b12 = it12.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7 && !b8 && !b9 && !b10 && !b11 && !b12)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7,
                    b8 ? it8.next() : default8,
                    b9 ? it9.next() : default9,
                    b10 ? it10.next() : default10,
                    b11 ? it11.next() : default11,
                    b12 ? it12.next() : default12
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, Function13<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();
        final Iterator<? extends T13> it13 = s13.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext() || it8.hasNext() || it9.hasNext() || it10.hasNext() || it11.hasNext() || it12.hasNext() || it13.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();
                boolean b8 = it8.hasNext();
                boolean b9 = it9.hasNext();
                boolean b10 = it10.hasNext();
                boolean b11 = it11.hasNext();
                boolean b12 = it12.hasNext();
                boolean b13 = it13.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7 && !b8 && !b9 && !b10 && !b11 && !b12 && !b13)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7,
                    b8 ? it8.next() : default8,
                    b9 ? it9.next() : default9,
                    b10 ? it10.next() : default10,
                    b11 ? it11.next() : default11,
                    b12 ? it12.next() : default12,
                    b13 ? it13.next() : default13
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, Function14<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();
        final Iterator<? extends T13> it13 = s13.iterator();
        final Iterator<? extends T14> it14 = s14.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext() || it8.hasNext() || it9.hasNext() || it10.hasNext() || it11.hasNext() || it12.hasNext() || it13.hasNext() || it14.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();
                boolean b8 = it8.hasNext();
                boolean b9 = it9.hasNext();
                boolean b10 = it10.hasNext();
                boolean b11 = it11.hasNext();
                boolean b12 = it12.hasNext();
                boolean b13 = it13.hasNext();
                boolean b14 = it14.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7 && !b8 && !b9 && !b10 && !b11 && !b12 && !b13 && !b14)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7,
                    b8 ? it8.next() : default8,
                    b9 ? it9.next() : default9,
                    b10 ? it10.next() : default10,
                    b11 ? it11.next() : default11,
                    b12 ? it12.next() : default12,
                    b13 ? it13.next() : default13,
                    b14 ? it14.next() : default14
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15, Function15<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();
        final Iterator<? extends T13> it13 = s13.iterator();
        final Iterator<? extends T14> it14 = s14.iterator();
        final Iterator<? extends T15> it15 = s15.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext() || it8.hasNext() || it9.hasNext() || it10.hasNext() || it11.hasNext() || it12.hasNext() || it13.hasNext() || it14.hasNext() || it15.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();
                boolean b8 = it8.hasNext();
                boolean b9 = it9.hasNext();
                boolean b10 = it10.hasNext();
                boolean b11 = it11.hasNext();
                boolean b12 = it12.hasNext();
                boolean b13 = it13.hasNext();
                boolean b14 = it14.hasNext();
                boolean b15 = it15.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7 && !b8 && !b9 && !b10 && !b11 && !b12 && !b13 && !b14 && !b15)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7,
                    b8 ? it8.next() : default8,
                    b9 ? it9.next() : default9,
                    b10 ? it10.next() : default10,
                    b11 ? it11.next() : default11,
                    b12 ? it12.next() : default12,
                    b13 ? it13.next() : default13,
                    b14 ? it14.next() : default14,
                    b15 ? it15.next() : default15
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15));
    }

    /**
     * Zip two streams into one using a {@link BiFunction} to produce resulting values,
     * when one of streams will end, a default value for that stream will be provided instead -
     * so the resulting stream will be as long as the longest of the two streams.
     * <p>
     * <code><pre>
     * // ("1:a", "2:x", "3:x")
     * Seq.zipAll(Seq.of(1, 2, 3), Seq.of("a"), 0, "x", (i, s) -> i + ":" + s)
     * </pre></code>
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> Seq<R> zipAll(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15, Seq<? extends T16> s16, T1 default1, T2 default2, T3 default3, T4 default4, T5 default5, T6 default6, T7 default7, T8 default8, T9 default9, T10 default10, T11 default11, T12 default12, T13 default13, T14 default14, T15 default15, T16 default16, Function16<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? super T16, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();
        final Iterator<? extends T13> it13 = s13.iterator();
        final Iterator<? extends T14> it14 = s14.iterator();
        final Iterator<? extends T15> it15 = s15.iterator();
        final Iterator<? extends T16> it16 = s16.iterator();

        class ZipAll implements Iterator<R> {

            @Override
            public boolean hasNext() {
                return it1.hasNext() || it2.hasNext() || it3.hasNext() || it4.hasNext() || it5.hasNext() || it6.hasNext() || it7.hasNext() || it8.hasNext() || it9.hasNext() || it10.hasNext() || it11.hasNext() || it12.hasNext() || it13.hasNext() || it14.hasNext() || it15.hasNext() || it16.hasNext();
            }

            @Override
            public R next() {
                boolean b1 = it1.hasNext();
                boolean b2 = it2.hasNext();
                boolean b3 = it3.hasNext();
                boolean b4 = it4.hasNext();
                boolean b5 = it5.hasNext();
                boolean b6 = it6.hasNext();
                boolean b7 = it7.hasNext();
                boolean b8 = it8.hasNext();
                boolean b9 = it9.hasNext();
                boolean b10 = it10.hasNext();
                boolean b11 = it11.hasNext();
                boolean b12 = it12.hasNext();
                boolean b13 = it13.hasNext();
                boolean b14 = it14.hasNext();
                boolean b15 = it15.hasNext();
                boolean b16 = it16.hasNext();

                if (!b1 && !b2 && !b3 && !b4 && !b5 && !b6 && !b7 && !b8 && !b9 && !b10 && !b11 && !b12 && !b13 && !b14 && !b15 && !b16)
                    throw new NoSuchElementException("next on empty iterator");

                return zipper.apply(
                    b1 ? it1.next() : default1,
                    b2 ? it2.next() : default2,
                    b3 ? it3.next() : default3,
                    b4 ? it4.next() : default4,
                    b5 ? it5.next() : default5,
                    b6 ? it6.next() : default6,
                    b7 ? it7.next() : default7,
                    b8 ? it8.next() : default8,
                    b9 ? it9.next() : default9,
                    b10 ? it10.next() : default10,
                    b11 ? it11.next() : default11,
                    b12 ? it12.next() : default12,
                    b13 ? it13.next() : default13,
                    b14 ? it14.next() : default14,
                    b15 ? it15.next() : default15,
                    b16 ? it16.next() : default16
                );
            }
        }

        return seq(new ZipAll()).onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16));
    }

// [jooq-tools] END [zip-all-static]

    /**
     * Zip a Stream with a corresponding Stream of indexes.
     * <p>
     * <code><pre>
     * // (tuple("a", 0), tuple("b", 1), tuple("c", 2))
     * Seq.of("a", "b", "c").zipWithIndex()
     * </pre></code>
     *
     * @see #zipWithIndex(Stream)
     */
    default Seq<Tuple2<T, Long>> zipWithIndex() {
        return zipWithIndex(this);
    }

    /**
     * Zip a stream with indexes into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("0:a", "1:b", "2:c")
     * Seq.of("a", "b", "c").zipWithIndex((s, i) -> i + ":" + s))
     * </pre></code>
     *
     * @see #zipWithIndex(Seq, BiFunction)
     */
    default <R> Seq<R> zipWithIndex(BiFunction<? super T, ? super Long, ? extends R> zipper) {
        return zipWithIndex(this, zipper);
    }

    /**
     * Fold a Stream to the left.
     * <p>
     * <code><pre>
     * // "!abc"
     * Seq.of("a", "b", "c").foldLeft("!", (u, t) -> u + t)
     * </pre></code>
     */
    default <U> U foldLeft(U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        return foldLeft(this, seed, function);
    }

    /**
     * Fold a Stream to the right.
     * <p>
     * <code><pre>
     * // "abc!"
     * Seq.of("a", "b", "c").foldRight("!", (t, u) -> t + u)
     * </pre></code>
     */
    default <U> U foldRight(U seed, BiFunction<? super T, ? super U, ? extends U> function) {
        return foldRight(this, seed, function);
    }

    /**
     * Scan a stream to the left.
     * <p>
     * <code><pre>
     * // ("", "a", "ab", "abc")
     * Seq.of("a", "b", "c").scanLeft("", (u, t) -> u + t)
     * </pre></code>
     */
    default <U> Seq<U> scanLeft(U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        return scanLeft(this, seed, function);
    }

    /**
     * Scan a stream to the right.
     * <p>
     * <code><pre>
     * // ("", "c", "cb", "cba")
     * Seq.of("a", "b", "c").scanRight("", (t, u) -> u + t)
     * </pre></code>
     */
    default <U> Seq<U> scanRight(U seed, BiFunction<? super T, ? super U, ? extends U> function) {
        return scanRight(this, seed, function);
    }

    /**
     * Reverse a stream.
     * <p>
     * <code><pre>
     * // (3, 2, 1)
     * Seq.of(1, 2, 3).reverse()
     * </pre></code>
     */
    default Seq<T> reverse() {
        return reverse(this);
    }

    /**
     * Shuffle a stream
     * <p>
     * <code><pre>
     * // e.g. (2, 3, 1)
     * Seq.of(1, 2, 3).shuffle()
     * </pre></code>
     */
    default Seq<T> shuffle() {
        return shuffle(this);
    }

    /**
     * Shuffle a stream using specified source of randomness
     * <p>
     * <code><pre>
     * // e.g. (2, 3, 1)
     * Seq.of(1, 2, 3).shuffle(new Random())
     * </pre></code>
     */
    default Seq<T> shuffle(Random random) {
        return shuffle(this, random);
    }

    /**
     * Returns a stream with all elements skipped for which a predicate evaluates to <code>true</code>.
     * <p>
     * <code><pre>
     * // (3, 4, 5)
     * Seq.of(1, 2, 3, 4, 5).skipWhile(i -> i &lt; 3)
     * </pre></code>
     *
     * @see #skipWhile(Stream, Predicate)
     */
    default Seq<T> skipWhile(Predicate<? super T> predicate) {
        return skipWhile(this, predicate);
    }

    /**
     * Returns a stream with all elements skipped for which a predicate evaluates to <code>true</code>
     * plus the first element for which it evaluates to false.
     * <p>
     * <code><pre>
     * // (4, 5)
     * Seq.of(1, 2, 3, 4, 5).skipWhileClosed(i -> i &lt; 3)
     * </pre></code>
     *
     * @see #skipWhileClosed(Stream, Predicate)
     */
    default Seq<T> skipWhileClosed(Predicate<? super T> predicate) {
        return skipWhileClosed(this, predicate);
    }

    /**
     * Returns a stream with all elements skipped for which a predicate evaluates to <code>false</code>.
     * <p>
     * <code><pre>
     * // (3, 4, 5)
     * Seq.of(1, 2, 3, 4, 5).skipUntil(i -> i == 3)
     * </pre></code>
     *
     * @see #skipUntil(Stream, Predicate)
     */
    default Seq<T> skipUntil(Predicate<? super T> predicate) {
        return skipUntil(this, predicate);
    }

    /**
     * Returns a stream with all elements skipped for which a predicate evaluates to <code>false</code>
     * plus the first element for which it evaluates to <code>true</code>.
     * <p>
     * <code><pre>
     * // (4, 5)
     * Seq.of(1, 2, 3, 4, 5).skipUntilClosed(i -> i == 3)
     * </pre></code>
     *
     * @see #skipUntilClosed(Stream, Predicate)
     */
    default Seq<T> skipUntilClosed(Predicate<? super T> predicate) {
        return skipUntilClosed(this, predicate);
    }

    /**
     * Returns a stream limited to all elements for which a predicate evaluates to <code>true</code>.
     * <p>
     * <code><pre>
     * // (1, 2)
     * Seq.of(1, 2, 3, 4, 5).limitWhile(i -> i &lt; 3)
     * </pre></code>
     *
     * @see #limitWhile(Stream, Predicate)
     */
    default Seq<T> limitWhile(Predicate<? super T> predicate) {
        return limitWhile(this, predicate);
    }

    /**
     * Returns a stream limited to all elements for which a predicate evaluates to <code>true</code>
     * plus the first element for which it evaluates to <code>false</code>.
     * <p>
     * <code><pre>
     * // (1, 2, 3)
     * Seq.of(1, 2, 3, 4, 5).limitWhileClosed(i -> i &lt; 3)
     * </pre></code>
     *
     * @see #limitWhileClosed(Stream, Predicate)
     */
    default Seq<T> limitWhileClosed(Predicate<? super T> predicate) {
        return limitWhileClosed(this, predicate);
    }

    /**
     * Returns a stream limited to all elements for which a predicate evaluates to <code>false</code>.
     * <p>
     * <code><pre>
     * // (1, 2)
     * Seq.of(1, 2, 3, 4, 5).limitUntil(i -> i == 3)
     * </pre></code>
     *
     * @see #limitUntil(Stream, Predicate)
     */
    default Seq<T> limitUntil(Predicate<? super T> predicate) {
        return limitUntil(this, predicate);
    }

    /**
     * Returns a stream limited to all elements for which a predicate evaluates to <code>false</code>
     * plus the first element for which it evaluates to <code>true</code>.
     * <p>
     * <code><pre>
     * // (1, 2, 3)
     * Seq.of(1, 2, 3, 4, 5).limitUntilClosed(i -> i == 3)
     * </pre></code>
     *
     * @see #limitUntilClosed(Stream, Predicate)
     */
    default Seq<T> limitUntilClosed(Predicate<? super T> predicate) {
        return limitUntilClosed(this, predicate);
    }

    /**
     * Returns a stream with a given value interspersed between any two values of this stream.
     * <p>
     * <code><pre>
     * // (1, 0, 2, 0, 3, 0, 4)
     * Seq.of(1, 2, 3, 4).intersperse(0)
     * </pre></code>
     *
     * @see #intersperse(Stream, Object)
     */
    default Seq<T> intersperse(T value) {
        return intersperse(this, value);
    }

    /**
     * Duplicate a Streams into two equivalent Streams.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (1, 2, 3))
     * Seq.of(1, 2, 3).duplicate()
     * </pre></code>
     *
     * @see #duplicate(Stream)
     */
    default Tuple2<Seq<T>, Seq<T>> duplicate() {
        return duplicate(this);
    }

    /**
     * Classify this stream's elements according to a given classifier function.
     * <p>
     * <code><pre>
     * // Seq(tuple(1, Seq(1, 3, 5)), tuple(0, Seq(2, 4, 6)))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2)
     * // Seq(tuple(true, Seq(1, 3, 5)), tuple(false, Seq(2, 4, 6)))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 != 0)
     * </pre></code>
     *
     * This is a non-terminal analog of {@link #groupBy(Function)})
     * @see #groupBy(Function)
     * @see #partition(Predicate)
     */
    default <K> Seq<Tuple2<K, Seq<T>>> grouped(Function<? super T, ? extends K> classifier) {
        return grouped(this, classifier);
    }

    /**
     * Classify this stream's elements according to a given classifier function
     * and collect each class's elements using a collector.
     * <p>
     * <code><pre>
     * // Seq(tuple(1, 9), tuple(0, 12))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2, Collectors.summingInt(i -> i))
     * // Seq(tuple(true, 9), tuple(false, 12))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 != 0, Collectors.summingInt(i -> i))
     * </pre></code> This is a non-terminal analog of
     * {@link #groupBy(Function, Collector)})
     *
     * @see #groupBy(Function, Collector)
     */
    default <K, A, D> Seq<Tuple2<K, D>> grouped(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return grouped(this, classifier, downstream);
    }

    /**
     * Partition a stream into two given a predicate.
     * <p>
     * <code><pre>
     * // tuple((1, 3, 5), (2, 4, 6))
     * Seq.of(1, 2, 3, 4, 5, 6).partition(i -> i % 2 != 0)
     * </pre></code>
     *
     * @see #partition(Stream, Predicate)
     */
    default Tuple2<Seq<T>, Seq<T>> partition(Predicate<? super T> predicate) {
        return partition(this, predicate);
    }

    /**
     * Split a stream at a given position.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (4, 5, 6))
     * Seq.of(1, 2, 3, 4, 5, 6).splitAt(3)
     * </pre></code>
     *
     * @see #splitAt(Stream, long)
     */
    default Tuple2<Seq<T>, Seq<T>> splitAt(long position) {
        return splitAt(this, position);
    }

    /**
     * Split a stream at the head.
     * <p>
     * <code><pre>
     * // tuple(1, (2, 3, 4, 5, 6))
     * Seq.of(1, 2, 3, 4, 5, 6).splitHead(3)
     * </pre></code>
     *
     * @see #splitAt(Stream, long)
     */
    default Tuple2<Optional<T>, Seq<T>> splitAtHead() {
        return splitAtHead(this);
    }

    /**
     * Returns a limited interval from a given Stream.
     * <p>
     * <code><pre>
     * // (4, 5)
     * Seq.of(1, 2, 3, 4, 5, 6).slice(3, 5)
     * </pre></code>
     *
     * @see #slice(Stream, long, long)
     */
    default Seq<T> slice(long from, long to) {
        return slice(this, from, to);
    }

    /**
     * Check if the sequence has any elements
     */
    default boolean isEmpty() {
        return !findAny().isPresent();
    }

    /**
     * Check if the sequence has no elements
     */
    default boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Sort by the results of function.
     */
    default <U extends Comparable<? super U>> Seq<T> sorted(Function<? super T, ? extends U> function) {
        return sorted(comparing(function));
    }

    /**
     * Sort by the results of function.
     */
    default <U> Seq<T> sorted(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return sorted(comparing(function, comparator));
    }

    // Methods taken from LINQ
    // -----------------------

    /**
     * Keep only those elements in a stream that are of a given type.
     * <p>
     * <code><pre>
     * // (1, 2, 3)
     * Seq.of(1, "a", 2, "b", 3).ofType(Integer.class)
     * </pre></code>
     *
     * @see #ofType(Stream, Class)
     */
    default <U> Seq<U> ofType(Class<? extends U> type) {
        return ofType(this, type);
    }

    /**
     * Cast all elements in a stream to a given type, possibly throwing a {@link ClassCastException}.
     * <p>
     * <code><pre>
     * // ClassCastException
     * Seq.of(1, "a", 2, "b", 3).cast(Integer.class)
     * </pre></code>
     *
     * @see #cast(Stream, Class)
     * @see #ofType(Class) Seq.ofType(Class) If you want to filter and cast
     */
    default <U> Seq<U> cast(Class<? extends U> type) {
        return cast(this, type);
    }

    /**
     * Map this stream to a stream containing a sliding window over the previous stream.
     * <p>
     * <code><pre>
     * // ((1, 2, 3), (2, 3, 4), (3, 4, 5))
     * .of(1, 2, 3, 4, 5).sliding(3);
     * </pre></code>
     * <p>
     * This is equivalent as using the more verbose window function version:
     * <code><pre>
     * int n = 3;
     * Seq.of(1, 2, 3, 4, 5)
     *    .window(0, n - 1)
     *    .filter(w -> w.count() == n)
     *    .map(w -> w.toList());
     * </pre></code>
     */
    default Seq<Seq<T>> sliding(long size) {
        if (size <= 0)
            throw new IllegalArgumentException("Size must be >= 1");

        return window(0, size - 1).filter(w -> w.count() == size).map(w -> w.window());
    }

    /**
     * Map this stream to a windowed stream using the default partition and order.
     * <p>
     * <code><pre>
     * // (0, 1, 2, 3, 4)
     * Seq.of(1, 2, 4, 2, 3).window().map(Window::rowNumber)
     * </pre></code>
     */
    default Seq<Window<T>> window() {
        return window(Window.of()).map(t -> t.v1);
    }

    /**
     * Map this stream to a windowed stream using the default partition and order with frame.
     * <p>
     * <code><pre>
     * // (2, 4, 4, 4, 3)
     * Seq.of(1, 2, 4, 2, 3).window(-1, 1).map(Window::max)
     * </pre></code>
     */
    default Seq<Window<T>> window(long lower, long upper) {
        return window(Window.of(lower, upper)).map(t -> t.v1);
    }

    /**
     * Map this stream to a windowed stream using the default partition and a specific order.
     * <p>
     * <code><pre>
     * // (0, 1, 4, 2, 3)
     * Seq.of(1, 2, 4, 2, 3).window(naturalOrder()).map(Window::rowNumber)
     * </pre></code>
     */
    default Seq<Window<T>> window(Comparator<? super T> orderBy) {
        return window(Window.of(orderBy)).map(t -> t.v1);
    }

    /**
     * Map this stream to a windowed stream using the default partition and a specific order with frame.
     * <p>
     * <code><pre>
     * // (1, 1, 3, 2, 2)
     * Seq.of(1, 2, 4, 2, 3).window(naturalOrder(), -1, 1).map(Window::min)
     * </pre></code>
     */
    default Seq<Window<T>> window(Comparator<? super T> orderBy, long lower, long upper) {
        return window(Window.of(orderBy, lower, upper)).map(t -> t.v1);
    }

    /**
     * Map this stream to a windowed stream using a specific partition and the default order.
     * <p>
     * <code><pre>
     * // (1, 2, 2, 2, 1)
     * Seq.of(1, 2, 4, 2, 3).window(i -> i % 2).map(Window::min)
     * </pre></code>
     */
    default <U> Seq<Window<T>> window(Function<? super T, ? extends U> partitionBy) {
        return window(Window.of(partitionBy)).map(t -> t.v1);
    }

    /**
     * Map this stream to a windowed stream using a specific partition and the default order.
     * <p>
     * <code><pre>
     * // (3, 4, 4, 2, 3)
     * Seq.of(1, 4, 2, 2, 3).window(i -> i % 2, -1, 1).map(Window::max)
     * </pre></code>
     */
    default <U> Seq<Window<T>> window(Function<? super T, ? extends U> partitionBy, long lower, long upper) {
        return window(Window.of(partitionBy, lower, upper)).map(t -> t.v1);
    }

    /**
     * Map this stream to a windowed stream using a specific partition and order.
     * <p>
     * <code><pre>
     * // (1, 2, 4, 4, 3)
     * Seq.of(1, 2, 4, 2, 3).window(i -> i % 2, naturalOrder()).map(Window::max)
     * </pre></code>
     */
    default <U> Seq<Window<T>> window(Function<? super T, ? extends U> partitionBy, Comparator<? super T> orderBy) {
        return window(Window.of(partitionBy, orderBy)).map(t -> t.v1);
    }

    /**
     * Map this stream to a windowed stream using a specific partition and order with frame.
     * <p>
     * <code><pre>
     * // (3, 2, 4, 4, 3)
     * Seq.of(1, 2, 4, 2, 3).window(i -> i % 2, naturalOrder(), -1, 1).map(Window::max)
     * </pre></code>
     */
    default <U> Seq<Window<T>> window(Function<? super T, ? extends U> partitionBy, Comparator<? super T> orderBy, long lower, long upper) {
        return window(Window.of(partitionBy, orderBy, lower, upper)).map(t -> t.v1);
    }

    // [jooq-tools] START [windows]

    /**
     * Map this stream to a windowed stream with 1 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple1<Window<T>>> window(
        WindowSpecification<T> specification1
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 2 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple2<Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 3 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple3<Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 4 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple4<Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 5 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple5<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 6 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple6<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 7 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple7<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 8 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple8<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7,
        WindowSpecification<T> specification8
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);
        Map<?, Partition<T>> partitions8 = SeqUtils.partitions(specification8, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7),
                   (Window<T>) new WindowImpl<>(t, partitions8.get(specification8.partition().apply(t.v1)), specification8)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 9 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple9<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7,
        WindowSpecification<T> specification8,
        WindowSpecification<T> specification9
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);
        Map<?, Partition<T>> partitions8 = SeqUtils.partitions(specification8, buffer);
        Map<?, Partition<T>> partitions9 = SeqUtils.partitions(specification9, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7),
                   (Window<T>) new WindowImpl<>(t, partitions8.get(specification8.partition().apply(t.v1)), specification8),
                   (Window<T>) new WindowImpl<>(t, partitions9.get(specification9.partition().apply(t.v1)), specification9)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 10 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple10<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7,
        WindowSpecification<T> specification8,
        WindowSpecification<T> specification9,
        WindowSpecification<T> specification10
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);
        Map<?, Partition<T>> partitions8 = SeqUtils.partitions(specification8, buffer);
        Map<?, Partition<T>> partitions9 = SeqUtils.partitions(specification9, buffer);
        Map<?, Partition<T>> partitions10 = SeqUtils.partitions(specification10, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7),
                   (Window<T>) new WindowImpl<>(t, partitions8.get(specification8.partition().apply(t.v1)), specification8),
                   (Window<T>) new WindowImpl<>(t, partitions9.get(specification9.partition().apply(t.v1)), specification9),
                   (Window<T>) new WindowImpl<>(t, partitions10.get(specification10.partition().apply(t.v1)), specification10)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 11 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple11<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7,
        WindowSpecification<T> specification8,
        WindowSpecification<T> specification9,
        WindowSpecification<T> specification10,
        WindowSpecification<T> specification11
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);
        Map<?, Partition<T>> partitions8 = SeqUtils.partitions(specification8, buffer);
        Map<?, Partition<T>> partitions9 = SeqUtils.partitions(specification9, buffer);
        Map<?, Partition<T>> partitions10 = SeqUtils.partitions(specification10, buffer);
        Map<?, Partition<T>> partitions11 = SeqUtils.partitions(specification11, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7),
                   (Window<T>) new WindowImpl<>(t, partitions8.get(specification8.partition().apply(t.v1)), specification8),
                   (Window<T>) new WindowImpl<>(t, partitions9.get(specification9.partition().apply(t.v1)), specification9),
                   (Window<T>) new WindowImpl<>(t, partitions10.get(specification10.partition().apply(t.v1)), specification10),
                   (Window<T>) new WindowImpl<>(t, partitions11.get(specification11.partition().apply(t.v1)), specification11)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 12 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple12<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7,
        WindowSpecification<T> specification8,
        WindowSpecification<T> specification9,
        WindowSpecification<T> specification10,
        WindowSpecification<T> specification11,
        WindowSpecification<T> specification12
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);
        Map<?, Partition<T>> partitions8 = SeqUtils.partitions(specification8, buffer);
        Map<?, Partition<T>> partitions9 = SeqUtils.partitions(specification9, buffer);
        Map<?, Partition<T>> partitions10 = SeqUtils.partitions(specification10, buffer);
        Map<?, Partition<T>> partitions11 = SeqUtils.partitions(specification11, buffer);
        Map<?, Partition<T>> partitions12 = SeqUtils.partitions(specification12, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7),
                   (Window<T>) new WindowImpl<>(t, partitions8.get(specification8.partition().apply(t.v1)), specification8),
                   (Window<T>) new WindowImpl<>(t, partitions9.get(specification9.partition().apply(t.v1)), specification9),
                   (Window<T>) new WindowImpl<>(t, partitions10.get(specification10.partition().apply(t.v1)), specification10),
                   (Window<T>) new WindowImpl<>(t, partitions11.get(specification11.partition().apply(t.v1)), specification11),
                   (Window<T>) new WindowImpl<>(t, partitions12.get(specification12.partition().apply(t.v1)), specification12)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 13 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple13<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7,
        WindowSpecification<T> specification8,
        WindowSpecification<T> specification9,
        WindowSpecification<T> specification10,
        WindowSpecification<T> specification11,
        WindowSpecification<T> specification12,
        WindowSpecification<T> specification13
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);
        Map<?, Partition<T>> partitions8 = SeqUtils.partitions(specification8, buffer);
        Map<?, Partition<T>> partitions9 = SeqUtils.partitions(specification9, buffer);
        Map<?, Partition<T>> partitions10 = SeqUtils.partitions(specification10, buffer);
        Map<?, Partition<T>> partitions11 = SeqUtils.partitions(specification11, buffer);
        Map<?, Partition<T>> partitions12 = SeqUtils.partitions(specification12, buffer);
        Map<?, Partition<T>> partitions13 = SeqUtils.partitions(specification13, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7),
                   (Window<T>) new WindowImpl<>(t, partitions8.get(specification8.partition().apply(t.v1)), specification8),
                   (Window<T>) new WindowImpl<>(t, partitions9.get(specification9.partition().apply(t.v1)), specification9),
                   (Window<T>) new WindowImpl<>(t, partitions10.get(specification10.partition().apply(t.v1)), specification10),
                   (Window<T>) new WindowImpl<>(t, partitions11.get(specification11.partition().apply(t.v1)), specification11),
                   (Window<T>) new WindowImpl<>(t, partitions12.get(specification12.partition().apply(t.v1)), specification12),
                   (Window<T>) new WindowImpl<>(t, partitions13.get(specification13.partition().apply(t.v1)), specification13)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 14 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple14<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7,
        WindowSpecification<T> specification8,
        WindowSpecification<T> specification9,
        WindowSpecification<T> specification10,
        WindowSpecification<T> specification11,
        WindowSpecification<T> specification12,
        WindowSpecification<T> specification13,
        WindowSpecification<T> specification14
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);
        Map<?, Partition<T>> partitions8 = SeqUtils.partitions(specification8, buffer);
        Map<?, Partition<T>> partitions9 = SeqUtils.partitions(specification9, buffer);
        Map<?, Partition<T>> partitions10 = SeqUtils.partitions(specification10, buffer);
        Map<?, Partition<T>> partitions11 = SeqUtils.partitions(specification11, buffer);
        Map<?, Partition<T>> partitions12 = SeqUtils.partitions(specification12, buffer);
        Map<?, Partition<T>> partitions13 = SeqUtils.partitions(specification13, buffer);
        Map<?, Partition<T>> partitions14 = SeqUtils.partitions(specification14, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7),
                   (Window<T>) new WindowImpl<>(t, partitions8.get(specification8.partition().apply(t.v1)), specification8),
                   (Window<T>) new WindowImpl<>(t, partitions9.get(specification9.partition().apply(t.v1)), specification9),
                   (Window<T>) new WindowImpl<>(t, partitions10.get(specification10.partition().apply(t.v1)), specification10),
                   (Window<T>) new WindowImpl<>(t, partitions11.get(specification11.partition().apply(t.v1)), specification11),
                   (Window<T>) new WindowImpl<>(t, partitions12.get(specification12.partition().apply(t.v1)), specification12),
                   (Window<T>) new WindowImpl<>(t, partitions13.get(specification13.partition().apply(t.v1)), specification13),
                   (Window<T>) new WindowImpl<>(t, partitions14.get(specification14.partition().apply(t.v1)), specification14)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 15 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple15<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7,
        WindowSpecification<T> specification8,
        WindowSpecification<T> specification9,
        WindowSpecification<T> specification10,
        WindowSpecification<T> specification11,
        WindowSpecification<T> specification12,
        WindowSpecification<T> specification13,
        WindowSpecification<T> specification14,
        WindowSpecification<T> specification15
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);
        Map<?, Partition<T>> partitions8 = SeqUtils.partitions(specification8, buffer);
        Map<?, Partition<T>> partitions9 = SeqUtils.partitions(specification9, buffer);
        Map<?, Partition<T>> partitions10 = SeqUtils.partitions(specification10, buffer);
        Map<?, Partition<T>> partitions11 = SeqUtils.partitions(specification11, buffer);
        Map<?, Partition<T>> partitions12 = SeqUtils.partitions(specification12, buffer);
        Map<?, Partition<T>> partitions13 = SeqUtils.partitions(specification13, buffer);
        Map<?, Partition<T>> partitions14 = SeqUtils.partitions(specification14, buffer);
        Map<?, Partition<T>> partitions15 = SeqUtils.partitions(specification15, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7),
                   (Window<T>) new WindowImpl<>(t, partitions8.get(specification8.partition().apply(t.v1)), specification8),
                   (Window<T>) new WindowImpl<>(t, partitions9.get(specification9.partition().apply(t.v1)), specification9),
                   (Window<T>) new WindowImpl<>(t, partitions10.get(specification10.partition().apply(t.v1)), specification10),
                   (Window<T>) new WindowImpl<>(t, partitions11.get(specification11.partition().apply(t.v1)), specification11),
                   (Window<T>) new WindowImpl<>(t, partitions12.get(specification12.partition().apply(t.v1)), specification12),
                   (Window<T>) new WindowImpl<>(t, partitions13.get(specification13.partition().apply(t.v1)), specification13),
                   (Window<T>) new WindowImpl<>(t, partitions14.get(specification14.partition().apply(t.v1)), specification14),
                   (Window<T>) new WindowImpl<>(t, partitions15.get(specification15.partition().apply(t.v1)), specification15)
              ))
              .onClose(this::close);
    }

    /**
     * Map this stream to a windowed stream with 16 distinct windows.
     */
    /// @Generated("This method was generated using jOOQ-tools")
    default Seq<Tuple16<Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>, Window<T>>> window(
        WindowSpecification<T> specification1,
        WindowSpecification<T> specification2,
        WindowSpecification<T> specification3,
        WindowSpecification<T> specification4,
        WindowSpecification<T> specification5,
        WindowSpecification<T> specification6,
        WindowSpecification<T> specification7,
        WindowSpecification<T> specification8,
        WindowSpecification<T> specification9,
        WindowSpecification<T> specification10,
        WindowSpecification<T> specification11,
        WindowSpecification<T> specification12,
        WindowSpecification<T> specification13,
        WindowSpecification<T> specification14,
        WindowSpecification<T> specification15,
        WindowSpecification<T> specification16
    ) {
        List<Tuple2<T, Long>> buffer = zipWithIndex().toList();

        Map<?, Partition<T>> partitions1 = SeqUtils.partitions(specification1, buffer);
        Map<?, Partition<T>> partitions2 = SeqUtils.partitions(specification2, buffer);
        Map<?, Partition<T>> partitions3 = SeqUtils.partitions(specification3, buffer);
        Map<?, Partition<T>> partitions4 = SeqUtils.partitions(specification4, buffer);
        Map<?, Partition<T>> partitions5 = SeqUtils.partitions(specification5, buffer);
        Map<?, Partition<T>> partitions6 = SeqUtils.partitions(specification6, buffer);
        Map<?, Partition<T>> partitions7 = SeqUtils.partitions(specification7, buffer);
        Map<?, Partition<T>> partitions8 = SeqUtils.partitions(specification8, buffer);
        Map<?, Partition<T>> partitions9 = SeqUtils.partitions(specification9, buffer);
        Map<?, Partition<T>> partitions10 = SeqUtils.partitions(specification10, buffer);
        Map<?, Partition<T>> partitions11 = SeqUtils.partitions(specification11, buffer);
        Map<?, Partition<T>> partitions12 = SeqUtils.partitions(specification12, buffer);
        Map<?, Partition<T>> partitions13 = SeqUtils.partitions(specification13, buffer);
        Map<?, Partition<T>> partitions14 = SeqUtils.partitions(specification14, buffer);
        Map<?, Partition<T>> partitions15 = SeqUtils.partitions(specification15, buffer);
        Map<?, Partition<T>> partitions16 = SeqUtils.partitions(specification16, buffer);

        return seq(buffer)
              .map(t -> tuple(
                   (Window<T>) new WindowImpl<>(t, partitions1.get(specification1.partition().apply(t.v1)), specification1),
                   (Window<T>) new WindowImpl<>(t, partitions2.get(specification2.partition().apply(t.v1)), specification2),
                   (Window<T>) new WindowImpl<>(t, partitions3.get(specification3.partition().apply(t.v1)), specification3),
                   (Window<T>) new WindowImpl<>(t, partitions4.get(specification4.partition().apply(t.v1)), specification4),
                   (Window<T>) new WindowImpl<>(t, partitions5.get(specification5.partition().apply(t.v1)), specification5),
                   (Window<T>) new WindowImpl<>(t, partitions6.get(specification6.partition().apply(t.v1)), specification6),
                   (Window<T>) new WindowImpl<>(t, partitions7.get(specification7.partition().apply(t.v1)), specification7),
                   (Window<T>) new WindowImpl<>(t, partitions8.get(specification8.partition().apply(t.v1)), specification8),
                   (Window<T>) new WindowImpl<>(t, partitions9.get(specification9.partition().apply(t.v1)), specification9),
                   (Window<T>) new WindowImpl<>(t, partitions10.get(specification10.partition().apply(t.v1)), specification10),
                   (Window<T>) new WindowImpl<>(t, partitions11.get(specification11.partition().apply(t.v1)), specification11),
                   (Window<T>) new WindowImpl<>(t, partitions12.get(specification12.partition().apply(t.v1)), specification12),
                   (Window<T>) new WindowImpl<>(t, partitions13.get(specification13.partition().apply(t.v1)), specification13),
                   (Window<T>) new WindowImpl<>(t, partitions14.get(specification14.partition().apply(t.v1)), specification14),
                   (Window<T>) new WindowImpl<>(t, partitions15.get(specification15.partition().apply(t.v1)), specification15),
                   (Window<T>) new WindowImpl<>(t, partitions16.get(specification16.partition().apply(t.v1)), specification16)
              ))
              .onClose(this::close);
    }

// [jooq-tools] END [windows]

    // Shortcuts to Collectors
    // -----------------------

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#groupingBy(Function)} collector.
     */
    default <K> Map<K, List<T>> groupBy(Function<? super T, ? extends K> classifier) {
        return collect(Collectors.groupingBy(classifier));
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#groupingBy(Function, Collector)} collector.
     */
    default <K, A, D> Map<K, D> groupBy(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return collect(Collectors.groupingBy(classifier, downstream));
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#groupingBy(Function, Supplier, Collector)} collector.
     */
    default <K, D, A, M extends Map<K, D>> M groupBy(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        return collect(Collectors.groupingBy(classifier, mapFactory, downstream));
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#joining()}
     * collector.
     *
     * @deprecated - Use {@link Object#toString()} instead. This method will be
     * removed in the future as it causes confusion with
     * {@link #innerJoin(Seq, BiPredicate)}.
     */
    @Deprecated
    default String join() {
        return map(Objects::toString).collect(Collectors.joining());
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#joining(CharSequence)}
     * collector.
     *
     * @deprecated - Use {@link #toString(CharSequence)} instead. This method
     * will be removed in the future as it causes confusion with
     * {@link #innerJoin(Seq, BiPredicate)}.
     */
    @Deprecated
    default String join(CharSequence delimiter) {
        return map(Objects::toString).collect(Collectors.joining(delimiter));
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}
     * collector.
     *
     * @deprecated - Use
     * {@link #toString(CharSequence, CharSequence, CharSequence)} instead. This
     * method will be removed in the future as it causes confusion with
     * {@link #innerJoin(Seq, BiPredicate)}.
     */
    @Deprecated
    default String join(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return map(Objects::toString).collect(Collectors.joining(delimiter, prefix, suffix));
    }

    /**
     * @see Stream#of(Object)
     */
    static <T> Seq<T> of(T value) {
        return seq(Stream.of(value));
    }

    /**
     * @see Stream#of(Object[])
     */
    @SafeVarargs
    static <T> Seq<T> of(T... values) {
        return seq(Stream.of(values));
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     */
    static Seq<Byte> range(byte fromInclusive, byte toExclusive) {
        return range(fromInclusive, toExclusive, (byte) 1);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Byte> range(byte fromInclusive, byte toExclusive, int step) {
        return toExclusive <= fromInclusive ? empty() : iterate(fromInclusive, t -> Byte.valueOf((byte) (t + step))).limitWhile(t -> t < toExclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     */
    static Seq<Short> range(short fromInclusive, short toExclusive) {
        return range(fromInclusive, toExclusive, (short) 1);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Short> range(short fromInclusive, short toExclusive, int step) {
        return toExclusive <= fromInclusive ? empty() : iterate(fromInclusive, t -> Short.valueOf((short) (t + step))).limitWhile(t -> t < toExclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     */
    static Seq<Character> range(char fromInclusive, char toExclusive) {
        return range(fromInclusive, toExclusive, (short) 1);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Character> range(char fromInclusive, char toExclusive, int step) {
        return toExclusive <= fromInclusive ? empty() : iterate(fromInclusive, t -> Character.valueOf((char) (t + step))).limitWhile(t -> t < toExclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     */
    static Seq<Integer> range(int fromInclusive, int toExclusive) {
        return range(fromInclusive, toExclusive, 1);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Integer> range(int fromInclusive, int toExclusive, int step) {
        return toExclusive <= fromInclusive ? empty() : iterate(fromInclusive, t -> Integer.valueOf(t + step)).limitWhile(t -> t < toExclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     */
    static Seq<Long> range(long fromInclusive, long toExclusive) {
        return range(fromInclusive, toExclusive, 1L);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Long> range(long fromInclusive, long toExclusive, long step) {
        return toExclusive <= fromInclusive ? empty() : iterate(fromInclusive, t -> Long.valueOf(t + step)).limitWhile(t -> t < toExclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     */
    static Seq<Instant> range(Instant fromInclusive, Instant toExclusive) {
        return range(fromInclusive, toExclusive, Duration.ofSeconds(1));
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toExclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Instant> range(Instant fromInclusive, Instant toExclusive, Duration step) {
        return toExclusive.compareTo(fromInclusive) <= 0 ? empty() : iterate(fromInclusive, t -> t.plus(step)).limitWhile(t -> t.compareTo(toExclusive) < 0);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     */
    static Seq<Byte> rangeClosed(byte fromInclusive, byte toInclusive) {
        return rangeClosed(fromInclusive, toInclusive, (byte) 1);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Byte> rangeClosed(byte fromInclusive, byte toInclusive, int step) {
        return toInclusive < fromInclusive ? empty() : iterate(fromInclusive, t -> Byte.valueOf((byte) (t + step))).limitWhile(t -> t <= toInclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     */
    static Seq<Short> rangeClosed(short fromInclusive, short toInclusive) {
        return rangeClosed(fromInclusive, toInclusive, (short) 1);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Short> rangeClosed(short fromInclusive, short toInclusive, int step) {
        return toInclusive < fromInclusive ? empty() : iterate(fromInclusive, t -> Short.valueOf((short) (t + step))).limitWhile(t -> t <= toInclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     */
    static Seq<Character> rangeClosed(char fromInclusive, char toInclusive) {
        return rangeClosed(fromInclusive, toInclusive, 1);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Character> rangeClosed(char fromInclusive, char toInclusive, int step) {
        return toInclusive < fromInclusive ? empty() : iterate(fromInclusive, t -> Character.valueOf((char) (t + step))).limitWhile(t -> t <= toInclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     */
    static Seq<Integer> rangeClosed(int fromInclusive, int toInclusive) {
        return rangeClosed(fromInclusive, toInclusive, 1);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Integer> rangeClosed(int fromInclusive, int toInclusive, int step) {
        return toInclusive < fromInclusive ? empty() : iterate(fromInclusive, t -> Integer.valueOf(t + step)).limitWhile(t -> t <= toInclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     */
    static Seq<Long> rangeClosed(long fromInclusive, long toInclusive) {
        return rangeClosed(fromInclusive, toInclusive, 1L);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Long> rangeClosed(long fromInclusive, long toInclusive, long step) {
        return toInclusive < fromInclusive ? empty() : iterate(fromInclusive, t -> Long.valueOf(t + step)).limitWhile(t -> t <= toInclusive);
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     */
    static Seq<Instant> rangeClosed(Instant fromInclusive, Instant toInclusive) {
        return rangeClosed(fromInclusive, toInclusive, Duration.ofSeconds(1));
    }

    /**
     * The range between two values.
     *
     * @param fromInclusive The lower bound
     * @param toInclusive The upper bound
     * @param step The increase between two values
     */
    static Seq<Instant> rangeClosed(Instant fromInclusive, Instant toInclusive, Duration step) {
        return toInclusive.compareTo(fromInclusive) < 0 ? empty() : iterate(fromInclusive, t -> t.plus(step)).limitWhile(t -> t.compareTo(toInclusive) <= 0);
    }

    /**
     * @see Stream#empty()
     */
    static <T> Seq<T> empty() {
        return seq(Stream.empty());
    }

    /**
     * @see Stream#iterate(Object, UnaryOperator)
     */
    static <T> Seq<T> iterate(final T seed, final UnaryOperator<T> f) {
        return seq(Stream.iterate(seed, f));
    }

    /**
     * Returns a sequence of elements provided by the <code>generator</code> until it returns
     * <code>Optional.empty()</code> (in other words, it performs <code>iterateUntilAbsent</code>).
     *
     * @TODO when jOOL switches to Java 9, implement it using its new <code>Stream.iterate</code>
     */
    static <T> Seq<T> iterateWhilePresent(T seed, Function<? super T, Optional<? extends T>> generator) {
        return iterate(Objects.requireNonNull(seed), t -> generator.apply(t).orElse(null)).limitWhile(Objects::nonNull);
    }

    /**
     * @see Stream#generate(Supplier)
     */
    static Seq<Void> generate() {
        return generate(() -> null);
    }

    /**
     * @see Stream#generate(Supplier)
     */
    static <T> Seq<T> generate(T value) {
        return generate(() -> value);
    }

    /**
     * @see Stream#generate(Supplier)
     */
    static <T> Seq<T> generate(Supplier<? extends T> s) {
        return seq(Stream.generate(s));
    }

    /**
     * Wrap an array slice into a <code>Seq</code>.
     *
     * @throws IndexOutOfBoundsException if
     *         (<code>startIndex &lt; 0 || endIndex &gt; size ||
     *         startIndex &gt; endIndex</code>)
     */
    static <T> Seq<T> seq(T[] values, int startIndex, int endIndex) {
        return seq(Arrays.asList(values).subList(startIndex, endIndex));
    }

    /**
     * Wrap a <code>Stream</code> into a <code>Seq</code>.
     */
    @SuppressWarnings("unchecked")
    static <T> Seq<T> seq(Stream<? extends T> stream) {
        if (stream == null)
            return Seq.empty();

        if (stream instanceof Seq)
            return (Seq<T>) stream;

        return new SeqImpl<>(stream);
    }

    /**
     * Wrap a <code>Stream</code> into a <code>Seq</code>.
     */
    @SuppressWarnings("unchecked")
    static <T> Seq<T> seq(Seq<? extends T> stream) {
        if (stream == null)
            return Seq.empty();

        return (Seq<T>) stream;
    }

    /**
     * Wrap a <code>IntStream</code> into a <code>Seq</code>.
     */
    static Seq<Integer> seq(IntStream stream) {
        if (stream == null)
            return Seq.empty();

        return new SeqImpl<>(stream.boxed());
    }

    /**
     * Wrap a <code>IntStream</code> into a <code>Seq</code>.
     */
    static Seq<Long> seq(LongStream stream) {
        if (stream == null)
            return Seq.empty();

        return new SeqImpl<>(stream.boxed());
    }

    /**
     * Wrap a <code>IntStream</code> into a <code>Seq</code>.
     */
    static Seq<Double> seq(DoubleStream stream) {
        if (stream == null)
            return Seq.empty();

        return new SeqImpl<>(stream.boxed());
    }

    /**
     * Wrap an <code>Iterable</code> into a <code>Seq</code>.
     */
    static <T> Seq<T> seq(Iterable<? extends T> iterable) {
        if (iterable == null)
            return Seq.empty();

        return seq(iterable.iterator());
    }

    /**
     * Wrap an <code>Iterator</code> into a <code>Seq</code>.
     */
    static <T> Seq<T> seq(Iterator<? extends T> iterator) {
        if (iterator == null)
            return Seq.empty();

        return seq(spliteratorUnknownSize(iterator, ORDERED));
    }

    /**
     * Wrap an <code>Enumeration</code> into a <code>Seq</code>.
     */
    static <T> Seq<T> seq(Enumeration<T> enumeration) {
        if (enumeration == null)
            return Seq.empty();

        return Seq.seq(new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return enumeration.hasMoreElements();
            }

            @Override
            public T next() {
                return enumeration.nextElement();
            }
        });
    }

    /**
     * Wrap a <code>Spliterator</code> into a <code>Seq</code>.
     */
    static <T> Seq<T> seq(Spliterator<? extends T> spliterator) {
        if (spliterator == null)
            return Seq.empty();

        return seq(StreamSupport.stream(spliterator, false));
    }

    /**
     * Wrap a <code>Map</code> into a <code>Seq</code>.
     */
    static <K, V> Seq<Tuple2<K, V>> seq(Map<? extends K, ? extends V> map) {
        if (map == null)
            return Seq.empty();

        return seq(map.entrySet()).map(e -> tuple(e.getKey(), e.getValue()));
    }

    /**
     * Wrap an <code>Optional</code> into a <code>Seq</code>.
     */
    static <T> Seq<T> seq(Optional<? extends T> optional) {

        // [#245] For the special kind of ugly client code...
        if (optional == null)
            return Seq.empty();

        return optional.map(Seq::of).orElseGet(Seq::empty);
    }

    /**
     * Wrap multiple <code>Optional</code>'s into a <code>Seq</code>.
     */
    @SafeVarargs
    static <T> Seq<T> seq(Optional<? extends T>... optionals) {
        if (optionals == null)
            return Seq.empty();

        return of(optionals).filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Get a stream from a single element from a <code>Supplier</code>.
     */
    static <T> Seq<T> seq(Supplier<? extends T> s) {
        return Seq.<T>generate(s).limit(1);
    }

    /**
     * Wrap an <code>InputStream</code> into a <code>Seq</code>.
     * <p>
     * Client code must close the <code>InputStream</code>. All
     * {@link IOException}'s thrown be the <code>InputStream</code> are wrapped
     * by {@link UncheckedIOException}'s.
     */
    static Seq<Byte> seq(InputStream is) {
        if (is == null)
            return Seq.empty();

        FunctionalSpliterator<Byte> spliterator = consumer -> {
            try {
                int value = is.read();

                if (value != -1)
                    consumer.accept((byte) value);

                return value != -1;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        return seq(spliterator).onClose(Unchecked.runnable(is::close));
    }

    /**
     * Wrap a <code>Reader</code> into a <code>Seq</code>.
     * <p>
     * Client code must close the <code>Reader</code>. All
     * {@link IOException}'s thrown be the <code>Reader</code> are wrapped
     * by {@link UncheckedIOException}'s.
     */
    static Seq<Character> seq(Reader reader) {
        if (reader == null)
            return Seq.empty();

        FunctionalSpliterator<Character> spliterator = consumer -> {
            try {
                int value = reader.read();

                if (value != -1)
                    consumer.accept((char) value);

                return value != -1;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        return seq(spliterator).onClose(Unchecked.runnable(reader::close));
    }

    /**
     * Repeat a stream infinitely.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 1, 2, 3, ...)
     * Seq.of(1, 2, 3).cycle();
     * </pre></code>
     */
    static <T> Seq<T> cycle(Stream<? extends T> stream) {
        return cycle(seq(stream));
    }

    /**
     * Repeat a stream infinitely.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 1, 2, 3, ...)
     * Seq.of(1, 2, 3).cycle();
     * </pre></code>
     */
    static <T> Seq<T> cycle(Iterable<? extends T> iterable) {
        return cycle(seq(iterable));
    }

    /**
     * Repeat a stream infinitely.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 1, 2, 3, ...)
     * Seq.of(1, 2, 3).cycle();
     * </pre></code>
     */
    static <T> Seq<T> cycle(Seq<? extends T> stream) {
        return cycle(stream, -1);
    }

    /**
     * Repeat a stream a certain amount of times.
     * <p>
     * <code><pre>
     * // ()
     * Seq.of(1, 2, 3).cycle(0);
     *
     * // (1, 2, 3)
     * Seq.of(1, 2, 3).cycle(1);
     *
     * // (1, 2, 3, 1, 2, 3, 1, 2, 3)
     * Seq.of(1, 2, 3).cycle(3);
     * </pre></code>
     *
     * @see #cycle(Stream)
     */
    static <T> Seq<T> cycle(Stream<? extends T> stream, long times) {
        return cycle(seq(stream), times);
    }

    /**
     * Repeat a stream a certain amount of times.
     * <p>
     * <code><pre>
     * // ()
     * Seq.of(1, 2, 3).cycle(0);
     *
     * // (1, 2, 3)
     * Seq.of(1, 2, 3).cycle(1);
     *
     * // (1, 2, 3, 1, 2, 3, 1, 2, 3)
     * Seq.of(1, 2, 3).cycle(3);
     * </pre></code>
     *
     * @see #cycle(Stream)
     */
    static <T> Seq<T> cycle(Iterable<? extends T> iterable, long times) {
        return cycle(seq(iterable), times);
    }

    /**
     * Repeat a stream a certain amount of times.
     * <p>
     * <code><pre>
     * // ()
     * Seq.of(1, 2, 3).cycle(0);
     *
     * // (1, 2, 3)
     * Seq.of(1, 2, 3).cycle(1);
     *
     * // (1, 2, 3, 1, 2, 3, 1, 2, 3)
     * Seq.of(1, 2, 3).cycle(3);
     * </pre></code>
     *
     * @see #cycle(Stream)
     */
    @SuppressWarnings("unchecked")
    static <T> Seq<T> cycle(Seq<? extends T> stream, long times) {
        if (times == 0)
            return empty();
        if (times == 1)
            return (Seq<T>) stream;

        List<T> list = new ArrayList<>();
        Spliterator<T>[] sp = new Spliterator[1];
        long[] remaining = new long[] { times };

        return SeqUtils.transform(stream, (delegate, action) -> {
            if (sp[0] == null) {
                if (delegate.tryAdvance(t -> {
                    list.add(t);
                    action.accept(t);
                }))
                    return true;
                else
                    sp[0] = list.spliterator();
            }

            if (!sp[0].tryAdvance(action)) {
                if (times != -1 && (remaining[0] = remaining[0] - 1) == 1)
                    return false;

                sp[0] = list.spliterator();

                if (!sp[0].tryAdvance(action))
                    return false;
            }

            return true;
        });
    }

    /**
     * Unzip a Map into its keys and values.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Map.of(1, "a", 2, "b", 3, "c"));
     * </pre></code>
     */
    static <K, V> Tuple2<Seq<K>, Seq<V>> unzip(Map<? extends K, ? extends V> map) {
        return unzip(seq(map));
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(Stream<Tuple2<T1, T2>> stream) {
        return unzip(seq(stream));
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2, U1, U2> Tuple2<Seq<U1>, Seq<U2>> unzip(Stream<Tuple2<T1, T2>> stream, Function<T1, U1> leftUnzipper, Function<T2, U2> rightUnzipper) {
        return unzip(seq(stream), leftUnzipper, rightUnzipper);
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2, U1, U2> Tuple2<Seq<U1>, Seq<U2>> unzip(Stream<Tuple2<T1, T2>> stream, Function<Tuple2<T1, T2>, Tuple2<U1, U2>> unzipper) {
        return unzip(seq(stream), unzipper);
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2, U1, U2> Tuple2<Seq<U1>, Seq<U2>> unzip(Stream<Tuple2<T1, T2>> stream, BiFunction<T1, T2, Tuple2<U1, U2>> unzipper) {
        return unzip(seq(stream), unzipper);
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(Iterable<Tuple2<T1, T2>> iterable) {
        return unzip(seq(iterable));
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2, U1, U2> Tuple2<Seq<U1>, Seq<U2>> unzip(Iterable<Tuple2<T1, T2>> iterable, Function<T1, U1> leftUnzipper, Function<T2, U2> rightUnzipper) {
        return unzip(seq(iterable), leftUnzipper, rightUnzipper);
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2, U1, U2> Tuple2<Seq<U1>, Seq<U2>> unzip(Iterable<Tuple2<T1, T2>> iterable, Function<Tuple2<T1, T2>, Tuple2<U1, U2>> unzipper) {
        return unzip(seq(iterable), unzipper);
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2, U1, U2> Tuple2<Seq<U1>, Seq<U2>> unzip(Iterable<Tuple2<T1, T2>> iterable, BiFunction<T1, T2, Tuple2<U1, U2>> unzipper) {
        return unzip(seq(iterable), unzipper);
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(Seq<Tuple2<T1, T2>> stream) {
        return unzip(stream, t -> t);
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2, U1, U2> Tuple2<Seq<U1>, Seq<U2>> unzip(Seq<Tuple2<T1, T2>> stream, Function<T1, U1> leftUnzipper, Function<T2, U2> rightUnzipper) {
        return unzip(stream, t -> tuple(leftUnzipper.apply(t.v1), rightUnzipper.apply(t.v2)));
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2, U1, U2> Tuple2<Seq<U1>, Seq<U2>> unzip(Seq<Tuple2<T1, T2>> stream, Function<Tuple2<T1, T2>, Tuple2<U1, U2>> unzipper) {
        return unzip(stream, (t1, t2) -> unzipper.apply(tuple(t1, t2)));
    }

    /**
     * Unzip one Stream into two.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (a, b, c))
     * Seq.unzip(Seq.of(tuple(1, "a"), tuple(2, "b"), tuple(3, "c")));
     * </pre></code>
     */
    static <T1, T2, U1, U2> Tuple2<Seq<U1>, Seq<U2>> unzip(Seq<Tuple2<T1, T2>> stream, BiFunction<T1, T2, Tuple2<U1, U2>> unzipper) {
        return stream
              .map(t -> unzipper.apply(t.v1, t.v2))
              .duplicate()
              .map1(s -> s.map(u -> u.v1))
              .map2(s -> s.map(u -> u.v2));
    }

 // [jooq-tools] START [zip-static]

    /**
     * Zip 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2) {
        return zip(seq(s1), seq(s2));
    }

    /**
     * Zip 3 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3) {
        return zip(seq(s1), seq(s2), seq(s3));
    }

    /**
     * Zip 4 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4));
    }

    /**
     * Zip 5 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5));
    }

    /**
     * Zip 6 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6));
    }

    /**
     * Zip 7 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7));
    }

    /**
     * Zip 8 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8));
    }

    /**
     * Zip 9 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9));
    }

    /**
     * Zip 10 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10));
    }

    /**
     * Zip 11 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11));
    }

    /**
     * Zip 12 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12));
    }

    /**
     * Zip 13 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13));
    }

    /**
     * Zip 14 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14));
    }

    /**
     * Zip 15 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15));
    }

    /**
     * Zip 16 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15, Stream<? extends T16> s16) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15), seq(s16));
    }

    /**
     * Zip 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2) {
        return zip(seq(i1), seq(i2));
    }

    /**
     * Zip 3 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3) {
        return zip(seq(i1), seq(i2), seq(i3));
    }

    /**
     * Zip 4 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4));
    }

    /**
     * Zip 5 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5));
    }

    /**
     * Zip 6 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6));
    }

    /**
     * Zip 7 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7));
    }

    /**
     * Zip 8 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8));
    }

    /**
     * Zip 9 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9));
    }

    /**
     * Zip 10 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10));
    }

    /**
     * Zip 11 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11));
    }

    /**
     * Zip 12 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12));
    }

    /**
     * Zip 13 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13));
    }

    /**
     * Zip 14 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Iterable<? extends T14> i14) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), seq(i14));
    }

    /**
     * Zip 15 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Iterable<? extends T14> i14, Iterable<? extends T15> i15) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), seq(i14), seq(i15));
    }

    /**
     * Zip 16 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Iterable<? extends T14> i14, Iterable<? extends T15> i15, Iterable<? extends T16> i16) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), seq(i14), seq(i15), seq(i16));
    }

    /**
     * Zip 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2) {
        return zip(s1, s2, (t1, t2) -> tuple(t1, t2))
              .onClose(SeqUtils.closeAll(s1, s2));
    }

    /**
     * Zip 3 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3) {
        return zip(s1, s2, s3, (t1, t2, t3) -> tuple(t1, t2, t3))
              .onClose(SeqUtils.closeAll(s1, s2, s3));
    }

    /**
     * Zip 4 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4) {
        return zip(s1, s2, s3, s4, (t1, t2, t3, t4) -> tuple(t1, t2, t3, t4))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4));
    }

    /**
     * Zip 5 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5) {
        return zip(s1, s2, s3, s4, s5, (t1, t2, t3, t4, t5) -> tuple(t1, t2, t3, t4, t5))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5));
    }

    /**
     * Zip 6 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6) {
        return zip(s1, s2, s3, s4, s5, s6, (t1, t2, t3, t4, t5, t6) -> tuple(t1, t2, t3, t4, t5, t6))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6));
    }

    /**
     * Zip 7 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7) {
        return zip(s1, s2, s3, s4, s5, s6, s7, (t1, t2, t3, t4, t5, t6, t7) -> tuple(t1, t2, t3, t4, t5, t6, t7))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7));
    }

    /**
     * Zip 8 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8) {
        return zip(s1, s2, s3, s4, s5, s6, s7, s8, (t1, t2, t3, t4, t5, t6, t7, t8) -> tuple(t1, t2, t3, t4, t5, t6, t7, t8))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8));
    }

    /**
     * Zip 9 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9) {
        return zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, (t1, t2, t3, t4, t5, t6, t7, t8, t9) -> tuple(t1, t2, t3, t4, t5, t6, t7, t8, t9))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9));
    }

    /**
     * Zip 10 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10) {
        return zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10) -> tuple(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10));
    }

    /**
     * Zip 11 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11) {
        return zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) -> tuple(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11));
    }

    /**
     * Zip 12 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12) {
        return zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12) -> tuple(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12));
    }

    /**
     * Zip 13 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13) {
        return zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13) -> tuple(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13));
    }

    /**
     * Zip 14 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14) {
        return zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14) -> tuple(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14));
    }

    /**
     * Zip 15 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15) {
        return zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15) -> tuple(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15));
    }

    /**
     * Zip 16 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15, Seq<? extends T16> s16) {
        return zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16) -> tuple(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16))
              .onClose(SeqUtils.closeAll(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16));
    }

    /**
     * Zip 2 streams into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), zipper);
    }

    /**
     * Zip 3 streams into one using a {@link Function3} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), zipper);
    }

    /**
     * Zip 4 streams into one using a {@link Function4} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), zipper);
    }

    /**
     * Zip 5 streams into one using a {@link Function5} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), zipper);
    }

    /**
     * Zip 6 streams into one using a {@link Function6} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), zipper);
    }

    /**
     * Zip 7 streams into one using a {@link Function7} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), zipper);
    }

    /**
     * Zip 8 streams into one using a {@link Function8} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), zipper);
    }

    /**
     * Zip 9 streams into one using a {@link Function9} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), zipper);
    }

    /**
     * Zip 10 streams into one using a {@link Function10} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Function10<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), zipper);
    }

    /**
     * Zip 11 streams into one using a {@link Function11} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Function11<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), zipper);
    }

    /**
     * Zip 12 streams into one using a {@link Function12} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Function12<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), zipper);
    }

    /**
     * Zip 13 streams into one using a {@link Function13} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Function13<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), zipper);
    }

    /**
     * Zip 14 streams into one using a {@link Function14} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Function14<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), zipper);
    }

    /**
     * Zip 15 streams into one using a {@link Function15} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15, Function15<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15), zipper);
    }

    /**
     * Zip 16 streams into one using a {@link Function16} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> Seq<R> zip(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15, Stream<? extends T16> s16, Function16<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? super T16, ? extends R> zipper) {
        return zip(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15), seq(s16), zipper);
    }

    /**
     * Zip 2 streams into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), zipper);
    }

    /**
     * Zip 3 streams into one using a {@link Function3} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), zipper);
    }

    /**
     * Zip 4 streams into one using a {@link Function4} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), zipper);
    }

    /**
     * Zip 5 streams into one using a {@link Function5} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), zipper);
    }

    /**
     * Zip 6 streams into one using a {@link Function6} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), zipper);
    }

    /**
     * Zip 7 streams into one using a {@link Function7} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), zipper);
    }

    /**
     * Zip 8 streams into one using a {@link Function8} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), zipper);
    }

    /**
     * Zip 9 streams into one using a {@link Function9} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), zipper);
    }

    /**
     * Zip 10 streams into one using a {@link Function10} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Function10<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), zipper);
    }

    /**
     * Zip 11 streams into one using a {@link Function11} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Function11<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), zipper);
    }

    /**
     * Zip 12 streams into one using a {@link Function12} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Function12<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), zipper);
    }

    /**
     * Zip 13 streams into one using a {@link Function13} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Function13<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), zipper);
    }

    /**
     * Zip 14 streams into one using a {@link Function14} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Iterable<? extends T14> i14, Function14<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), seq(i14), zipper);
    }

    /**
     * Zip 15 streams into one using a {@link Function15} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Iterable<? extends T14> i14, Iterable<? extends T15> i15, Function15<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), seq(i14), seq(i15), zipper);
    }

    /**
     * Zip 16 streams into one using a {@link Function16} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> Seq<R> zip(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Iterable<? extends T14> i14, Iterable<? extends T15> i15, Iterable<? extends T16> i16, Function16<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? super T16, ? extends R> zipper) {
        return zip(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), seq(i14), seq(i15), seq(i16), zipper);
    }

    /**
     * Zip 2 streams into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 3 streams into one using a {@link Function3} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 4 streams into one using a {@link Function4} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 5 streams into one using a {@link Function5} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 6 streams into one using a {@link Function6} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 7 streams into one using a {@link Function7} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 8 streams into one using a {@link Function8} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext() && it8.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next(), it8.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 9 streams into one using a {@link Function9} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext() && it8.hasNext() && it9.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next(), it8.next(), it9.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 10 streams into one using a {@link Function10} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Function10<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext() && it8.hasNext() && it9.hasNext() && it10.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next(), it8.next(), it9.next(), it10.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 11 streams into one using a {@link Function11} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Function11<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext() && it8.hasNext() && it9.hasNext() && it10.hasNext() && it11.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next(), it8.next(), it9.next(), it10.next(), it11.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 12 streams into one using a {@link Function12} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Function12<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext() && it8.hasNext() && it9.hasNext() && it10.hasNext() && it11.hasNext() && it12.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next(), it8.next(), it9.next(), it10.next(), it11.next(), it12.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 13 streams into one using a {@link Function13} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Function13<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();
        final Iterator<? extends T13> it13 = s13.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext() && it8.hasNext() && it9.hasNext() && it10.hasNext() && it11.hasNext() && it12.hasNext() && it13.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next(), it8.next(), it9.next(), it10.next(), it11.next(), it12.next(), it13.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 14 streams into one using a {@link Function14} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Function14<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();
        final Iterator<? extends T13> it13 = s13.iterator();
        final Iterator<? extends T14> it14 = s14.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext() && it8.hasNext() && it9.hasNext() && it10.hasNext() && it11.hasNext() && it12.hasNext() && it13.hasNext() && it14.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next(), it8.next(), it9.next(), it10.next(), it11.next(), it12.next(), it13.next(), it14.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 15 streams into one using a {@link Function15} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15, Function15<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();
        final Iterator<? extends T13> it13 = s13.iterator();
        final Iterator<? extends T14> it14 = s14.iterator();
        final Iterator<? extends T15> it15 = s15.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext() && it8.hasNext() && it9.hasNext() && it10.hasNext() && it11.hasNext() && it12.hasNext() && it13.hasNext() && it14.hasNext() && it15.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next(), it8.next(), it9.next(), it10.next(), it11.next(), it12.next(), it13.next(), it14.next(), it15.next());
            }
        }

        return seq(new Zip());
    }

    /**
     * Zip 16 streams into one using a {@link Function16} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("1:a", "2:b", "3:c")
     * Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"), (i, s) -> i + ":" + s)
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> Seq<R> zip(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15, Seq<? extends T16> s16, Function16<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? super T15, ? super T16, ? extends R> zipper) {
        final Iterator<? extends T1> it1 = s1.iterator();
        final Iterator<? extends T2> it2 = s2.iterator();
        final Iterator<? extends T3> it3 = s3.iterator();
        final Iterator<? extends T4> it4 = s4.iterator();
        final Iterator<? extends T5> it5 = s5.iterator();
        final Iterator<? extends T6> it6 = s6.iterator();
        final Iterator<? extends T7> it7 = s7.iterator();
        final Iterator<? extends T8> it8 = s8.iterator();
        final Iterator<? extends T9> it9 = s9.iterator();
        final Iterator<? extends T10> it10 = s10.iterator();
        final Iterator<? extends T11> it11 = s11.iterator();
        final Iterator<? extends T12> it12 = s12.iterator();
        final Iterator<? extends T13> it13 = s13.iterator();
        final Iterator<? extends T14> it14 = s14.iterator();
        final Iterator<? extends T15> it15 = s15.iterator();
        final Iterator<? extends T16> it16 = s16.iterator();

        class Zip implements Iterator<R> {
            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext() && it3.hasNext() && it4.hasNext() && it5.hasNext() && it6.hasNext() && it7.hasNext() && it8.hasNext() && it9.hasNext() && it10.hasNext() && it11.hasNext() && it12.hasNext() && it13.hasNext() && it14.hasNext() && it15.hasNext() && it16.hasNext();
            }

            @Override
            public R next() {
                return zipper.apply(it1.next(), it2.next(), it3.next(), it4.next(), it5.next(), it6.next(), it7.next(), it8.next(), it9.next(), it10.next(), it11.next(), it12.next(), it13.next(), it14.next(), it15.next(), it16.next());
            }
        }

        return seq(new Zip());
    }

// [jooq-tools] END [zip-static]

    /**
     * Zip a Stream with a corresponding Stream of indexes.
     * <p>
     * <code><pre>
     * // (tuple("a", 0), tuple("b", 1), tuple("c", 2))
     * Seq.of("a", "b", "c").zipWithIndex()
     * </pre></code>
     */
    static <T> Seq<Tuple2<T, Long>> zipWithIndex(Stream<? extends T> stream) {
        return zipWithIndex(seq(stream));
    }

    /**
     * Zip a Stream with a corresponding Stream of indexes.
     * <p>
     * <code><pre>
     * // (tuple("a", 0), tuple("b", 1), tuple("c", 2))
     * Seq.of("a", "b", "c").zipWithIndex()
     * </pre></code>
     */
    static <T> Seq<Tuple2<T, Long>> zipWithIndex(Iterable<? extends T> iterable) {
        return zipWithIndex(seq(iterable));
    }

    /**
     * Zip a Stream with a corresponding Stream of indexes.
     * <p>
     * <code><pre>
     * // (tuple("a", 0), tuple("b", 1), tuple("c", 2))
     * Seq.of("a", "b", "c").zipWithIndex()
     * </pre></code>
     */
    static <T> Seq<Tuple2<T, Long>> zipWithIndex(Seq<? extends T> stream) {
        long[] index = { -1L };

        return SeqUtils.transform(stream, (delegate, action) ->
            delegate.tryAdvance(t ->
                action.accept(tuple(t, ++index[0]))
            )
        );
    }

    /**
     * Zip a stream with indexes into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("0:a", "1:b", "2:c")
     * Seq.of("a", "b", "c").zipWithIndex((s, i) -> i + ":" + s))
     * </pre></code>
     */
    static <T, R> Seq<R> zipWithIndex(Stream<? extends T> stream, BiFunction<? super T, ? super Long, ? extends R> zipper) {
        return zipWithIndex(seq(stream), zipper);
    }

    /**
     * Zip a stream with indexes into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("0:a", "1:b", "2:c")
     * Seq.of("a", "b", "c").zipWithIndex((s, i) -> i + ":" + s))
     * </pre></code>
     */
    static <T, R> Seq<R> zipWithIndex(Iterable<? extends T> iterable, BiFunction<? super T, ? super Long, ? extends R> zipper) {
        return zipWithIndex(seq(iterable), zipper);
    }

    /**
     * Zip a stream with indexes into one using a {@link BiFunction} to produce resulting values.
     * <p>
     * <code><pre>
     * // ("0:a", "1:b", "2:c")
     * Seq.of("a", "b", "c").zipWithIndex((s, i) -> i + ":" + s))
     * </pre></code>
     */
    static <T, R> Seq<R> zipWithIndex(Seq<? extends T> stream, BiFunction<? super T, ? super Long, ? extends R> zipper) {
        long[] index = { -1L };

        return SeqUtils.transform(stream, (delegate, action) ->
            delegate.tryAdvance(t ->
                action.accept(zipper.apply(t, ++index[0]))
            )
        );
    }

    /**
     * Fold a stream to the left.
     * <p>
     * <code><pre>
     * // "abc"
     * Seq.of("a", "b", "c").foldLeft("", (u, t) -> u + t)
     * </pre></code>
     */
    static <T, U> U foldLeft(Stream<? extends T> stream, U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        return foldLeft(seq(stream), seed, function);
    }

    /**
     * Fold a stream to the left.
     * <p>
     * <code><pre>
     * // "abc"
     * Seq.of("a", "b", "c").foldLeft("", (u, t) -> u + t)
     * </pre></code>
     */
    static <T, U> U foldLeft(Iterable<? extends T> iterable, U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        return foldLeft(seq(iterable), seed, function);
    }

    /**
     * Fold a stream to the left.
     * <p>
     * <code><pre>
     * // "abc"
     * Seq.of("a", "b", "c").foldLeft("", (u, t) -> u + t)
     * </pre></code>
     */
    static <T, U> U foldLeft(Seq<? extends T> stream, U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        final Iterator<? extends T> it = stream.iterator();
        U result = seed;

        while (it.hasNext())
            result = function.apply(result, it.next());

        return result;
    }

    /**
     * Fold a stream to the right.
     * <p>
     * <code><pre>
     * // "cba"
     * Seq.of("a", "b", "c").foldRight("", (t, u) -> u + t)
     * </pre></code>
     */
    static <T, U> U foldRight(Stream<? extends T> stream, U seed, BiFunction<? super T, ? super U, ? extends U> function) {
        return foldRight(seq(stream), seed, function);
    }

    /**
     * Fold a stream to the right.
     * <p>
     * <code><pre>
     * // "cba"
     * Seq.of("a", "b", "c").foldRight("", (t, u) -> u + t)
     * </pre></code>
     */
    static <T, U> U foldRight(Iterable<? extends T> iterable, U seed, BiFunction<? super T, ? super U, ? extends U> function) {
        return foldRight(seq(iterable), seed, function);
    }

    /**
     * Fold a stream to the right.
     * <p>
     * <code><pre>
     * // "cba"
     * Seq.of("a", "b", "c").foldRight("", (t, u) -> u + t)
     * </pre></code>
     */
    static <T, U> U foldRight(Seq<? extends T> stream, U seed, BiFunction<? super T, ? super U, ? extends U> function) {
        return stream.reverse().foldLeft(seed, (u, t) -> function.apply(t, u));
    }

    /**
     * Scan a stream to the left.
     * <p>
     * <code><pre>
     * // ("", "a", "ab", "abc")
     * Seq.of("a", "b", "c").scanLeft("", (u, t) -> u + t)
     * </pre></code>
     */
    static <T, U> Seq<U> scanLeft(Stream<? extends T> stream, U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        return scanLeft(seq(stream), seed, function);
    }

    /**
     * Scan a stream to the left.
     * <p>
     * <code><pre>
     * // ("", "a", "ab", "abc")
     * Seq.of("a", "b", "c").scanLeft("", (u, t) -> u + t)
     * </pre></code>
     */
    static <T, U> Seq<U> scanLeft(Iterable<? extends T> iterable, U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        return scanLeft(seq(iterable), seed, function);
    }

    /**
     * Scan a stream to the left.
     * <p>
     * <code><pre>
     * // ("", "a", "ab", "abc")
     * Seq.of("a", "b", "c").scanLeft("", (u, t) -> u + t)
     * </pre></code>
     */
    static <T, U> Seq<U> scanLeft(Seq<? extends T> stream, U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        @SuppressWarnings("unchecked")
        U[] value = (U[]) new Object[] { seed };

        return Seq.of(seed).concat(SeqUtils.transform(stream, (delegate, action) ->
            delegate.tryAdvance(t ->
                action.accept(value[0] = function.apply(value[0], t))
            )
        ));
    }

    /**
     * Scan a stream to the right.
     * <p>
     * <code><pre>
     * // ("", "c", "cb", "cba")
     * Seq.of("a", "b", "c").scanRight("", (t, u) -> u + t)
     * </pre></code>
     */
    static <T, U> Seq<U> scanRight(Stream<? extends T> stream, U seed, BiFunction<? super T, ? super U, ? extends U> function) {
        return scanRight(seq(stream), seed, function);
    }

    /**
     * Scan a stream to the right.
     * <p>
     * <code><pre>
     * // ("", "c", "cb", "cba")
     * Seq.of("a", "b", "c").scanRight("", (t, u) -> u + t)
     * </pre></code>
     */
    static <T, U> Seq<U> scanRight(Iterable<? extends T> iterable, U seed, BiFunction<? super T, ? super U, ? extends U> function) {
        return scanRight(seq(iterable), seed, function);
    }

    /**
     * Scan a stream to the right.
     * <p>
     * <code><pre>
     * // ("", "c", "cb", "cba")
     * Seq.of("a", "b", "c").scanRight("", (t, u) -> u + t)
     * </pre></code>
     */
    static <T, U> Seq<U> scanRight(Seq<? extends T> stream, U seed, BiFunction<? super T, ? super U, ? extends U> function) {
        return stream.reverse().scanLeft(seed, (u, t) -> function.apply(t, u));
    }

    /**
     * Unfold a function into a stream.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5)
     * Seq.unfold(1, i -> i &lt;= 6 ? Optional.of(tuple(i, i + 1)) : Optional.empty())
     * </pre></code>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static <T, U> Seq<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        Tuple2<? extends T, ? extends U>[] unfolded = new Tuple2[] { tuple((T) null, seed) };

        return seq((FunctionalSpliterator<T>) action -> {
            Optional<? extends Tuple2<? extends T, ? extends U>> result = unfolder.apply(unfolded[0].v2);

            if (result.isPresent())
                action.accept((unfolded[0] = result.get()).v1);

            return result.isPresent();
        });
    }

    /**
     * Reverse a stream.
     * <p>
     * <code><pre>
     * // (3, 2, 1)
     * Seq.of(1, 2, 3).reverse()
     * </pre></code>
     */
    static <T> Seq<T> reverse(Stream<? extends T> stream) {
        return reverse(seq(stream));
    }

    /**
     * Reverse a stream.
     * <p>
     * <code><pre>
     * // (3, 2, 1)
     * Seq.of(1, 2, 3).reverse()
     * </pre></code>
     */
    static <T> Seq<T> reverse(Iterable<? extends T> iterable) {
        return reverse(seq(iterable));
    }

    /**
     * Reverse a stream.
     * <p>
     * <code><pre>
     * // (3, 2, 1)
     * Seq.of(1, 2, 3).reverse()
     * </pre></code>
     */
    static <T> Seq<T> reverse(Seq<? extends T> stream) {
        List<T> list = toList(stream);
        Collections.reverse(list);
        return seq(list).onClose(stream::close);
    }

    /**
     * Shuffle a stream
     * <p>
     * <code><pre>
     * // e.g. (2, 3, 1)
     * Seq.of(1, 2, 3).shuffle()
     * </pre></code>
     */
    static <T> Seq<T> shuffle(Stream<? extends T> stream) {
        return shuffle(seq(stream));
    }

    /**
     * Shuffle a stream
     * <p>
     * <code><pre>
     * // e.g. (2, 3, 1)
     * Seq.of(1, 2, 3).shuffle()
     * </pre></code>
     */
    static <T> Seq<T> shuffle(Iterable<? extends T> iterable) {
        return shuffle(seq(iterable));
    }

    /**
     * Shuffle a stream
     * <p>
     * <code><pre>
     * // e.g. (2, 3, 1)
     * Seq.of(1, 2, 3).shuffle()
     * </pre></code>
     */
    static <T> Seq<T> shuffle(Seq<? extends T> stream) {
        return shuffle(stream, null);
    }

    /**
     * Shuffle a stream using specified source of randomness
     * <p>
     * <code><pre>
     * // e.g. (2, 3, 1)
     * Seq.of(1, 2, 3).shuffle(new Random())
     * </pre></code>
     */
    static <T> Seq<T> shuffle(Stream<? extends T> stream, Random random) {
        return shuffle(seq(stream), random);
    }

    /**
     * Shuffle a stream using specified source of randomness
     * <p>
     * <code><pre>
     * // e.g. (2, 3, 1)
     * Seq.of(1, 2, 3).shuffle(new Random())
     * </pre></code>
     */
    static <T> Seq<T> shuffle(Iterable<? extends T> iterable, Random random) {
        return shuffle(seq(iterable), random);
    }

    /**
     * Shuffle a stream using specified source of randomness
     * <p>
     * <code><pre>
     * // e.g. (2, 3, 1)
     * Seq.of(1, 2, 3).shuffle(new Random())
     * </pre></code>
     */
    static <T> Seq<T> shuffle(Seq<? extends T> stream, Random random) {
        Spliterator<? extends T>[] shuffled = new Spliterator[1];

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        return SeqUtils.<T, T>transform(stream, (delegate, action) -> {
            if (shuffled[0] == null) {
                List<T> list = Seq.<T>seq(delegate).toList();

                if (random == null)
                    Collections.shuffle(list);
                else
                    Collections.shuffle(list, random);

                shuffled[0] = list.spliterator();
            }

            return shuffled[0].tryAdvance(action);
        }).onClose(stream::close);
    }

    // [jooq-tools] START [crossapply-static]

    /**
     * Cross apply 2 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)));
    }

    /**
     * Cross apply 3 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)));
    }

    /**
     * Cross apply 4 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)));
    }

    /**
     * Cross apply 5 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)));
    }

    /**
     * Cross apply 6 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)));
    }

    /**
     * Cross apply 7 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)));
    }

    /**
     * Cross apply 8 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)));
    }

    /**
     * Cross apply 9 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)));
    }

    /**
     * Cross apply 10 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)));
    }

    /**
     * Cross apply 11 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)));
    }

    /**
     * Cross apply 12 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)));
    }

    /**
     * Cross apply 13 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12, Function<? super T12, ? extends Stream<? extends T13>> function13) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)));
    }

    /**
     * Cross apply 14 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12, Function<? super T12, ? extends Stream<? extends T13>> function13, Function<? super T13, ? extends Stream<? extends T14>> function14) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)));
    }

    /**
     * Cross apply 15 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12, Function<? super T12, ? extends Stream<? extends T13>> function13, Function<? super T13, ? extends Stream<? extends T14>> function14, Function<? super T14, ? extends Stream<? extends T15>> function15) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)), t -> seq(function15.apply(t)));
    }

    /**
     * Cross apply 16 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> crossApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12, Function<? super T12, ? extends Stream<? extends T13>> function13, Function<? super T13, ? extends Stream<? extends T14>> function14, Function<? super T14, ? extends Stream<? extends T15>> function15, Function<? super T15, ? extends Stream<? extends T16>> function16) {
        return crossApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)), t -> seq(function15.apply(t)), t -> seq(function16.apply(t)));
    }

    /**
     * Cross apply 2 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)));
    }

    /**
     * Cross apply 3 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)));
    }

    /**
     * Cross apply 4 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)));
    }

    /**
     * Cross apply 5 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)));
    }

    /**
     * Cross apply 6 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)));
    }

    /**
     * Cross apply 7 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)));
    }

    /**
     * Cross apply 8 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)));
    }

    /**
     * Cross apply 9 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)));
    }

    /**
     * Cross apply 10 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)));
    }

    /**
     * Cross apply 11 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)));
    }

    /**
     * Cross apply 12 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)));
    }

    /**
     * Cross apply 13 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12, Function<? super T12, ? extends Iterable<? extends T13>> function13) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)));
    }

    /**
     * Cross apply 14 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12, Function<? super T12, ? extends Iterable<? extends T13>> function13, Function<? super T13, ? extends Iterable<? extends T14>> function14) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)));
    }

    /**
     * Cross apply 15 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12, Function<? super T12, ? extends Iterable<? extends T13>> function13, Function<? super T13, ? extends Iterable<? extends T14>> function14, Function<? super T14, ? extends Iterable<? extends T15>> function15) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)), t -> seq(function15.apply(t)));
    }

    /**
     * Cross apply 16 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> crossApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12, Function<? super T12, ? extends Iterable<? extends T13>> function13, Function<? super T13, ? extends Iterable<? extends T14>> function14, Function<? super T14, ? extends Iterable<? extends T15>> function15, Function<? super T15, ? extends Iterable<? extends T16>> function16) {
        return crossApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)), t -> seq(function15.apply(t)), t -> seq(function16.apply(t)));
    }

    /**
     * Cross apply 2 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 3 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 4 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 5 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 6 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 7 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 8 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).map(t8 -> t.concat(t8)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 9 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).map(t9 -> t.concat(t9)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 10 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).map(t10 -> t.concat(t10)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 11 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).map(t11 -> t.concat(t11)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 12 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).map(t12 -> t.concat(t12)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 13 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12, Function<? super T12, ? extends Seq<? extends T13>> function13) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).map(t12 -> t.concat(t12)))
                  .flatMap(t -> function13.apply(t.v12).map(t13 -> t.concat(t13)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 14 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12, Function<? super T12, ? extends Seq<? extends T13>> function13, Function<? super T13, ? extends Seq<? extends T14>> function14) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).map(t12 -> t.concat(t12)))
                  .flatMap(t -> function13.apply(t.v12).map(t13 -> t.concat(t13)))
                  .flatMap(t -> function14.apply(t.v13).map(t14 -> t.concat(t14)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 15 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12, Function<? super T12, ? extends Seq<? extends T13>> function13, Function<? super T13, ? extends Seq<? extends T14>> function14, Function<? super T14, ? extends Seq<? extends T15>> function15) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).map(t12 -> t.concat(t12)))
                  .flatMap(t -> function13.apply(t.v12).map(t13 -> t.concat(t13)))
                  .flatMap(t -> function14.apply(t.v13).map(t14 -> t.concat(t14)))
                  .flatMap(t -> function15.apply(t.v14).map(t15 -> t.concat(t15)))
                  .onClose(seq::close);
    }

    /**
     * Cross apply 16 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(1, 2).crossApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> crossApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12, Function<? super T12, ? extends Seq<? extends T13>> function13, Function<? super T13, ? extends Seq<? extends T14>> function14, Function<? super T14, ? extends Seq<? extends T15>> function15, Function<? super T15, ? extends Seq<? extends T16>> function16) {
        return seq.flatMap(t1 -> function2.apply(t1).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).map(t12 -> t.concat(t12)))
                  .flatMap(t -> function13.apply(t.v12).map(t13 -> t.concat(t13)))
                  .flatMap(t -> function14.apply(t.v13).map(t14 -> t.concat(t14)))
                  .flatMap(t -> function15.apply(t.v14).map(t15 -> t.concat(t15)))
                  .flatMap(t -> function16.apply(t.v15).map(t16 -> t.concat(t16)))
                  .onClose(seq::close);
    }

// [jooq-tools] END [crossapply-static]

    // [jooq-tools] START [outerapply-static]

    /**
     * Outer apply 2 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)));
    }

    /**
     * Outer apply 3 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)));
    }

    /**
     * Outer apply 4 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)));
    }

    /**
     * Outer apply 5 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)));
    }

    /**
     * Outer apply 6 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)));
    }

    /**
     * Outer apply 7 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)));
    }

    /**
     * Outer apply 8 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)));
    }

    /**
     * Outer apply 9 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)));
    }

    /**
     * Outer apply 10 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)));
    }

    /**
     * Outer apply 11 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)));
    }

    /**
     * Outer apply 12 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)));
    }

    /**
     * Outer apply 13 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12, Function<? super T12, ? extends Stream<? extends T13>> function13) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)));
    }

    /**
     * Outer apply 14 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12, Function<? super T12, ? extends Stream<? extends T13>> function13, Function<? super T13, ? extends Stream<? extends T14>> function14) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)));
    }

    /**
     * Outer apply 15 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12, Function<? super T12, ? extends Stream<? extends T13>> function13, Function<? super T13, ? extends Stream<? extends T14>> function14, Function<? super T14, ? extends Stream<? extends T15>> function15) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)), t -> seq(function15.apply(t)));
    }

    /**
     * Outer apply 16 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> outerApply(Stream<? extends T1> stream, Function<? super T1, ? extends Stream<? extends T2>> function2, Function<? super T2, ? extends Stream<? extends T3>> function3, Function<? super T3, ? extends Stream<? extends T4>> function4, Function<? super T4, ? extends Stream<? extends T5>> function5, Function<? super T5, ? extends Stream<? extends T6>> function6, Function<? super T6, ? extends Stream<? extends T7>> function7, Function<? super T7, ? extends Stream<? extends T8>> function8, Function<? super T8, ? extends Stream<? extends T9>> function9, Function<? super T9, ? extends Stream<? extends T10>> function10, Function<? super T10, ? extends Stream<? extends T11>> function11, Function<? super T11, ? extends Stream<? extends T12>> function12, Function<? super T12, ? extends Stream<? extends T13>> function13, Function<? super T13, ? extends Stream<? extends T14>> function14, Function<? super T14, ? extends Stream<? extends T15>> function15, Function<? super T15, ? extends Stream<? extends T16>> function16) {
        return outerApply(seq(stream), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)), t -> seq(function15.apply(t)), t -> seq(function16.apply(t)));
    }

    /**
     * Outer apply 2 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)));
    }

    /**
     * Outer apply 3 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)));
    }

    /**
     * Outer apply 4 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)));
    }

    /**
     * Outer apply 5 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)));
    }

    /**
     * Outer apply 6 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)));
    }

    /**
     * Outer apply 7 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)));
    }

    /**
     * Outer apply 8 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)));
    }

    /**
     * Outer apply 9 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)));
    }

    /**
     * Outer apply 10 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)));
    }

    /**
     * Outer apply 11 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)));
    }

    /**
     * Outer apply 12 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)));
    }

    /**
     * Outer apply 13 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12, Function<? super T12, ? extends Iterable<? extends T13>> function13) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)));
    }

    /**
     * Outer apply 14 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12, Function<? super T12, ? extends Iterable<? extends T13>> function13, Function<? super T13, ? extends Iterable<? extends T14>> function14) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)));
    }

    /**
     * Outer apply 15 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12, Function<? super T12, ? extends Iterable<? extends T13>> function13, Function<? super T13, ? extends Iterable<? extends T14>> function14, Function<? super T14, ? extends Iterable<? extends T15>> function15) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)), t -> seq(function15.apply(t)));
    }

    /**
     * Outer apply 16 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> outerApply(Iterable<? extends T1> iterable, Function<? super T1, ? extends Iterable<? extends T2>> function2, Function<? super T2, ? extends Iterable<? extends T3>> function3, Function<? super T3, ? extends Iterable<? extends T4>> function4, Function<? super T4, ? extends Iterable<? extends T5>> function5, Function<? super T5, ? extends Iterable<? extends T6>> function6, Function<? super T6, ? extends Iterable<? extends T7>> function7, Function<? super T7, ? extends Iterable<? extends T8>> function8, Function<? super T8, ? extends Iterable<? extends T9>> function9, Function<? super T9, ? extends Iterable<? extends T10>> function10, Function<? super T10, ? extends Iterable<? extends T11>> function11, Function<? super T11, ? extends Iterable<? extends T12>> function12, Function<? super T12, ? extends Iterable<? extends T13>> function13, Function<? super T13, ? extends Iterable<? extends T14>> function14, Function<? super T14, ? extends Iterable<? extends T15>> function15, Function<? super T15, ? extends Iterable<? extends T16>> function16) {
        return outerApply(seq(iterable), t -> seq(function2.apply(t)), t -> seq(function3.apply(t)), t -> seq(function4.apply(t)), t -> seq(function5.apply(t)), t -> seq(function6.apply(t)), t -> seq(function7.apply(t)), t -> seq(function8.apply(t)), t -> seq(function9.apply(t)), t -> seq(function10.apply(t)), t -> seq(function11.apply(t)), t -> seq(function12.apply(t)), t -> seq(function13.apply(t)), t -> seq(function14.apply(t)), t -> seq(function15.apply(t)), t -> seq(function16.apply(t)));
    }

    /**
     * Outer apply 2 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 3 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 4 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 5 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 6 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 7 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 8 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).onEmpty(null).map(t8 -> t.concat(t8)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 9 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).onEmpty(null).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).onEmpty(null).map(t9 -> t.concat(t9)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 10 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).onEmpty(null).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).onEmpty(null).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).onEmpty(null).map(t10 -> t.concat(t10)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 11 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).onEmpty(null).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).onEmpty(null).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).onEmpty(null).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).onEmpty(null).map(t11 -> t.concat(t11)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 12 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).onEmpty(null).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).onEmpty(null).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).onEmpty(null).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).onEmpty(null).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).onEmpty(null).map(t12 -> t.concat(t12)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 13 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12, Function<? super T12, ? extends Seq<? extends T13>> function13) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).onEmpty(null).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).onEmpty(null).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).onEmpty(null).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).onEmpty(null).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).onEmpty(null).map(t12 -> t.concat(t12)))
                  .flatMap(t -> function13.apply(t.v12).onEmpty(null).map(t13 -> t.concat(t13)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 14 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12, Function<? super T12, ? extends Seq<? extends T13>> function13, Function<? super T13, ? extends Seq<? extends T14>> function14) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).onEmpty(null).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).onEmpty(null).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).onEmpty(null).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).onEmpty(null).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).onEmpty(null).map(t12 -> t.concat(t12)))
                  .flatMap(t -> function13.apply(t.v12).onEmpty(null).map(t13 -> t.concat(t13)))
                  .flatMap(t -> function14.apply(t.v13).onEmpty(null).map(t14 -> t.concat(t14)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 15 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12, Function<? super T12, ? extends Seq<? extends T13>> function13, Function<? super T13, ? extends Seq<? extends T14>> function14, Function<? super T14, ? extends Seq<? extends T15>> function15) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).onEmpty(null).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).onEmpty(null).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).onEmpty(null).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).onEmpty(null).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).onEmpty(null).map(t12 -> t.concat(t12)))
                  .flatMap(t -> function13.apply(t.v12).onEmpty(null).map(t13 -> t.concat(t13)))
                  .flatMap(t -> function14.apply(t.v13).onEmpty(null).map(t14 -> t.concat(t14)))
                  .flatMap(t -> function15.apply(t.v14).onEmpty(null).map(t15 -> t.concat(t15)))
                  .onClose(seq::close);
    }

    /**
     * Outer apply 16 functions to a stream.
     * <p>
     * <code><pre>
     * // (tuple(0, null), tuple(1, 0), tuple(2, 0), tuple(2, 1))
     * Seq.of(0, 1, 2).outerApply(t -> Seq.range(0, t))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> outerApply(Seq<? extends T1> seq, Function<? super T1, ? extends Seq<? extends T2>> function2, Function<? super T2, ? extends Seq<? extends T3>> function3, Function<? super T3, ? extends Seq<? extends T4>> function4, Function<? super T4, ? extends Seq<? extends T5>> function5, Function<? super T5, ? extends Seq<? extends T6>> function6, Function<? super T6, ? extends Seq<? extends T7>> function7, Function<? super T7, ? extends Seq<? extends T8>> function8, Function<? super T8, ? extends Seq<? extends T9>> function9, Function<? super T9, ? extends Seq<? extends T10>> function10, Function<? super T10, ? extends Seq<? extends T11>> function11, Function<? super T11, ? extends Seq<? extends T12>> function12, Function<? super T12, ? extends Seq<? extends T13>> function13, Function<? super T13, ? extends Seq<? extends T14>> function14, Function<? super T14, ? extends Seq<? extends T15>> function15, Function<? super T15, ? extends Seq<? extends T16>> function16) {
        return seq.flatMap(t1 -> function2.apply(t1).onEmpty(null).map(t2 -> tuple(t1, t2)))
                  .flatMap(t -> function3.apply(t.v2).onEmpty(null).map(t3 -> t.concat(t3)))
                  .flatMap(t -> function4.apply(t.v3).onEmpty(null).map(t4 -> t.concat(t4)))
                  .flatMap(t -> function5.apply(t.v4).onEmpty(null).map(t5 -> t.concat(t5)))
                  .flatMap(t -> function6.apply(t.v5).onEmpty(null).map(t6 -> t.concat(t6)))
                  .flatMap(t -> function7.apply(t.v6).onEmpty(null).map(t7 -> t.concat(t7)))
                  .flatMap(t -> function8.apply(t.v7).onEmpty(null).map(t8 -> t.concat(t8)))
                  .flatMap(t -> function9.apply(t.v8).onEmpty(null).map(t9 -> t.concat(t9)))
                  .flatMap(t -> function10.apply(t.v9).onEmpty(null).map(t10 -> t.concat(t10)))
                  .flatMap(t -> function11.apply(t.v10).onEmpty(null).map(t11 -> t.concat(t11)))
                  .flatMap(t -> function12.apply(t.v11).onEmpty(null).map(t12 -> t.concat(t12)))
                  .flatMap(t -> function13.apply(t.v12).onEmpty(null).map(t13 -> t.concat(t13)))
                  .flatMap(t -> function14.apply(t.v13).onEmpty(null).map(t14 -> t.concat(t14)))
                  .flatMap(t -> function15.apply(t.v14).onEmpty(null).map(t15 -> t.concat(t15)))
                  .flatMap(t -> function16.apply(t.v15).onEmpty(null).map(t16 -> t.concat(t16)))
                  .onClose(seq::close);
    }

// [jooq-tools] END [outerapply-static]

    // [jooq-tools] START [crossjoin-static]

    /**
     * Cross join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2) {
        return crossJoin(seq(s1), seq(s2));
    }

    /**
     * Cross join 3 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3) {
        return crossJoin(seq(s1), seq(s2), seq(s3));
    }

    /**
     * Cross join 4 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4));
    }

    /**
     * Cross join 5 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5));
    }

    /**
     * Cross join 6 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6));
    }

    /**
     * Cross join 7 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7));
    }

    /**
     * Cross join 8 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8));
    }

    /**
     * Cross join 9 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9));
    }

    /**
     * Cross join 10 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10));
    }

    /**
     * Cross join 11 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11));
    }

    /**
     * Cross join 12 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12));
    }

    /**
     * Cross join 13 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13));
    }

    /**
     * Cross join 14 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14));
    }

    /**
     * Cross join 15 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15));
    }

    /**
     * Cross join 16 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> crossJoin(Stream<? extends T1> s1, Stream<? extends T2> s2, Stream<? extends T3> s3, Stream<? extends T4> s4, Stream<? extends T5> s5, Stream<? extends T6> s6, Stream<? extends T7> s7, Stream<? extends T8> s8, Stream<? extends T9> s9, Stream<? extends T10> s10, Stream<? extends T11> s11, Stream<? extends T12> s12, Stream<? extends T13> s13, Stream<? extends T14> s14, Stream<? extends T15> s15, Stream<? extends T16> s16) {
        return crossJoin(seq(s1), seq(s2), seq(s3), seq(s4), seq(s5), seq(s6), seq(s7), seq(s8), seq(s9), seq(s10), seq(s11), seq(s12), seq(s13), seq(s14), seq(s15), seq(s16));
    }

    /**
     * Cross join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2) {
        return crossJoin(seq(i1), seq(i2));
    }

    /**
     * Cross join 3 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3) {
        return crossJoin(seq(i1), seq(i2), seq(i3));
    }

    /**
     * Cross join 4 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4));
    }

    /**
     * Cross join 5 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5));
    }

    /**
     * Cross join 6 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6));
    }

    /**
     * Cross join 7 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7));
    }

    /**
     * Cross join 8 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8));
    }

    /**
     * Cross join 9 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9));
    }

    /**
     * Cross join 10 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10));
    }

    /**
     * Cross join 11 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11));
    }

    /**
     * Cross join 12 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12));
    }

    /**
     * Cross join 13 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13));
    }

    /**
     * Cross join 14 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Iterable<? extends T14> i14) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), seq(i14));
    }

    /**
     * Cross join 15 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Iterable<? extends T14> i14, Iterable<? extends T15> i15) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), seq(i14), seq(i15));
    }

    /**
     * Cross join 16 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> crossJoin(Iterable<? extends T1> i1, Iterable<? extends T2> i2, Iterable<? extends T3> i3, Iterable<? extends T4> i4, Iterable<? extends T5> i5, Iterable<? extends T6> i6, Iterable<? extends T7> i7, Iterable<? extends T8> i8, Iterable<? extends T9> i9, Iterable<? extends T10> i10, Iterable<? extends T11> i11, Iterable<? extends T12> i12, Iterable<? extends T13> i13, Iterable<? extends T14> i14, Iterable<? extends T15> i15, Iterable<? extends T16> i16) {
        return crossJoin(seq(i1), seq(i2), seq(i3), seq(i4), seq(i5), seq(i6), seq(i7), seq(i8), seq(i9), seq(i10), seq(i11), seq(i12), seq(i13), seq(i14), seq(i15), seq(i16));
    }

    /**
     * Cross join 2 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2> Seq<Tuple2<T1, T2>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2) {
        List<? extends T2> list = s2.toList();
        return seq(s1).flatMap(v1 -> seq(list).map(v2 -> tuple(v1, v2)))
                      .onClose(SeqUtils.closeAll(s1, s2));
    }

    /**
     * Cross join 3 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3> Seq<Tuple3<T1, T2, T3>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple2<T2, T3>> list = Seq.<T2, T3>crossJoin(s2, s3).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2)))
                 .onClose(SeqUtils.closeAll(s2, s3));
    }

    /**
     * Cross join 4 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4> Seq<Tuple4<T1, T2, T3, T4>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple3<T2, T3, T4>> list = Seq.<T2, T3, T4>crossJoin(s2, s3, s4).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4));
    }

    /**
     * Cross join 5 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5> Seq<Tuple5<T1, T2, T3, T4, T5>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple4<T2, T3, T4, T5>> list = Seq.<T2, T3, T4, T5>crossJoin(s2, s3, s4, s5).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5));
    }

    /**
     * Cross join 6 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6> Seq<Tuple6<T1, T2, T3, T4, T5, T6>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple5<T2, T3, T4, T5, T6>> list = Seq.<T2, T3, T4, T5, T6>crossJoin(s2, s3, s4, s5, s6).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6));
    }

    /**
     * Cross join 7 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7> Seq<Tuple7<T1, T2, T3, T4, T5, T6, T7>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple6<T2, T3, T4, T5, T6, T7>> list = Seq.<T2, T3, T4, T5, T6, T7>crossJoin(s2, s3, s4, s5, s6, s7).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7));
    }

    /**
     * Cross join 8 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Seq<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple7<T2, T3, T4, T5, T6, T7, T8>> list = Seq.<T2, T3, T4, T5, T6, T7, T8>crossJoin(s2, s3, s4, s5, s6, s7, s8).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6, t.v7)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7, s8));
    }

    /**
     * Cross join 9 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Seq<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple8<T2, T3, T4, T5, T6, T7, T8, T9>> list = Seq.<T2, T3, T4, T5, T6, T7, T8, T9>crossJoin(s2, s3, s4, s5, s6, s7, s8, s9).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6, t.v7, t.v8)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7, s8, s9));
    }

    /**
     * Cross join 10 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Seq<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple9<T2, T3, T4, T5, T6, T7, T8, T9, T10>> list = Seq.<T2, T3, T4, T5, T6, T7, T8, T9, T10>crossJoin(s2, s3, s4, s5, s6, s7, s8, s9, s10).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6, t.v7, t.v8, t.v9)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7, s8, s9, s10));
    }

    /**
     * Cross join 11 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Seq<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple10<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> list = Seq.<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>crossJoin(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6, t.v7, t.v8, t.v9, t.v10)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11));
    }

    /**
     * Cross join 12 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Seq<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple11<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> list = Seq.<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>crossJoin(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6, t.v7, t.v8, t.v9, t.v10, t.v11)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12));
    }

    /**
     * Cross join 13 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Seq<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple12<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> list = Seq.<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>crossJoin(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6, t.v7, t.v8, t.v9, t.v10, t.v11, t.v12)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13));
    }

    /**
     * Cross join 14 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Seq<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple13<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> list = Seq.<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>crossJoin(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6, t.v7, t.v8, t.v9, t.v10, t.v11, t.v12, t.v13)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14));
    }

    /**
     * Cross join 15 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Seq<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple14<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> list = Seq.<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>crossJoin(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6, t.v7, t.v8, t.v9, t.v10, t.v11, t.v12, t.v13, t.v14)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15));
    }

    /**
     * Cross join 16 streams into one.
     * <p>
     * <code><pre>
     * // (tuple(1, "a"), tuple(1, "b"), tuple(2, "a"), tuple(2, "b"))
     * Seq.of(1, 2).crossJoin(Seq.of("a", "b"))
     * </pre></code>
     */
    /// @Generated("This method was generated using jOOQ-tools")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Seq<Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> crossJoin(Seq<? extends T1> s1, Seq<? extends T2> s2, Seq<? extends T3> s3, Seq<? extends T4> s4, Seq<? extends T5> s5, Seq<? extends T6> s6, Seq<? extends T7> s7, Seq<? extends T8> s8, Seq<? extends T9> s9, Seq<? extends T10> s10, Seq<? extends T11> s11, Seq<? extends T12> s12, Seq<? extends T13> s13, Seq<? extends T14> s14, Seq<? extends T15> s15, Seq<? extends T16> s16) {

        // [#323] Some explicit type variable bindings required because of compiler regressions in JDK 9
        List<Tuple15<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> list = Seq.<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>crossJoin(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16).toList();
        return s1.flatMap(v1 -> seq(list).map(t -> tuple(v1, t.v1, t.v2, t.v3, t.v4, t.v5, t.v6, t.v7, t.v8, t.v9, t.v10, t.v11, t.v12, t.v13, t.v14, t.v15)))
                 .onClose(SeqUtils.closeAll(s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16));
    }

// [jooq-tools] END [crossjoin-static]

    /**
     * Concatenate a number of streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).concat(Seq.of(4, 5, 6))
     * </pre></code>
     */
    @SafeVarargs
    @SuppressWarnings({ "unchecked" })
    static <T> Seq<T> concat(Stream<? extends T>... streams) {
        return concat(SeqUtils.seqs(streams));
    }

    /**
     * Concatenate a number of streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).concat(Seq.of(4, 5, 6))
     * </pre></code>
     */
    @SafeVarargs
    @SuppressWarnings({ "unchecked" })
    static <T> Seq<T> concat(Iterable<? extends T>... iterables) {
        return concat(SeqUtils.seqs(iterables));
    }

    /**
     * Concatenate a number of streams.
     * <p>
     * <code><pre>
     * // (1, 2, 3, 4, 5, 6)
     * Seq.of(1, 2, 3).concat(Seq.of(4, 5, 6))
     * </pre></code>
     */
    @SafeVarargs
    static <T> Seq<T> concat(Seq<? extends T>... streams) {
        if (streams == null || streams.length == 0)
            return Seq.empty();

        if (streams.length == 1)
            return seq(streams[0]);

        Stream<? extends T> result = streams[0];
        for (int i = 1; i < streams.length; i++)
            result = Stream.concat(result, streams[i]);

        return seq(result);
    }

    /**
     * Concatenate a number of optionals.
     * <p>
     * <code><pre>
     * // (1, 2)
     * Seq.concat(Optional.of(1), Optional.empty(), Optional.of(2))
     * </pre></code>
     */
    @SafeVarargs
    static <T> Seq<T> concat(Optional<? extends T>... optionals) {
        if (optionals == null)
            return null;

        return Seq.of(optionals).filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Duplicate a Streams into two equivalent Streams.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (1, 2, 3))
     * Seq.of(1, 2, 3).duplicate()
     * </pre></code>
     */
    static <T> Tuple2<Seq<T>, Seq<T>> duplicate(Stream<? extends T> stream) {
        SeqBuffer<T> buffer = SeqBuffer.of(stream);
        return tuple(buffer.seq(), buffer.seq());
    }

    /**
     * Consume a stream and concatenate all elements.
     */
    static String toString(Stream<?> stream) {
        return toString(stream, "");
    }

    /**
     * Consume a stream and concatenate all elements using a separator.
     */
    static String toString(Stream<?> stream, CharSequence delimiter) {
        return stream.map(Objects::toString).collect(Collectors.joining(delimiter));
    }

    /**
     * Collect a Stream into a List.
     */
    static <T, C extends Collection<T>> C toCollection(Stream<? extends T> stream, Supplier<? extends C> collectionFactory) {
        return stream.collect(Collectors.toCollection(collectionFactory));
    }

    /**
     * Collect a Stream into a List.
     */
    static <T> List<T> toList(Stream<? extends T> stream) {
        return stream.collect(Collectors.toList());
    }

    /**
     * Collect a Stream into a Set.
     */
    static <T> Set<T> toSet(Stream<? extends T> stream) {
        return stream.collect(Collectors.toSet());
    }

    /**
     * Collect a Stream of {@link Tuple2} into a Map.
     */
    static <T, K, V> Map<K, V> toMap(Stream<Tuple2<K, V>> stream) {
        return stream.collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));
    }

    /**
     * Collect a Stream into a Map.
     */
    static <T, K, V> Map<K, V> toMap(Stream<? extends T> stream, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return stream.collect(Collectors.toMap(keyMapper, valueMapper));
    }

    /**
     * Returns a limited interval from a given Stream.
     * <p>
     * <code><pre>
     * // (4, 5)
     * Seq.of(1, 2, 3, 4, 5, 6).slice(3, 5)
     * </pre></code>
     */
    static <T> Seq<T> slice(Stream<? extends T> stream, long from, long to) {
        long f = Math.max(from, 0);
        long t = Math.max(to - f, 0);

        return seq(stream.skip(f).limit(t));
    }

    /**
     * Returns a stream with n elements skipped.
     * <p>
     * <code><pre>
     * // (4, 5, 6)
     * Seq.of(1, 2, 3, 4, 5, 6).skip(3)
     * </pre></code>
     */
    static <T> Seq<T> skip(Stream<? extends T> stream, long elements) {
        return seq(stream.skip(elements));
    }

    /**
     * Returns a stream with all elements skipped for which a predicate evaluates to <code>true</code>.
     * <p>
     * <code><pre>
     * // (3, 4, 5)
     * Seq.of(1, 2, 3, 4, 5).skipWhile(i -> i &lt; 3)
     * </pre></code>
     */
    static <T> Seq<T> skipWhile(Stream<? extends T> stream, Predicate<? super T> predicate) {
        return skipUntil(stream, predicate.negate());
    }

    /**
     * Returns a stream with all elements skipped for which a predicate evaluates to <code>true</code>
     * plus the first element for which it evaluates to <code>false</code>.
     * <p>
     * <code><pre>
     * // (4, 5)
     * Seq.of(1, 2, 3, 4, 5).skipWhileClosed(i -> i &lt; 3)
     * </pre></code>
     */
    static <T> Seq<T> skipWhileClosed(Stream<? extends T> stream, Predicate<? super T> predicate) {
        return skipUntilClosed(stream, predicate.negate());
    }

    /**
     * Returns a stream with all elements skipped for which a predicate evaluates to <code>false</code>.
     * <p>
     * <code><pre>
     * // (3, 4, 5)
     * Seq.of(1, 2, 3, 4, 5).skipUntil(i -> i == 3)
     * </pre></code>
     */
    @SuppressWarnings("unchecked")
    static <T> Seq<T> skipUntil(Stream<? extends T> stream, Predicate<? super T> predicate) {
        // [0]: true = we've skipped values until the predicate yielded true
        // [1]: true = there is at least one value that was considered for skipping
        boolean[] test = { false, false };

        return SeqUtils.transform(stream, (delegate, action) -> {
            if (test[0]) {
                return delegate.tryAdvance(action);
            }
            else {
                do {
                    test[1] = delegate.tryAdvance(t -> {
                        if (test[0] = predicate.test(t))
                            action.accept(t);
                    });
                }
                while (test[1] && !test[0]);
                return test[0];
            }
        });
    }

    /**
     * Returns a stream with all elements skipped for which a predicate evaluates to <code>false</code>
     * plus the first element for which it evaluates to <code>true</code>.
     * <p>
     * <code><pre>
     * // (4, 5)
     * Seq.of(1, 2, 3, 4, 5).skipUntilClosed(i -> i == 3)
     * </pre></code>
     */
    @SuppressWarnings("unchecked")
    static <T> Seq<T> skipUntilClosed(Stream<? extends T> stream, Predicate<? super T> predicate) {
        // [0]: true = we've skipped values until the predicate yielded true
        // [1]: true = there is at least one value that was considered for skipping
        boolean[] test = { false, false };

        return SeqUtils.transform(stream, (delegate, action) -> {
            if (!test[0]) {
                do {
                    test[1] = delegate.tryAdvance(t -> test[0] = predicate.test(t));
                }
                while (test[1] && !test[0]);
            }

            return test[0] && delegate.tryAdvance(action);
        });
    }

    /**
     * Returns a stream limited to n elements.
     * <p>
     * <code><pre>
     * // (1, 2, 3)
     * Seq.of(1, 2, 3, 4, 5, 6).limit(3)
     * </pre></code>
     */
    static <T> Seq<T> limit(Stream<? extends T> stream, long elements) {
        return seq(stream.limit(elements));
    }

    /**
     * Alias for limit
     *
     * @see Seq#limit(long)
     */
    default Seq<T> take(long maxSize) {
        return limit(maxSize);
    }

    /**
     * Alias for skip
     *
     * @see Seq#skip(long)
     */
    default Seq<T> drop(long n) {
        return skip(n);
    }

    /**
     * Returns a stream limited to all elements for which a predicate evaluates to <code>true</code>.
     * <p>
     * <code><pre>
     * // (1, 2)
     * Seq.of(1, 2, 3, 4, 5).limitWhile(i -> i &lt; 3)
     * </pre></code>
     */
    static <T> Seq<T> limitWhile(Stream<? extends T> stream, Predicate<? super T> predicate) {
        return limitUntil(stream, predicate.negate());
    }

    /**
     * Returns a stream limited to all elements for which a predicate evaluates to <code>true</code>
     * plus the first element for which it evaluates to <code>false</code>.
     * <p>
     * <code><pre>
     * // (1, 2, 3)
     * Seq.of(1, 2, 3, 4, 5).limitWhileClosed(i -> i &lt; 3)
     * </pre></code>
     */
    static <T> Seq<T> limitWhileClosed(Stream<? extends T> stream, Predicate<? super T> predicate) {
        return limitUntilClosed(stream, predicate.negate());
    }

    /**
     * Returns a stream limited to all elements for which a predicate evaluates to <code>false</code>.
     * <p>
     * <code><pre>
     * // (1, 2)
     * Seq.of(1, 2, 3, 4, 5).limitUntil(i -> i == 3)
     * </pre></code>
     */
    @SuppressWarnings("unchecked")
    static <T> Seq<T> limitUntil(Stream<? extends T> stream, Predicate<? super T> predicate) {
        boolean[] test = { false };

        return SeqUtils.transform(stream, (delegate, action) ->
            !test[0] && delegate.tryAdvance(t -> {
                if (!(test[0] = predicate.test(t)))
                    action.accept(t);
            })
        );
    }

    /**
     * Returns a stream limited to all elements for which a predicate evaluates to <code>false</code>
     * plus the first element for which it evaluates to <code>true</code>.
     * <p>
     * <code><pre>
     * // (1, 2, 3)
     * Seq.of(1, 2, 3, 4, 5).limitUntilClosed(i -> i == 3)
     * </pre></code>
     */
    @SuppressWarnings("unchecked")
    static <T> Seq<T> limitUntilClosed(Stream<? extends T> stream, Predicate<? super T> predicate) {
        boolean[] test = { false };

        return SeqUtils.transform(stream, (delegate, action) ->
            !test[0] && delegate.tryAdvance(t -> {
                test[0] = predicate.test(t);
                action.accept(t);
            })
        );
    }

    /**
     * Returns a stream with a given value interspersed between any two values of this stream.
     * <p>
     * <code><pre>
     * // (1, 0, 2, 0, 3, 0, 4)
     * Seq.of(1, 2, 3, 4).intersperse(0)
     * </pre></code>
     */
    static <T> Seq<T> intersperse(Stream<? extends T> stream, T value) {
        return seq(stream.flatMap(t -> Stream.of(value, t)).skip(1));
    }

    /**
     * Classify this stream's elements according to a given classifier function
     * <p>
     * <code><pre>
     * // Seq(tuple(1, Seq(1, 3, 5)), tuple(0, Seq(2, 4, 6)))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 )
     * // Seq(tuple(true, Seq(1, 3, 5)), tuple(false, Seq(2, 4, 6)))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 != 0)
     * </pre></code>
     *
     * This is a non-terminal analog of {@link #groupBy(Stream, Function)})
     * @see #groupBy(Function)
     * @see #partition(Predicate)
     */
    public static <K, T> Seq<Tuple2<K, Seq<T>>> grouped(Stream<? extends T> stream, Function<? super T, ? extends K> classifier) {
        return grouped(seq(stream), classifier);
    }

    /**
     * Classify this stream's elements according to a given classifier function
     * <p>
     * <code><pre>
     * // Seq(tuple(1, Seq(1, 3, 5)), tuple(0, Seq(2, 4, 6)))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 )
     * // Seq(tuple(true, Seq(1, 3, 5)), tuple(false, Seq(2, 4, 6)))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 != 0)
     * </pre></code>
     *
     * This is a non-terminal analog of {@link #groupBy(Stream, Function)})
     * @see #groupBy(Function)
     * @see #partition(Predicate)
     */
    public static <K, T> Seq<Tuple2<K, Seq<T>>> grouped(Iterable<? extends T> iterable, Function<? super T, ? extends K> classifier) {
        return grouped(seq(iterable), classifier);
    }

    /**
     * Classify this stream's elements according to a given classifier function
     * <p>
     * <code><pre>
     * // Seq(tuple(1, Seq(1, 3, 5)), tuple(0, Seq(2, 4, 6)))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 )
     * // Seq(tuple(true, Seq(1, 3, 5)), tuple(false, Seq(2, 4, 6)))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 != 0)
     * </pre></code>
     *
     * This is a non-terminal analog of {@link #groupBy(Stream, Function)})
     * @see #groupBy(Function)
     * @see #partition(Predicate)
     */
    public static <K, T> Seq<Tuple2<K, Seq<T>>> grouped(Seq<? extends T> seq, Function<? super T, ? extends K> classifier) {
        final Iterator<? extends T> it = seq.iterator();

        class ClassifyingIterator implements Iterator<Tuple2<K, Seq<T>>> {
            final Map<K, Queue<T>> buffers = new LinkedHashMap<>();
            final Queue<K> keys = new LinkedList<>();

            class Classification implements Iterator<T> {
                final K key;
                Queue<T> buffer;

                Classification(K key) {
                    this.key = key;
                }

                void fetchClassification() {
                    if (buffer == null)
                        buffer = buffers.get(key);

                    while (buffer.isEmpty() && it.hasNext())
                        fetchNextNewKey();
                }

                @Override
                public boolean hasNext() {
                    fetchClassification();
                    return !buffer.isEmpty();
                }

                @Override
                public T next() {
                    return buffer.poll();
                }
            }

            void fetchClassifying() {
                while (it.hasNext() && fetchNextNewKey());
            }

            boolean fetchNextNewKey() {
                T next = it.next();
                K nextK = classifier.apply(next);

                Queue<T> buffer = buffers.get(nextK);

                try {
                    if (buffer == null) {
                        buffer = new ArrayDeque<>();
                        buffers.put(nextK, buffer);
                        keys.add(nextK);
                        return false;
                    }
                }
                finally {
                    buffer.offer(next);
                }

                return true;
            }

            @Override
            public boolean hasNext() {
                fetchClassifying();
                return !keys.isEmpty();
            }

            @Override
            public Tuple2<K, Seq<T>> next() {
                K nextK = keys.poll();
                return tuple(nextK, seq(new Classification(nextK)));
            }
        }

        return seq(new ClassifyingIterator()).onClose(seq::close);
    }

    /**
     * Classify this stream's elements according to a given classifier function
     * and collect each class's elements using a collector.
     * <p>
     * <code><pre>
     * // Seq(tuple(1, 9), tuple(0, 12))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2, Collectors.summingInt(i -> i))
     * // Seq(tuple(true, 9), tuple(false, 12))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 != 0, Collectors.summingInt(i -> i))
     * </pre></code> This is a non-terminal analog of
     * {@link #groupBy(Function, Collector)})
     *
     * @see #groupBy(Function, Collector)
     */
    public static <K, T, A, D> Seq<Tuple2<K, D>> grouped(Stream<? extends T> stream, Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return grouped(seq(stream), classifier, downstream);
    }

    /**
     * Classify this stream's elements according to a given classifier function
     * and collect each class's elements using a collector.
     * <p>
     * <code><pre>
     * // Seq(tuple(1, 9), tuple(0, 12))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2, Collectors.summingInt(i -> i))
     * // Seq(tuple(true, 9), tuple(false, 12))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 != 0, Collectors.summingInt(i -> i))
     * </pre></code> This is a non-terminal analog of
     * {@link #groupBy(Function, Collector)})
     *
     * @see #groupBy(Function, Collector)
     */
    public static <K, T, A, D> Seq<Tuple2<K, D>> grouped(Iterable<? extends T> iterable, Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return grouped(seq(iterable), classifier, downstream);
    }

    /**
     * Classify this stream's elements according to a given classifier function
     * and collect each class's elements using a collector.
     * <p>
     * <code><pre>
     * // Seq(tuple(1, 9), tuple(0, 12))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2, Collectors.summingInt(i -> i))
     * // Seq(tuple(true, 9), tuple(false, 12))
     * Seq.of(1, 2, 3, 4, 5, 6).grouped(i -> i % 2 != 0, Collectors.summingInt(i -> i))
     * </pre></code> This is a non-terminal analog of
     * {@link #groupBy(Function, Collector)})
     *
     * @see #groupBy(Function, Collector)
     */
    public static <K, T, A, D> Seq<Tuple2<K, D>> grouped(Seq<? extends T> seq, Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return grouped(seq, classifier).map(t -> tuple(t.v1, t.v2.collect(downstream)));
    }

    /**
     * Partition a stream into two given a predicate.
     * <p>
     * <code><pre>
     * // tuple((1, 3, 5), (2, 4, 6))
     * Seq.of(1, 2, 3, 4, 5, 6).partition(i -> i % 2 != 0)
     * </pre></code>
     */
    static <T> Tuple2<Seq<T>, Seq<T>> partition(Stream<? extends T> stream, Predicate<? super T> predicate) {
        final Iterator<? extends T> it = stream.iterator();
        final LinkedList<T> buffer1 = new LinkedList<>();
        final LinkedList<T> buffer2 = new LinkedList<>();

        class Partition implements Iterator<T> {

            final boolean b;

            Partition(boolean b) {
                this.b = b;
            }

            void fetch() {
                while (buffer(b).isEmpty() && it.hasNext()) {
                    T next = it.next();
                    buffer(predicate.test(next)).offer(next);
                }
            }

            LinkedList<T> buffer(boolean test) {
                return test ? buffer1 : buffer2;
            }

            @Override
            public boolean hasNext() {
                fetch();
                return !buffer(b).isEmpty();
            }

            @Override
            public T next() {
                return buffer(b).poll();
            }
        }

        return tuple(seq(new Partition(true)), seq(new Partition(false)));
    }

    /**
     * Split a stream at a given position.
     * <p>
     * <code><pre>
     * // tuple((1, 2, 3), (4, 5, 6))
     * Seq.of(1, 2, 3, 4, 5, 6).splitAt(3)
     * </pre></code>
     */
    static <T> Tuple2<Seq<T>, Seq<T>> splitAt(Stream<? extends T> stream, long position) {
        SeqBuffer<T> buffer = SeqBuffer.of(stream);
        return tuple(buffer.seq().limit(position), buffer.seq().skip(position));
    }

    /**
     * Split a stream at the head.
     * <p>
     * <code><pre>
     * // tuple(1, (2, 3, 4, 5, 6))
     * Seq.of(1, 2, 3, 4, 5, 6).splitHead(3)
     * </pre></code>
     */
    static <T> Tuple2<Optional<T>, Seq<T>> splitAtHead(Stream<T> stream) {
        Iterator<T> it = stream.iterator();
        return tuple(it.hasNext() ?
                Optional.of(it.next()) :
                Optional.empty(), seq(it));
    }

    // Methods taken from LINQ
    // -----------------------

    /**
     * Keep only those elements in a stream that are of a given type.
     * <p>
     * <code><pre>
     * // (1, 2, 3)
     * Seq.of(1, "a", 2, "b", 3).ofType(Integer.class)
     * </pre></code>
     */
    @SuppressWarnings("unchecked")
    static <T, U> Seq<U> ofType(Stream<? extends T> stream, Class<? extends U> type) {
        return seq(stream).filter(type::isInstance).map(t -> (U) t);
    }

    /**
     * Cast all elements in a stream to a given type, possibly throwing a {@link ClassCastException}.
     * <p>
     * <code><pre>
     * // ClassCastException
     * Seq.of(1, "a", 2, "b", 3).cast(Integer.class)
     * </pre></code>
     *
     * @see #ofType(Stream, Class) Seq.ofType(Stream, Class) If you want to filter and cast
     */
    static <T, U> Seq<U> cast(Stream<? extends T> stream, Class<? extends U> type) {
        return seq(stream).map(type::cast);
    }

    // Shortcuts to Collectors
    // -----------------------

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#groupingBy(Function)} collector.
     */
    static <T, K> Map<K, List<T>> groupBy(Stream<? extends T> stream, Function<? super T, ? extends K> classifier) {
        return Seq.<T>seq(stream).groupBy(classifier);
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#groupingBy(Function, Collector)} collector.
     */
    static <T, K, A, D> Map<K, D> groupBy(Stream<? extends T> stream, Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return seq(stream).groupBy(classifier, downstream);
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#groupingBy(Function, Supplier, Collector)} collector.
     */
    static <T, K, D, A, M extends Map<K, D>> M groupBy(Stream<? extends T> stream, Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        return seq(stream).groupBy(classifier, mapFactory, downstream);
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#joining()}
     * collector.
     *
     * @deprecated - Use {@link Object#toString()} instead. This method will be
     * removed in the future as it causes confusion with
     * {@link #innerJoin(Seq, BiPredicate)}.
     */
    @Deprecated
    static String join(Stream<?> stream) {
        return seq(stream).join();
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#joining(CharSequence)}
     * collector.
     *
     * @deprecated - Use {@link Object#toString()} instead. This method will be
     * removed in the future as it causes confusion with
     * {@link #innerJoin(Seq, BiPredicate)}.
     */
    @Deprecated
    static String join(Stream<?> stream, CharSequence delimiter) {
        return seq(stream).join(delimiter);
    }

    /**
     * Shortcut for calling {@link Stream#collect(Collector)} with a
     * {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}
     * collector.
     *
     * @deprecated - Use {@link Object#toString()} instead. This method will be
     * removed in the future as it causes confusion with
     * {@link #innerJoin(Seq, BiPredicate)}.
     */
    @Deprecated
    static String join(Stream<?> stream, CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return seq(stream).join(delimiter, prefix, suffix);
    }

    // Covariant overriding of Stream return types
    // -------------------------------------------

    @Override
    Seq<T> filter(Predicate<? super T> predicate);

    @Override
    <R> Seq<R> map(Function<? super T, ? extends R> mapper);

    @Override
    IntStream mapToInt(ToIntFunction<? super T> mapper);

    @Override
    LongStream mapToLong(ToLongFunction<? super T> mapper);

    @Override
    DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    @Override
    <R> Seq<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    @Override
    IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);

    @Override
    LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper);

    @Override
    DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper);

    @Override
    Seq<T> distinct();

    @Override
    Seq<T> sorted();

    @Override
    Seq<T> sorted(Comparator<? super T> comparator);

    @Override
    Seq<T> peek(Consumer<? super T> action);

    @Override
    Seq<T> limit(long maxSize);

    @Override
    Seq<T> skip(long n);

    @Override
    Seq<T> onClose(Runnable closeHandler);

    @Override
    void close();

    @Override
    long count();

    // These methods have no effect
    // ----------------------------

    /**
     * Returns this stream. All Seq streams are sequential, hence the name.
     *
     * @return this stream unmodified
     */
    @Override
    default Seq<T> sequential() {
        return this;
    }

    /**
     * Seq streams are always sequential and, as such, doesn't support
     * parallelization.
     *
     * @return this sequential stream unmodified
     * @see <a href="https://github.com/jOOQ/jOOL/issues/130">jOOL Issue #130</a>
     */
    @Override
    default Seq<T> parallel() {
        return this;
    }

    /**
     * Returns this stream. All Seq streams are ordered so this method has
     * no effect.
     *
     * @return this stream unmodified
     */
    @Override
    default Seq<T> unordered() {
        return this;
    }

    @Override
    default Spliterator<T> spliterator() {
        return Iterable.super.spliterator();
    }

    @Override
    default void forEach(Consumer<? super T> action) {
        Iterable.super.forEach(action);
    }

    // Debugging tools
    // ---------------

    /**
     * Generate a nicely formatted representation of this stream.
     * <p>
     * Clients should not rely on the concrete formatting of this method, which
     * is intended for debugging convenience only.
     */
    String format();

    /**
     * Print contents of this stream to {@link System#out}.
     */
    default void printOut() {
        print(System.out);
    }

    /**
     * Print contents of this stream to {@link System#err}.
     */
    default void printErr() {
        print(System.err);
    }

    /**
     * Print contents of this stream to the argument writer.
     */
    default void print(PrintWriter writer) {
        forEach(writer::println);
    }

    /**
     * Print contents of this stream to the argument stream.
     */
    default void print(PrintStream stream) {
        forEach(stream::println);
    }
}
