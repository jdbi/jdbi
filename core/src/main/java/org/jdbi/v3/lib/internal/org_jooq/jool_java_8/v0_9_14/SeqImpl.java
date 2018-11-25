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

import static java.util.Comparator.naturalOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple;

/**
 * @author Lukas Eder
 */
class SeqImpl<T> implements Seq<T> {

    static final Object               NULL = new Object();

    private final Stream<? extends T> stream;
    private Object[]                  buffered;

    SeqImpl(Stream<? extends T> stream) {
        this.stream = stream.sequential();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<T> stream() {
        // This cast is safe as <T> in Stream<T> is effectively declaration-site
        // covariant.
        return (Stream<T>) (buffered == null ? stream : Stream.of(buffered));
    }

    @Override
    public Seq<T> filter(Predicate<? super T> predicate) {
        return Seq.seq(stream().filter(predicate));
    }

    @Override
    public <R> Seq<R> map(Function<? super T, ? extends R> mapper) {
        return Seq.seq(stream().map(mapper));
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return stream().mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return stream().mapToLong(mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return stream().mapToDouble(mapper);
    }

    @Override
    public <R> Seq<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return Seq.seq(stream().flatMap(mapper));
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return stream().flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return stream().flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return stream().flatMapToDouble(mapper);
    }

    @Override
    public Seq<T> distinct() {
        return Seq.seq(stream().distinct());
    }

    @Override
    public Seq<T> sorted() {
        return Seq.seq(stream().sorted());
    }

    @Override
    public Seq<T> sorted(Comparator<? super T> comparator) {
        return Seq.seq(stream().sorted(comparator));
    }

    @Override
    public Seq<T> peek(Consumer<? super T> action) {
        return Seq.seq(stream().peek(action));
    }

    @Override
    public Seq<T> limit(long maxSize) {
        return Seq.seq(stream().limit(maxSize));
    }

    @Override
    public Seq<T> skip(long n) {
        return Seq.seq(stream().skip(n));
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        stream().forEach(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        stream().forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return stream().toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return stream().toArray(generator);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        return stream().reduce(identity, accumulator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return stream().reduce(accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return stream().reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return stream().collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return stream().collect(collector);
    }

    @Override
    public long count() {
        return stream().count();
    }

    @Override
    public long count(Predicate<? super T> predicate) {
        return filter(predicate).count();
    }

    @Override
    public long countDistinct() {
        return collect(Agg.countDistinct());
    }

    @Override
    public long countDistinct(Predicate<? super T> predicate) {
        return filter(predicate).countDistinct();
    }

    @Override
    public <U> long countDistinctBy(Function<? super T, ? extends U> function) {
        return collect(Agg.countDistinctBy(function));
    }

    @Override
    public <U> long countDistinctBy(Function<? super T, ? extends U> function, Predicate<? super U> predicate) {
        return map(function).filter(predicate).countDistinct();
    }

    @Override
    public Optional<T> sum() {
        return collect(Agg.sum());
    }

    @Override
    public <U> Optional<U> sum(Function<? super T, ? extends U> function) {
        return collect(Agg.sum(function));
    }

    @Override
    public int sumInt(ToIntFunction<? super T> function) {
        return collect(Collectors.summingInt(function));
    }

    @Override
    public long sumLong(ToLongFunction<? super T> function) {
        return collect(Collectors.summingLong(function));
    }

    @Override
    public double sumDouble(ToDoubleFunction<? super T> function) {
        return collect(Collectors.summingDouble(function));
    }

    @Override
    public Optional<T> avg() {
        return collect(Agg.avg());
    }

    @Override
    public <U> Optional<U> avg(Function<? super T, ? extends U> function) {
        return collect(Agg.avg(function));
    }

    @Override
    public double avgInt(ToIntFunction<? super T> function) {
        return collect(Collectors.averagingInt(function));
    }

    @Override
    public double avgLong(ToLongFunction<? super T> function) {
        return collect(Collectors.averagingLong(function));
    }

    @Override
    public double avgDouble(ToDoubleFunction<? super T> function) {
        return collect(Collectors.averagingDouble(function));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> min() {
        return min((Comparator) naturalOrder());
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return stream().min(comparator);
    }

    @Override
    public <U extends Comparable<? super U>> Optional<U> min(Function<? super T, ? extends U> function) {
        return collect(Agg.min(function));
    }

    @Override
    public <U> Optional<U> min(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.min(function, comparator));
    }

    @Override
    public <U extends Comparable<? super U>> Optional<T> minBy(Function<? super T, ? extends U> function) {
        return collect(Agg.minBy(function));
    }

    @Override
    public <U> Optional<T> minBy(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.minBy(function, comparator));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Seq<T> minAll() {
        return minAll((Comparator) naturalOrder());
    }

    @Override
    public Seq<T> minAll(Comparator<? super T> comparator) {
        return collect(Agg.minAll(comparator));
    }

    @Override
    public <U extends Comparable<? super U>> Seq<U> minAll(Function<? super T, ? extends U> function) {
        return collect(Agg.minAll(function));
    }

    @Override
    public <U> Seq<U> minAll(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.minAll(function, comparator));
    }

    @Override
    public <U extends Comparable<? super U>> Seq<T> minAllBy(Function<? super T, ? extends U> function) {
        return collect(Agg.minAllBy(function));
    }

    @Override
    public <U> Seq<T> minAllBy(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.minAllBy(function, comparator));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> max() {
        return max((Comparator) naturalOrder());
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return stream().max(comparator);
    }

    @Override
    public <U extends Comparable<? super U>> Optional<U> max(Function<? super T, ? extends U> function) {
        return collect(Agg.max(function));
    }

    @Override
    public <U> Optional<U> max(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.max(function, comparator));
    }

    @Override
    public <U extends Comparable<? super U>> Optional<T> maxBy(Function<? super T, ? extends U> function) {
        return collect(Agg.maxBy(function));
    }

    @Override
    public <U> Optional<T> maxBy(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.maxBy(function, comparator));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Seq<T> maxAll() {
        return maxAll((Comparator) naturalOrder());
    }

    @Override
    public Seq<T> maxAll(Comparator<? super T> comparator) {
        return collect(Agg.maxAll(comparator));
    }

    @Override
    public <U extends Comparable<? super U>> Seq<U> maxAll(Function<? super T, ? extends U> function) {
        return collect(Agg.maxAll(function));
    }

    @Override
    public <U> Seq<U> maxAll(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.maxAll(function, comparator));
    }

    @Override
    public <U extends Comparable<? super U>> Seq<T> maxAllBy(Function<? super T, ? extends U> function) {
        return collect(Agg.maxAllBy(function));
    }

    @Override
    public <U> Seq<T> maxAllBy(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.maxAllBy(function, comparator));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> median() {
        return median((Comparator) naturalOrder());
    }

    @Override
    public Optional<T> median(Comparator<? super T> comparator) {
        return collect(Agg.median(comparator));
    }

    @Override
    public <U extends Comparable<? super U>> Optional<T> medianBy(Function<? super T, ? extends U> function) {
        return collect(Agg.medianBy(function));
    }

    @Override
    public <U> Optional<T> medianBy(Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.medianBy(function, comparator));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> percentile(double percentile) {
        return percentile(percentile, (Comparator) naturalOrder());
    }

    @Override
    public Optional<T> percentile(double percentile, Comparator<? super T> comparator) {
        return collect(Agg.percentile(percentile, comparator));
    }

    @Override
    public <U extends Comparable<? super U>> Optional<T> percentileBy(double percentile, Function<? super T, ? extends U> function) {
        return collect(Agg.percentileBy(percentile, function));
    }

    @Override
    public <U> Optional<T> percentileBy(double percentile, Function<? super T, ? extends U> function, Comparator<? super U> comparator) {
        return collect(Agg.percentileBy(percentile, function, comparator));
    }

    @Override
    public Optional<T> mode() {
        return collect(Agg.mode());
    }

    @Override
    public <U> Optional<T> modeBy(Function<? super T, ? extends U> function) {
        return collect(Agg.modeBy(function));
    }

    @Override
    public Seq<T> modeAll() {
        return collect(Agg.modeAll());
    }

    @Override
    public <U> Seq<T> modeAllBy(Function<? super T, ? extends U> function) {
        return collect(Agg.modeAllBy(function));
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return stream().anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return stream().allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return stream().noneMatch(predicate);
    }

    @Override
    public Optional<T> bitAnd() {
        return collect(Agg.bitAnd());
    }

    @Override
    public <U> Optional<U> bitAnd(Function<? super T, ? extends U> function) {
        return collect(Agg.bitAnd(function));
    }

    @Override
    public int bitAndInt(ToIntFunction<? super T> function) {
        return collect(Agg.bitAndInt(function));
    }

    @Override
    public long bitAndLong(ToLongFunction<? super T> function) {
        return collect(Agg.bitAndLong(function));
    }

    @Override
    public Optional<T> bitOr() {
        return collect(Agg.bitOr());
    }

    @Override
    public <U> Optional<U> bitOr(Function<? super T, ? extends U> function) {
        return collect(Agg.bitOr(function));
    }

    @Override
    public int bitOrInt(ToIntFunction<? super T> function) {
        return collect(Agg.bitOrInt(function));
    }

    @Override
    public long bitOrLong(ToLongFunction<? super T> function) {
        return collect(Agg.bitOrLong(function));
    }

    @Override
    public Optional<T> findFirst() {
        return stream().findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return stream().findAny();
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return stream().spliterator();
    }

    /**
     * Always returns false. Seq streams are always sequential and, as such,
     * doesn't support parallelization.
     *
     * @return false
     * @see <a href="https://github.com/jOOQ/jOOL/issues/130">jOOL Issue #130</a>
     */
    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public Seq<T> onClose(Runnable closeHandler) {
        return Seq.seq(stream.onClose(closeHandler));
    }

    @Override
    public void close() {
        stream.close();
    }

    @Override
    public List<T> toList() {
        return Seq.toList(this);
    }

    @Override
    public <L extends List<T>> L toList(Supplier<L> factory) {
        return Seq.toCollection(this, factory);
    }

    @Override
    public List<T> toUnmodifiableList() {
        return Collections.unmodifiableList(toList());
    }

    @Override
    public Set<T> toSet() {
        return Seq.toSet(this);
    }

    @Override
    public <S extends Set<T>> S toSet(Supplier<S> factory) {
        return Seq.toCollection(this, factory);
    }

    @Override
    public Set<T> toUnmodifiableSet() {
        return Collections.unmodifiableSet(toSet());
    }

    @Override
    public <C extends Collection<T>> C toCollection(Supplier<C> factory) {
        return Seq.toCollection(this, factory);
    }

    @Override
    public <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return Seq.toMap(this, keyMapper, valueMapper);
    }

