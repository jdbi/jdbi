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
import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Seq;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function1;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function7;

/**
 * A tuple of degree 7.
 *
 * @author Lukas Eder
 */
public class Tuple7<T1, T2, T3, T4, T5, T6, T7> implements Tuple, Comparable<Tuple7<T1, T2, T3, T4, T5, T6, T7>>, Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    public final T1 v1;
    public final T2 v2;
    public final T3 v3;
    public final T4 v4;
    public final T5 v5;
    public final T6 v6;
    public final T7 v7;

    public T1 v1() {
        return v1;
    }

    public T2 v2() {
        return v2;
    }

    public T3 v3() {
        return v3;
    }

    public T4 v4() {
        return v4;
    }

    public T5 v5() {
        return v5;
    }

    public T6 v6() {
        return v6;
    }

    public T7 v7() {
        return v7;
    }

    public Tuple7(Tuple7<T1, T2, T3, T4, T5, T6, T7> tuple) {
        this.v1 = tuple.v1;
        this.v2 = tuple.v2;
        this.v3 = tuple.v3;
        this.v4 = tuple.v4;
        this.v5 = tuple.v5;
        this.v6 = tuple.v6;
        this.v7 = tuple.v7;
    }

    public Tuple7(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.v5 = v5;
        this.v6 = v6;
        this.v7 = v7;
    }

    /**
     * Concatenate a value to this tuple.
     */
    public final <T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(T8 value) {
        return new Tuple8<>(v1, v2, v3, v4, v5, v6, v7, value);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(Tuple1<T8> tuple) {
        return new Tuple8<>(v1, v2, v3, v4, v5, v6, v7, tuple.v1);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(Tuple2<T8, T9> tuple) {
        return new Tuple9<>(v1, v2, v3, v4, v5, v6, v7, tuple.v1, tuple.v2);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T8, T9, T10> Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> concat(Tuple3<T8, T9, T10> tuple) {
        return new Tuple10<>(v1, v2, v3, v4, v5, v6, v7, tuple.v1, tuple.v2, tuple.v3);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T8, T9, T10, T11> Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> concat(Tuple4<T8, T9, T10, T11> tuple) {
        return new Tuple11<>(v1, v2, v3, v4, v5, v6, v7, tuple.v1, tuple.v2, tuple.v3, tuple.v4);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T8, T9, T10, T11, T12> Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> concat(Tuple5<T8, T9, T10, T11, T12> tuple) {
        return new Tuple12<>(v1, v2, v3, v4, v5, v6, v7, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T8, T9, T10, T11, T12, T13> Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> concat(Tuple6<T8, T9, T10, T11, T12, T13> tuple) {
        return new Tuple13<>(v1, v2, v3, v4, v5, v6, v7, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T8, T9, T10, T11, T12, T13, T14> Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> concat(Tuple7<T8, T9, T10, T11, T12, T13, T14> tuple) {
        return new Tuple14<>(v1, v2, v3, v4, v5, v6, v7, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T8, T9, T10, T11, T12, T13, T14, T15> Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> concat(Tuple8<T8, T9, T10, T11, T12, T13, T14, T15> tuple) {
        return new Tuple15<>(v1, v2, v3, v4, v5, v6, v7, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7, tuple.v8);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T8, T9, T10, T11, T12, T13, T14, T15, T16> Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> concat(Tuple9<T8, T9, T10, T11, T12, T13, T14, T15, T16> tuple) {
        return new Tuple16<>(v1, v2, v3, v4, v5, v6, v7, tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, tuple.v7, tuple.v8, tuple.v9);
    }

    /**
     * Split this tuple into two tuples of degree 0 and 7.
     */
    public final Tuple2<Tuple0, Tuple7<T1, T2, T3, T4, T5, T6, T7>> split0() {
        return new Tuple2<>(limit0(), skip0());
    }

    /**
     * Split this tuple into two tuples of degree 1 and 6.
     */
    public final Tuple2<Tuple1<T1>, Tuple6<T2, T3, T4, T5, T6, T7>> split1() {
        return new Tuple2<>(limit1(), skip1());
    }

    /**
     * Split this tuple into two tuples of degree 2 and 5.
     */
    public final Tuple2<Tuple2<T1, T2>, Tuple5<T3, T4, T5, T6, T7>> split2() {
        return new Tuple2<>(limit2(), skip2());
    }

    /**
     * Split this tuple into two tuples of degree 3 and 4.
     */
    public final Tuple2<Tuple3<T1, T2, T3>, Tuple4<T4, T5, T6, T7>> split3() {
        return new Tuple2<>(limit3(), skip3());
    }

    /**
     * Split this tuple into two tuples of degree 4 and 3.
     */
    public final Tuple2<Tuple4<T1, T2, T3, T4>, Tuple3<T5, T6, T7>> split4() {
        return new Tuple2<>(limit4(), skip4());
    }

    /**
     * Split this tuple into two tuples of degree 5 and 2.
     */
    public final Tuple2<Tuple5<T1, T2, T3, T4, T5>, Tuple2<T6, T7>> split5() {
        return new Tuple2<>(limit5(), skip5());
    }

    /**
     * Split this tuple into two tuples of degree 6 and 1.
     */
    public final Tuple2<Tuple6<T1, T2, T3, T4, T5, T6>, Tuple1<T7>> split6() {
        return new Tuple2<>(limit6(), skip6());
    }

    /**
     * Split this tuple into two tuples of degree 7 and 0.
     */
    public final Tuple2<Tuple7<T1, T2, T3, T4, T5, T6, T7>, Tuple0> split7() {
        return new Tuple2<>(limit7(), skip7());
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
        return new Tuple2<>(v1, v2);
    }

    /**
     * Limit this tuple to degree 3.
     */
    public final Tuple3<T1, T2, T3> limit3() {
        return new Tuple3<>(v1, v2, v3);
    }

    /**
     * Limit this tuple to degree 4.
     */
    public final Tuple4<T1, T2, T3, T4> limit4() {
        return new Tuple4<>(v1, v2, v3, v4);
    }

    /**
     * Limit this tuple to degree 5.
     */
    public final Tuple5<T1, T2, T3, T4, T5> limit5() {
        return new Tuple5<>(v1, v2, v3, v4, v5);
    }

    /**
     * Limit this tuple to degree 6.
     */
    public final Tuple6<T1, T2, T3, T4, T5, T6> limit6() {
        return new Tuple6<>(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Limit this tuple to degree 7.
     */
    public final Tuple7<T1, T2, T3, T4, T5, T6, T7> limit7() {
        return this;
    }

    /**
     * Skip 0 degrees from this tuple.
     */
    public final Tuple7<T1, T2, T3, T4, T5, T6, T7> skip0() {
        return this;
    }

    /**
     * Skip 1 degrees from this tuple.
     */
    public final Tuple6<T2, T3, T4, T5, T6, T7> skip1() {
        return new Tuple6<>(v2, v3, v4, v5, v6, v7);
    }

    /**
     * Skip 2 degrees from this tuple.
     */
    public final Tuple5<T3, T4, T5, T6, T7> skip2() {
        return new Tuple5<>(v3, v4, v5, v6, v7);
    }

    /**
     * Skip 3 degrees from this tuple.
     */
    public final Tuple4<T4, T5, T6, T7> skip3() {
        return new Tuple4<>(v4, v5, v6, v7);
    }

    /**
     * Skip 4 degrees from this tuple.
     */
    public final Tuple3<T5, T6, T7> skip4() {
        return new Tuple3<>(v5, v6, v7);
    }

    /**
     * Skip 5 degrees from this tuple.
     */
    public final Tuple2<T6, T7> skip5() {
        return new Tuple2<>(v6, v7);
    }

    /**
     * Skip 6 degrees from this tuple.
     */
    public final Tuple1<T7> skip6() {
        return new Tuple1<>(v7);
    }

    /**
     * Skip 7 degrees from this tuple.
     */
    public final Tuple0 skip7() {
        return new Tuple0();
    }

    /**
     * Apply this tuple as arguments to a function.
     */
    public final <R> R map(Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> function) {
        return function.apply(v1, v2, v3, v4, v5, v6, v7);
    }

    /**
     * Apply attribute 1 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U1> Tuple7<U1, T2, T3, T4, T5, T6, T7> map1(Function<? super T1, ? extends U1> function) {
        return Tuple.tuple(function.apply(v1), v2, v3, v4, v5, v6, v7);
    }

    /**
     * Apply attribute 2 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U2> Tuple7<T1, U2, T3, T4, T5, T6, T7> map2(Function<? super T2, ? extends U2> function) {
        return Tuple.tuple(v1, function.apply(v2), v3, v4, v5, v6, v7);
    }

    /**
     * Apply attribute 3 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U3> Tuple7<T1, T2, U3, T4, T5, T6, T7> map3(Function<? super T3, ? extends U3> function) {
        return Tuple.tuple(v1, v2, function.apply(v3), v4, v5, v6, v7);
    }

    /**
     * Apply attribute 4 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U4> Tuple7<T1, T2, T3, U4, T5, T6, T7> map4(Function<? super T4, ? extends U4> function) {
        return Tuple.tuple(v1, v2, v3, function.apply(v4), v5, v6, v7);
    }

    /**
     * Apply attribute 5 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U5> Tuple7<T1, T2, T3, T4, U5, T6, T7> map5(Function<? super T5, ? extends U5> function) {
        return Tuple.tuple(v1, v2, v3, v4, function.apply(v5), v6, v7);
    }

    /**
     * Apply attribute 6 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U6> Tuple7<T1, T2, T3, T4, T5, U6, T7> map6(Function<? super T6, ? extends U6> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, function.apply(v6), v7);
    }

    /**
     * Apply attribute 7 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U7> Tuple7<T1, T2, T3, T4, T5, T6, U7> map7(Function<? super T7, ? extends U7> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, v6, function.apply(v7));
    }

    @Override
    @Deprecated
    public final Object[] array() {
        return toArray();
    }

    @Override
    public final Object[] toArray() {
        return new Object[] { v1, v2, v3, v4, v5, v6, v7 };
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
        Supplier<? extends K> keySupplier2,
        Supplier<? extends K> keySupplier3,
        Supplier<? extends K> keySupplier4,
        Supplier<? extends K> keySupplier5,
        Supplier<? extends K> keySupplier6,
        Supplier<? extends K> keySupplier7
    ) {
        Map<K, Object> result = new LinkedHashMap<>();

        result.put(keySupplier1.get(), v1);
        result.put(keySupplier2.get(), v2);
        result.put(keySupplier3.get(), v3);
        result.put(keySupplier4.get(), v4);
        result.put(keySupplier5.get(), v5);
        result.put(keySupplier6.get(), v6);
        result.put(keySupplier7.get(), v7);

        return result;
    }

    public final <K> Map<K, ?> toMap(
        K key1,
        K key2,
        K key3,
        K key4,
        K key5,
        K key6,
        K key7
    ) {
        Map<K, Object> result = new LinkedHashMap<>();

        result.put(key1, v1);
        result.put(key2, v2);
        result.put(key3, v3);
        result.put(key4, v4);
        result.put(key5, v5);
        result.put(key6, v6);
        result.put(key7, v7);

        return result;
    }

    /**
     * The degree of this tuple: 7.
     */
    @Override
    public final int degree() {
        return 7;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Iterator<Object> iterator() {
        return (Iterator<Object>) list().iterator();
    }

    @Override
    public int compareTo(Tuple7<T1, T2, T3, T4, T5, T6, T7> other) {
        int result = 0;

        result = Tuples.compare(v1, other.v1); if (result != 0) return result;
        result = Tuples.compare(v2, other.v2); if (result != 0) return result;
        result = Tuples.compare(v3, other.v3); if (result != 0) return result;
        result = Tuples.compare(v4, other.v4); if (result != 0) return result;
        result = Tuples.compare(v5, other.v5); if (result != 0) return result;
        result = Tuples.compare(v6, other.v6); if (result != 0) return result;
        result = Tuples.compare(v7, other.v7); if (result != 0) return result;

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Tuple7))
            return false;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Tuple7<T1, T2, T3, T4, T5, T6, T7> that = (Tuple7) o;

        if (!Objects.equals(v1, that.v1)) return false;
        if (!Objects.equals(v2, that.v2)) return false;
        if (!Objects.equals(v3, that.v3)) return false;
        if (!Objects.equals(v4, that.v4)) return false;
        if (!Objects.equals(v5, that.v5)) return false;
        if (!Objects.equals(v6, that.v6)) return false;
        if (!Objects.equals(v7, that.v7)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + ((v1 == null) ? 0 : v1.hashCode());
        result = prime * result + ((v2 == null) ? 0 : v2.hashCode());
        result = prime * result + ((v3 == null) ? 0 : v3.hashCode());
        result = prime * result + ((v4 == null) ? 0 : v4.hashCode());
        result = prime * result + ((v5 == null) ? 0 : v5.hashCode());
        result = prime * result + ((v6 == null) ? 0 : v6.hashCode());
        result = prime * result + ((v7 == null) ? 0 : v7.hashCode());

        return result;
    }

    @Override
    public String toString() {
        return "("
             +        v1
             + ", " + v2
             + ", " + v3
             + ", " + v4
             + ", " + v5
             + ", " + v6
             + ", " + v7
             + ")";
    }

    @Override
    public Tuple7<T1, T2, T3, T4, T5, T6, T7> clone() {
        return new Tuple7<>(this);
    }
}
