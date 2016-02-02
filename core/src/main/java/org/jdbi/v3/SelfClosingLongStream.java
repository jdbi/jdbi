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

import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class SelfClosingLongStream implements LongStream {
    private final LongStream delegate;

    SelfClosingLongStream(LongStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public LongStream filter(LongPredicate predicate) {
        return new SelfClosingLongStream(delegate.filter(predicate));
    }

    @Override
    public LongStream map(LongUnaryOperator mapper) {
        return new SelfClosingLongStream(delegate.map(mapper));
    }

    @Override
    public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        return new SelfClosingStream<>(delegate.mapToObj(mapper));
    }

    @Override
    public IntStream mapToInt(LongToIntFunction mapper) {
        return new SelfClosingIntStream(delegate.mapToInt(mapper));
    }

    @Override
    public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        return new SelfClosingDoubleStream(delegate.mapToDouble(mapper));
    }

    @Override
    public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        return new SelfClosingLongStream(delegate.flatMap(mapper));
    }

    @Override
    public LongStream distinct() {
        return new SelfClosingLongStream(delegate.distinct());
    }

    @Override
    public LongStream sorted() {
        return new SelfClosingLongStream(delegate.sorted());
    }

    @Override
    public LongStream peek(LongConsumer action) {
        return new SelfClosingLongStream(delegate.peek(action));
    }

    @Override
    public LongStream limit(long maxSize) {
        return new SelfClosingLongStream(delegate.limit(maxSize));
    }

    @Override
    public LongStream skip(long n) {
        return new SelfClosingLongStream(delegate.skip(n));
    }

    @Override
    public void forEach(LongConsumer action) {
        try {
            delegate.forEach(action);
        }
        finally {
            close();
        }
    }

    @Override
    public void forEachOrdered(LongConsumer action) {
        try {
            delegate.forEachOrdered(action);
        }
        finally {
            close();
        }
    }

    @Override
    public long[] toArray() {
        try {
            return delegate.toArray();
        }
        finally {
            close();
        }
    }

    @Override
    public long reduce(long identity, LongBinaryOperator op) {
        try {
            return delegate.reduce(identity, op);
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        try {
            return delegate.reduce(op);
        }
        finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        try {
            return delegate.collect(supplier, accumulator, combiner);
        }
        finally {
            close();
        }
    }

    @Override
    public long sum() {
        try {
            return delegate.sum();
        }
        finally {
            close();
        }

    }

    @Override
    public OptionalLong min() {
        try {
            return delegate.min();
        }
        finally {
            close();
        }

    }

    @Override
    public OptionalLong max() {
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
    public LongSummaryStatistics summaryStatistics() {
        try {
            return delegate.summaryStatistics();
        }
        finally {
            close();
        }
    }

    @Override
    public boolean anyMatch(LongPredicate predicate) {
        try {
            return delegate.anyMatch(predicate);
        }
        finally {
            close();
        }

    }

    @Override
    public boolean allMatch(LongPredicate predicate) {
        try {
            return delegate.allMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public boolean noneMatch(LongPredicate predicate) {
        try {
            return delegate.noneMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalLong findFirst() {
        try {
            return delegate.findFirst();
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalLong findAny() {
        try {
            return delegate.findAny();
        }
        finally {
            close();
        }
    }

    @Override
    public DoubleStream asDoubleStream() {
        return new SelfClosingDoubleStream(delegate.asDoubleStream());
    }

    @Override
    public Stream<Long> boxed() {
        return new SelfClosingStream<>(delegate.boxed());
    }

    @Override
    public LongStream sequential() {
        return new SelfClosingLongStream(delegate.sequential());
    }

    @Override
    public LongStream parallel() {
        return new SelfClosingLongStream(delegate.parallel());
    }

    @Override
    public LongStream unordered() {
        return new SelfClosingLongStream(delegate.unordered());
    }

    @Override
    public LongStream onClose(Runnable closeHandler) {
        return new SelfClosingLongStream(delegate.onClose(closeHandler));
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public PrimitiveIterator.OfLong iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator.OfLong spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }
}
