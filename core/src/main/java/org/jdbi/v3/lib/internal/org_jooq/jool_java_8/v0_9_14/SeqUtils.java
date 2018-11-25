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
import static org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Seq.seq;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple2;

/**
 * @author Lukas Eder
 */
class SeqUtils {

    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T> Seq<T>[] seqs(Stream<? extends T>... streams) {
        if (streams == null)
            return null;

        return Seq.of(streams).map(Seq::seq).toArray(Seq[]::new);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T> Seq<T>[] seqs(Iterable<? extends T>... iterables) {
        if (iterables == null)
            return null;

        return Seq.of(iterables).map(Seq::seq).toArray(Seq[]::new);
    }

    static <T, U> Seq<U> transform(Stream<? extends T> stream, DelegatingSpliterator<T, U> delegating) {
        Spliterator<? extends T> delegate = stream.spliterator();

        return Seq.seq(new Spliterator<U>() {
            @Override
            public boolean tryAdvance(Consumer<? super U> action) {
                return delegating.tryAdvance(delegate, action);
            }

            @Override
            public Spliterator<U> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return delegate.characteristics() & Spliterator.ORDERED;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Comparator<? super U> getComparator() {

                // This implementation works with the JDK 8, as the information
                // is really only used in
                // java.util.stream.StreamOpFlag.fromCharacteristics(Spliterator<?> spliterator)
                // Currently, the point of this method is only to be used for
                // optimisations (e.g. to avoid sorting a stream twice in a row)
                return (Comparator) delegate.getComparator();
            }
        }).onClose(() -> stream.close());
    }

    static <T> Map<?, Partition<T>> partitions(WindowSpecification<T> window, List<Tuple2<T, Long>> input) {
        return seq(input).groupBy(
            window.partition().compose(t -> t.v1),
            Collector.<
                Tuple2<T, Long>,
                Collection<Tuple2<T, Long>>,
                Partition<T>
            >of(
                () -> window.order().isPresent()
                    ? new TreeSet<>(comparing((Tuple2<T, Long> t) -> t.v1, window.order().get()).thenComparing(t -> t.v2))
                    : new ArrayList<>(),
                (s, t) -> s.add(t),
                (s1, s2) -> { s1.addAll(s2); return s1; },
                Partition::new
            )
        );
    }

    static <T> OptionalLong indexOf(Iterator<T> iterator, Predicate<? super T> predicate) {
        for (long index = 0; iterator.hasNext(); index++)
            if (predicate.test(iterator.next()))
                return OptionalLong.of(index);

        return OptionalLong.empty();
    }

    /**
     * Sneaky throw any type of Throwable.
     */
    static void sneakyThrow(Throwable throwable) {
        SeqUtils.<RuntimeException>sneakyThrow0(throwable);
    }

    /**
     * Sneaky throw any type of Throwable.
     */
    @SuppressWarnings("unchecked")
    static <E extends Throwable> void sneakyThrow0(Throwable throwable) throws E {
        throw (E) throwable;
    }

    @FunctionalInterface
    interface DelegatingSpliterator<T, U> {
        boolean tryAdvance(Spliterator<? extends T> delegate, Consumer<? super U> action);
    }

    static Runnable closeAll(AutoCloseable... closeables) {
        return () -> {
            Throwable t = null;

            for (AutoCloseable closeable : closeables) {
                try {
                    closeable.close();
                }
                catch (Throwable t1) {
                    if (t == null)
                        t = t1;
                    else
                        t.addSuppressed(t1);
                }
            }

            if (t != null)
                sneakyThrow(t);
        };
    }
}
