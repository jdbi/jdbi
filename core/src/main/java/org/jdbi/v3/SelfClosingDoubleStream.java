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

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class SelfClosingDoubleStream implements DoubleStream {
    private final DoubleStream delegate;

    SelfClosingDoubleStream(DoubleStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public DoubleStream filter(DoublePredicate predicate) {
        return new SelfClosingDoubleStream(delegate.filter(predicate));
    }

    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
        return new SelfClosingDoubleStream(delegate.map(mapper));
    }

    @Override
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        return new SelfClosingStream<>(delegate.mapToObj(mapper));
    }

    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
        return new SelfClosingIntStream(delegate.mapToInt(mapper));
    }

    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
        return new SelfClosingLongStream(delegate.mapToLong(mapper));
    }

    @Override
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        return new SelfClosingDoubleStream(delegate.flatMap(mapper));
    }

    @Override
    public DoubleStream distinct() {
        return new SelfClosingDoubleStream(delegate.distinct());
    }

    @Override
    public DoubleStream sorted() {
        return new SelfClosingDoubleStream(delegate.sorted());
    }

    @Override
    public DoubleStream peek(DoubleConsumer action) {
        return new SelfClosingDoubleStream(delegate.peek(action));
    }

    @Override
    public DoubleStream limit(long maxSize) {
        return new SelfClosingDoubleStream(delegate.limit(maxSize));
    }

    @Override
    public DoubleStream skip(long n) {
        return new SelfClosingDoubleStream(delegate.skip(n));
    }

    @Override
    public void forEach(DoubleConsumer action) {
        try {
            delegate.forEach(action);
        }
        finally {
            close();
        }
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        try {
            delegate.forEachOrdered(action);
        }
        finally {
            close();
        }
    }

    @Override
    public double[] toArray() {
        try {
            return delegate.toArray();
        }
        finally {
            close();
        }
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        try {
            return delegate.reduce(identity, op);
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        try {
            return delegate.reduce(op);
        }
        finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        try {
            return delegate.collect(supplier, accumulator, combiner);
        }
        finally {
            close();
        }
    }

    @Override
    public double sum() {
        try {
            return delegate.sum();
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalDouble min() {
        try {
            return delegate.min();
        }
        finally {
            close();
        }

    }

    @Override
    public OptionalDouble max() {
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
    public DoubleSummaryStatistics summaryStatistics() {
        try {
            return delegate.summaryStatistics();
        }
        finally {
            close();
        }
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        try {
            return delegate.anyMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        try {
            return delegate.allMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        try {
            return delegate.noneMatch(predicate);
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalDouble findFirst() {
        try {
            return delegate.findFirst();
        }
        finally {
            close();
        }
    }

    @Override
    public OptionalDouble findAny() {
        try {
            return delegate.findAny();
        }
        finally {
            close();
        }
    }

    @Override
    public Stream<Double> boxed() {
        return new SelfClosingStream<>(delegate.boxed());
    }

    @Override
    public DoubleStream sequential() {
        return new SelfClosingDoubleStream(delegate.sequential());
    }

    @Override
    public DoubleStream parallel() {
        return new SelfClosingDoubleStream(delegate.parallel());
    }

    @Override
    public DoubleStream unordered() {
        return new SelfClosingDoubleStream(delegate.unordered());
    }

    @Override
    public DoubleStream onClose(Runnable closeHandler) {
        return new SelfClosingDoubleStream(delegate.onClose(closeHandler));
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public PrimitiveIterator.OfDouble iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator.OfDouble spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }
}