    @Override
    public <K> Map<K, T> toMap(Function<? super T, ? extends K> keyMapper) {
        return toMap(keyMapper, Function.identity());
    }

    @Override
    public String toString() {
        buffered = toArray();
        return Seq.toString(stream());
    }

    @Override
    public String toString(CharSequence delimiter) {
        return Seq.toString(this, delimiter);
    }

    @Override
    public String toString(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return map(Objects::toString).collect(Collectors.joining(delimiter, prefix, suffix));
    }

    @Override
    public String commonPrefix() {
        return map(Objects::toString).collect(Agg.commonPrefix());
    }

    @Override
    public String commonSuffix() {
        return map(Objects::toString).collect(Agg.commonSuffix());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String format() {
        final List<String[]> strings = new ArrayList<>();
        Class<?>[] types0 = null;

        for (T t : this) {
            Object[] array = t instanceof Tuple
                           ? ((Tuple) t).toArray()
                           : new Object[] { t };

            if (types0 == null)
                types0 = new Class[array.length];

            for (int i = 0; i < array.length; i++)
                if (types0[i] == null && array[i] != null)
                    types0[i] = array[i].getClass();

            strings.add(Seq
                .of(array)
                .map(o ->
                     o instanceof Optional
                   ? ((Optional) o).map(Objects::toString).orElse("{empty}")
                   : Objects.toString(o))
                .toArray(String[]::new));
        }

        if (strings.isEmpty())
            return "(empty seq)";

        final Class<?>[] types = types0;
        final int length = types.length;
        final int[] maxLengths = new int[length];
        for (int s = 0; s < strings.size(); s++)
            for (int l = 0; l < length; l++)
                maxLengths[l] = Math.max(2, Math.max(maxLengths[l], strings.get(s)[l].length()));

        Function<String, String>[] pad = IntStream
            .range(0, length)
            .mapToObj(i -> (Function<String, String>) string -> {
                boolean number = Number.class.isAssignableFrom(types[i]);
                return Seq.seq(Collections.nCopies(maxLengths[i] - string.length(), " ")).toString("", number ? "" : string, number ? string : "");
            })
            .toArray(Function[]::new);

        StringBuilder separator = new StringBuilder("+-");
        for (int l = 0; l < length; l++) {
            if (l > 0)
                separator.append("-+-");

            for (int p = 0; p < maxLengths[l]; p++)
                separator.append('-');
        }
        separator.append("-+\n");

        StringBuilder result = new StringBuilder(separator).append("| ");
        for (int l = 0; l < length; l++) {
            String n = "v" + (l + 1);

            if (l > 0)
                result.append(" | ");

            result.append(pad[l].apply(n));
        }
        result.append(" |\n").append(separator);
        for (int s = 0; s < strings.size(); s++) {
            result.append("| ");

            for (int l = 0; l < length; l++) {
                String string = strings.get(s)[l];

                if (l > 0)
                    result.append(" | ");

                result.append(pad[l].apply(string));
            }

            result.append(" |\n");
        }

        return result.append(separator).toString();
    }
}
