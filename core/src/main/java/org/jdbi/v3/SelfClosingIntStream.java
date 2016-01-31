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

import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class SelfClosingIntStream implements IntStream {
    private final IntStream delegate;

    SelfClosingIntStream(IntStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public IntStream filter(IntPredicate predicate) {
        return new SelfClosingIntStream(delegate.filter(predicate));
    }

    @Override
    public IntStream map(IntUnaryOperator mapper) {
        return new SelfClosingIntStream(delegate.map(mapper));
    }

    @Override
    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        return new SelfClosingStream<>(delegate.mapToObj(mapper));
    }

    @Override
    public LongStream mapToLong(IntToLongFunction mapper) {
        return new SelfClosingLongStream(delegate.mapToLong(mapper));
    }

    @Override
    public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        return new SelfClosingDoubleStream(delegate.mapToDouble(mapper));
    }

    @Override
    public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
        return new SelfClosingIntStream(delegate.flatMap(mapper));
    }

    @Override
    public IntStream distinct() {
        return new SelfClosingIntStream(delegate.distinct());
    }

    @Override
    public IntStream sorted() {
        return new SelfClosingIntStream(delegate.sorted());
    }

    @Override
    public IntStream peek(IntConsumer action) {
        return new SelfClosingIntStream(delegate.peek(action));
    }

    @Override
    public IntStream limit(long maxSize) {
        return new SelfClosingIntStream(delegate.limit(maxSize));
    }

    @Override
    public IntStream skip(long n) {
        return new SelfClosingIntStream(delegate.skip(n));
    }

    @Override
    public void forEach(IntConsumer action) {
        try {
            delegate.forEach(action);
        }
        finally {
            close();
        }
    }

    @Override
    public void forEachOrdered(IntConsumer action) {
        try {
            delegate.forEachOrdered(action);
        }
        finally {
            close();
        }
    }

    @Override
    public int[] toArray() {
        try {
            return delegate.toArray();
        }
        finally {
            close();
        }
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        try {
            return delegate.reduce(identity, op);
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        try {
            return delegate.reduce(op);
        }
        finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        try {
            return delegate.collect(supplier, accumulator, combiner);
        }
        finally {
            close();
        }
    }

    @Override
    public int sum() {
        try {
            return delegate.sum();
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalInt min() {
        try {
            return delegate.min();
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalInt max() {
        try {
            return delegate.max();
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
    public OptionalDouble average() {
        try {
            return delegate.average();
        }
        finally {
            close();
        }
    }

    @Override
    public IntSummaryStatistics summaryStatistics() {
        try {
            return delegate.summaryStatistics();
        }
        finally {
            close();
        }
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
        try {
            return delegate.anyMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        try {
            return delegate.allMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public boolean noneMatch(IntPredicate predicate) {
        try {
            return delegate.noneMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalInt findFirst() {
        try {
            return delegate.findFirst();
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalInt findAny() {
        try {
            return delegate.findAny();
        }
        finally {
            close();
        }
    }

    @Override
    public LongStream asLongStream() {
        return new SelfClosingLongStream(delegate.asLongStream());
    }

    @Override
    public DoubleStream asDoubleStream() {
        return new SelfClosingDoubleStream(delegate.asDoubleStream());
    }

    @Override
    public Stream<Integer> boxed() {
        return new SelfClosingStream<>(delegate.boxed());
    }

    @Override
    public IntStream sequential() {
        return new SelfClosingIntStream(delegate.sequential());
    }

    @Override
    public IntStream parallel() {
        return new SelfClosingIntStream(delegate.parallel());
    }

    @Override
    public IntStream unordered() {
        return new SelfClosingIntStream(delegate.unordered());
    }

    @Override
    public IntStream onClose(Runnable closeHandler) {
        return new SelfClosingIntStream(delegate.onClose(closeHandler));
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator.OfInt spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }
}
