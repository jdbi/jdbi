/**
 * Copyright (c) Data Geekery GmbH, contact@datageekery.com
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
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Seq;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function1;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function2;

/**
 * A tuple of degree 2.
 *
 * @author Lukas Eder
 */
public class Tuple2<T1, T2> implements Tuple, Comparable<Tuple2<T1, T2>>, Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    public final T1 v1;
    public final T2 v2;

    public T1 v1() {
        return v1;
    }

    public T2 v2() {
        return v2;
    }

    public Tuple2(Tuple2<T1, T2> tuple) {
        this.v1 = tuple.v1;
        this.v2 = tuple.v2;
    }

    public Tuple2(T1 v1, T2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    /**
     * Concatenate a value to this tuple.
     */
    public final <T3> Tuple3<T1, T2, T3> concat(T3 value) {
        return new Tuple3<>(v1, v2, value);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3> Tuple3<T1, T2, T3> concat(Tuple1<T3> tuple) {
        return new Tuple3<>(v1, v2, tuple.v1);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4> Tuple4<T1, T2, T3, T4> concat(Tuple2<T3, T4> tuple) {
        return new Tuple4<>(v1, v2, tuple.v1, tuple.v2);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> concat(Tuple3<T3, T4, T5> tuple) {
        return new Tuple5<>(v1, v2, tuple.v1, tuple.v2, tuple.v3);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(Tuple4<T3, T4, T5, T6> tuple) {
        return new Tuple6<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(Tuple5<T3, T4, T5, T6, T7> tuple) {
        return new Tuple7<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(Tuple6<T3, T4, T5, T6, T7, T8> tuple) {
        return new Tuple8<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(Tuple7<T3, T4, T5, T6, T7, T8, T9> tuple) {
        return new Tuple9<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7, T8, T9, T10> Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> concat(Tuple8<T3, T4, T5, T6, T7, T8, T9, T10> tuple) {
        return new Tuple10<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7, tuple.v8);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7, T8, T9, T10, T11> Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> concat(Tuple9<T3, T4, T5, T6, T7, T8, T9, T10, T11> tuple) {
        return new Tuple11<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7, tuple.v8, tuple.v9);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> concat(Tuple10<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> tuple) {
        return new Tuple12<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7, tuple.v8, tuple.v9, tuple.v10);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> concat(Tuple11<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> tuple) {
        return new Tuple13<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7, tuple.v8, tuple.v9, tuple.v10, tuple.v11);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> concat(Tuple12<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> tuple) {
        return new Tuple14<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7, tuple.v8, tuple.v9, tuple.v10, tuple.v11, tuple.v12);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> concat(Tuple13<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> tuple) {
        return new Tuple15<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7, tuple.v8, tuple.v9, tuple.v10, tuple.v11, tuple.v12, tuple.v13);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> concat(Tuple14<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> tuple) {
        return new Tuple16<>(v1, v2, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7, tuple.v8, tuple.v9, tuple.v10, tuple.v11, tuple.v12, tuple.v13, tuple.v14);
    }

    /**
     * Split this tuple into two tuples of degree 0 and 2.
     */
    public final Tuple2<Tuple0, Tuple2<T1, T2>> split0() {
        return new Tuple2<>(limit0(), skip0());
    }

    /**
     * Split this tuple into two tuples of degree 1 and 1.
     */
    public final Tuple2<Tuple1<T1>, Tuple1<T2>> split1() {
        return new Tuple2<>(limit1(), skip1());
    }

    /**
     * Split this tuple into two tuples of degree 2 and 0.
     */
    public final Tuple2<Tuple2<T1, T2>, Tuple0> split2() {
        return new Tuple2<>(limit2(), skip2());
    }

    /**
     * Limit this tuple to degree 0.
     */
    public final Tuple0 limit0() {
        return new Tuple0();
    }

    /**
     * Limit this tuple to degree 1.
     */
    public final Tuple1<T1> limit1() {
        return new Tuple1<>(v1);
    }

    /**
     * Limit this tuple to degree 2.
     */
    public final Tuple2<T1, T2> limit2() {
        return this;
    }

    /**
     * Skip 0 degrees from this tuple.
     */
    public final Tuple2<T1, T2> skip0() {
        return this;
    }

    /**
     * Skip 1 degrees from this tuple.
     */
    public final Tuple1<T2> skip1() {
        return new Tuple1<>(v2);
    }

    /**
     * Skip 2 degrees from this tuple.
     */
    public final Tuple0 skip2() {
        return new Tuple0();
    }

    /**
     * Get a tuple with the two attributes swapped.
     */
    public final Tuple2<T2, T1> swap() {
        return new Tuple2<>(v2, v1);
    }

    /**
     * Whether two tuples overlap.
     * <p>
     * <code><pre>
     * // true
     * range(1, 3).overlaps(range(2, 4))
     *
     * // false
     * range(1, 3).overlaps(range(5, 8))
     * </pre></code>
     */
    public static final <T extends Comparable<? super T>> boolean overlaps(Tuple2<T, T> left, Tuple2<T, T> right) {
        return left.v1.compareTo(right.v2) <= 0
            && left.v2.compareTo(right.v1) >= 0;
    }

    /**
     * The intersection of two ranges.
     * <p>
     * <code><pre>
     * // (2, 3)
     * range(1, 3).intersect(range(2, 4))
     *
     * // none
     * range(1, 3).intersect(range(5, 8))
     * </pre></code>
     */
    public static final <T extends Comparable<? super T>> Optional<Tuple2<T, T>> intersect(Tuple2<T, T> left, Tuple2<T, T> right) {
        if (overlaps(left, right))
            return Optional.of(new Tuple2<>(
                left.v1.compareTo(right.v1) >= 0 ? left.v1 : right.v1,
                left.v2.compareTo(right.v2) <= 0 ? left.v2 : right.v2
            ));
        else
            return Optional.empty();
    }

    /**
     * Apply this tuple as arguments to a function.
     */
    public final <R> R map(BiFunction<? super T1, ? super T2, ? extends R> function) {
        return function.apply(v1, v2);
    }

    /**
     * Apply attribute 1 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U1> Tuple2<U1, T2> map1(Function<? super T1, ? extends U1> function) {
        return Tuple.tuple(function.apply(v1), v2);
    }

    /**
     * Apply attribute 2 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U2> Tuple2<T1, U2> map2(Function<? super T2, ? extends U2> function) {
        return Tuple.tuple(v1, function.apply(v2));
    }

    @Override
    @Deprecated
    public final Object[] array() {
        return toArray();
    }

    @Override
    public final Object[] toArray() {
        return new Object[] { v1, v2 };
    }

    @Override
    @Deprecated
    public final List<?> list() {
        return toList();
    }

    @Override
    public final List<?> toList() {
        return Arrays.asList(toArray());
    }

    @Override
    public final Seq<?> toSeq() {
        return Seq.seq(toList());
    }

    @Override
    public final Map<String, ?> toMap() {
        return toMap(i -> "v" + (i + 1));
    }

    @Override
    public final <K> Map<K, ?> toMap(Function<? super Integer, ? extends K> keyMapper) {
        Map<K, Object> result = new LinkedHashMap<>();
        Object[] array = toArray();

        for (int i = 0; i < array.length; i++)
            result.put(keyMapper.apply(i), array[i]);

        return result;
    }

    public final <K> Map<K, ?> toMap(
        Supplier<? extends K> keySupplier1,
        Supplier<? extends K> keySupplier2
    ) {
        Map<K, Object> result = new LinkedHashMap<>();

        result.put(keySupplier1.get(), v1);
        result.put(keySupplier2.get(), v2);

        return result;
    }

    public final <K> Map<K, ?> toMap(
        K key1,
        K key2
    ) {
        Map<K, Object> result = new LinkedHashMap<>();

        result.put(key1, v1);
        result.put(key2, v2);

        return result;
    }

    /**
     * The degree of this tuple: 2.
     */
    @Override
    public final int degree() {
        return 2;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Iterator<Object> iterator() {
        return (Iterator<Object>) list().iterator();
    }

    @Override
    public int compareTo(Tuple2<T1, T2> other) {
        int result = 0;

        result = Tuples.compare(v1, other.v1); if (result != 0) return result;
        result = Tuples.compare(v2, other.v2); if (result != 0) return result;

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Tuple2))
            return false;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Tuple2<T1, T2> that = (Tuple2) o;

        if (!Objects.equals(v1, that.v1)) return false;
        if (!Objects.equals(v2, that.v2)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + ((v1 == null) ? 0 : v1.hashCode());
        result = prime * result + ((v2 == null) ? 0 : v2.hashCode());

        return result;
    }

    @Override
    public String toString() {
        return "("
             +        v1
             + ", " + v2
             + ")";
    }

    @Override
    public Tuple2<T1, T2> clone() {
        return new Tuple2<>(this);
    }
}
