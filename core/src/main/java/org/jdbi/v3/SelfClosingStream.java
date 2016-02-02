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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class SelfClosingStream<T> implements Stream<T> {
    private final Stream<T> delegate;

    SelfClosingStream(Stream<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        return new SelfClosingStream<>(delegate.filter(predicate));
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return new SelfClosingStream<>(delegate.map(mapper));
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return new SelfClosingIntStream(delegate.mapToInt(mapper));
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return new SelfClosingLongStream(delegate.mapToLong(mapper));
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return new SelfClosingDoubleStream(delegate.mapToDouble(mapper));
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return new SelfClosingStream<>(delegate.flatMap(mapper));
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return new SelfClosingIntStream(delegate.flatMapToInt(mapper));
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return new SelfClosingLongStream(delegate.flatMapToLong(mapper));
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return new SelfClosingDoubleStream(delegate.flatMapToDouble(mapper));
    }

    @Override
    public Stream<T> distinct() {
        return new SelfClosingStream<>(delegate.distinct());
    }

    @Override
    public Stream<T> sorted() {
        return new SelfClosingStream<>(delegate.sorted());
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        return new SelfClosingStream<>(delegate.sorted(comparator));
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        return new SelfClosingStream<>(delegate.peek(action));
    }

    @Override
    public Stream<T> limit(long maxSize) {
        return new SelfClosingStream<>(delegate.limit(maxSize));
    }

    @Override
    public Stream<T> skip(long n) {
        return new SelfClosingStream<>(delegate.skip(n));
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        try {
            delegate.forEach(action);
        }
        finally {
            close();
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        try {
            delegate.forEachOrdered(action);
        }
        finally {
            close();
        }
    }

    @Override
    public Object[] toArray() {
        try {
            return delegate.toArray();
        }
        finally {
            close();
        }
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        try {
            return delegate.toArray(generator);
        }
        finally {
            close();
        }
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        try {
            return delegate.reduce(identity, accumulator);
        }
        finally {
            close();
        }
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        try {
            return delegate.reduce(accumulator);
        }
        finally {
            close();
        }
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        try {
            return delegate.reduce(identity, accumulator, combiner);
        }
        finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        try {
            return delegate.collect(supplier, accumulator, combiner);
        }
        finally {
            close();
        }
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        try {
            return delegate.collect(collector);
        }
        finally {
            close();
        }
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        try {
            return delegate.min(comparator);
        }
        finally {
            close();
        }
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        try {
            return delegate.max(comparator);
        }
        finally {
            close();
        }
    }

    @Override
    public long count() {
        try {
            return delegate.count();
        }
        finally {
            close();
        }
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        try {
            return delegate.anyMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        try {
            return delegate.allMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        try {
            return delegate.noneMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public Optional<T> findFirst() {
        try {
            return delegate.findFirst();
        }
        finally {
            close();
        }
    }

    @Override
    public Optional<T> findAny() {
        try {
            return delegate.findAny();
        }
        finally {
            close();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    @Override
    public Stream<T> sequential() {
        return new SelfClosingStream<>(delegate.sequential());
    }

    @Override
    public Stream<T> parallel() {
        return new SelfClosingStream<>(delegate.parallel());
    }

    @Override
    public Stream<T> unordered() {
        return new SelfClosingStream<>(delegate.unordered());
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        return new SelfClosingStream<>(delegate.onClose(closeHandler));
    }

    @Override
    public void close() {
        delegate.close();
    }
}
