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
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function.Function14;

/**
 * A tuple of degree 14.
 *
 * @author Lukas Eder
 */
public class Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> implements Tuple, Comparable<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>>, Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    public final T1 v1;
    public final T2 v2;
    public final T3 v3;
    public final T4 v4;
    public final T5 v5;
    public final T6 v6;
    public final T7 v7;
    public final T8 v8;
    public final T9 v9;
    public final T10 v10;
    public final T11 v11;
    public final T12 v12;
    public final T13 v13;
    public final T14 v14;

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

    public T8 v8() {
        return v8;
    }

    public T9 v9() {
        return v9;
    }

    public T10 v10() {
        return v10;
    }

    public T11 v11() {
        return v11;
    }

    public T12 v12() {
        return v12;
    }

    public T13 v13() {
        return v13;
    }

    public T14 v14() {
        return v14;
    }

    public Tuple14(Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> tuple) {
        this.v1 = tuple.v1;
        this.v2 = tuple.v2;
        this.v3 = tuple.v3;
        this.v4 = tuple.v4;
        this.v5 = tuple.v5;
        this.v6 = tuple.v6;
        this.v7 = tuple.v7;
        this.v8 = tuple.v8;
        this.v9 = tuple.v9;
        this.v10 = tuple.v10;
        this.v11 = tuple.v11;
        this.v12 = tuple.v12;
        this.v13 = tuple.v13;
        this.v14 = tuple.v14;
    }

    public Tuple14(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.v5 = v5;
        this.v6 = v6;
        this.v7 = v7;
        this.v8 = v8;
        this.v9 = v9;
        this.v10 = v10;
        this.v11 = v11;
        this.v12 = v12;
        this.v13 = v13;
        this.v14 = v14;
    }

    /**
     * Concatenate a value to this tuple.
     */
    public final <T15> Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> concat(T15 value) {
        return new Tuple15<>(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, value);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T15> Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> concat(Tuple1<T15> tuple) {
        return new Tuple15<>(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, tuple.v1);
    }

    /**
     * Concatenate a tuple to this tuple.
     */
    public final <T15, T16> Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> concat(Tuple2<T15, T16> tuple) {
        return new Tuple16<>(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, tuple.v1, tuple.v2);
    }

    /**
     * Split this tuple into two tuples of degree 0 and 14.
     */
    public final Tuple2<Tuple0, Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> split0() {
        return new Tuple2<>(limit0(), skip0());
    }

    /**
     * Split this tuple into two tuples of degree 1 and 13.
     */
    public final Tuple2<Tuple1<T1>, Tuple13<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> split1() {
        return new Tuple2<>(limit1(), skip1());
    }

    /**
     * Split this tuple into two tuples of degree 2 and 12.
     */
    public final Tuple2<Tuple2<T1, T2>, Tuple12<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> split2() {
        return new Tuple2<>(limit2(), skip2());
    }

    /**
     * Split this tuple into two tuples of degree 3 and 11.
     */
    public final Tuple2<Tuple3<T1, T2, T3>, Tuple11<T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> split3() {
        return new Tuple2<>(limit3(), skip3());
    }

    /**
     * Split this tuple into two tuples of degree 4 and 10.
     */
    public final Tuple2<Tuple4<T1, T2, T3, T4>, Tuple10<T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> split4() {
        return new Tuple2<>(limit4(), skip4());
    }

    /**
     * Split this tuple into two tuples of degree 5 and 9.
     */
    public final Tuple2<Tuple5<T1, T2, T3, T4, T5>, Tuple9<T6, T7, T8, T9, T10, T11, T12, T13, T14>> split5() {
        return new Tuple2<>(limit5(), skip5());
    }

    /**
     * Split this tuple into two tuples of degree 6 and 8.
     */
    public final Tuple2<Tuple6<T1, T2, T3, T4, T5, T6>, Tuple8<T7, T8, T9, T10, T11, T12, T13, T14>> split6() {
        return new Tuple2<>(limit6(), skip6());
    }

    /**
     * Split this tuple into two tuples of degree 7 and 7.
     */
    public final Tuple2<Tuple7<T1, T2, T3, T4, T5, T6, T7>, Tuple7<T8, T9, T10, T11, T12, T13, T14>> split7() {
        return new Tuple2<>(limit7(), skip7());
    }

    /**
     * Split this tuple into two tuples of degree 8 and 6.
     */
    public final Tuple2<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, Tuple6<T9, T10, T11, T12, T13, T14>> split8() {
        return new Tuple2<>(limit8(), skip8());
    }

    /**
     * Split this tuple into two tuples of degree 9 and 5.
     */
    public final Tuple2<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>, Tuple5<T10, T11, T12, T13, T14>> split9() {
        return new Tuple2<>(limit9(), skip9());
    }

    /**
     * Split this tuple into two tuples of degree 10 and 4.
     */
    public final Tuple2<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>, Tuple4<T11, T12, T13, T14>> split10() {
        return new Tuple2<>(limit10(), skip10());
    }

    /**
     * Split this tuple into two tuples of degree 11 and 3.
     */
    public final Tuple2<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>, Tuple3<T12, T13, T14>> split11() {
        return new Tuple2<>(limit11(), skip11());
    }

    /**
     * Split this tuple into two tuples of degree 12 and 2.
     */
    public final Tuple2<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>, Tuple2<T13, T14>> split12() {
        return new Tuple2<>(limit12(), skip12());
    }

    /**
     * Split this tuple into two tuples of degree 13 and 1.
     */
    public final Tuple2<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>, Tuple1<T14>> split13() {
        return new Tuple2<>(limit13(), skip13());
    }

    /**
     * Split this tuple into two tuples of degree 14 and 0.
     */
    public final Tuple2<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>, Tuple0> split14() {
        return new Tuple2<>(limit14(), skip14());
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
        return new Tuple7<>(v1, v2, v3, v4, v5, v6, v7);
    }

    /**
     * Limit this tuple to degree 8.
     */
    public final Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> limit8() {
        return new Tuple8<>(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Limit this tuple to degree 9.
     */
    public final Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> limit9() {
        return new Tuple9<>(v1, v2, v3, v4, v5, v6, v7, v8, v9);
    }

    /**
     * Limit this tuple to degree 10.
     */
    public final Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> limit10() {
        return new Tuple10<>(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10);
    }

    /**
     * Limit this tuple to degree 11.
     */
    public final Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> limit11() {
        return new Tuple11<>(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11);
    }

    /**
     * Limit this tuple to degree 12.
     */
    public final Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> limit12() {
        return new Tuple12<>(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);
    }

    /**
     * Limit this tuple to degree 13.
     */
    public final Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> limit13() {
        return new Tuple13<>(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13);
    }

    /**
     * Limit this tuple to degree 14.
     */
    public final Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> limit14() {
        return this;
    }

    /**
     * Skip 0 degrees from this tuple.
     */
    public final Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> skip0() {
        return this;
    }

    /**
     * Skip 1 degrees from this tuple.
     */
    public final Tuple13<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> skip1() {
        return new Tuple13<>(v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Skip 2 degrees from this tuple.
     */
    public final Tuple12<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> skip2() {
        return new Tuple12<>(v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Skip 3 degrees from this tuple.
     */
    public final Tuple11<T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> skip3() {
        return new Tuple11<>(v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Skip 4 degrees from this tuple.
     */
    public final Tuple10<T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> skip4() {
        return new Tuple10<>(v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Skip 5 degrees from this tuple.
     */
    public final Tuple9<T6, T7, T8, T9, T10, T11, T12, T13, T14> skip5() {
        return new Tuple9<>(v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Skip 6 degrees from this tuple.
     */
    public final Tuple8<T7, T8, T9, T10, T11, T12, T13, T14> skip6() {
        return new Tuple8<>(v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Skip 7 degrees from this tuple.
     */
    public final Tuple7<T8, T9, T10, T11, T12, T13, T14> skip7() {
        return new Tuple7<>(v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Skip 8 degrees from this tuple.
     */
    public final Tuple6<T9, T10, T11, T12, T13, T14> skip8() {
        return new Tuple6<>(v9, v10, v11, v12, v13, v14);
    }

    /**
     * Skip 9 degrees from this tuple.
     */
    public final Tuple5<T10, T11, T12, T13, T14> skip9() {
        return new Tuple5<>(v10, v11, v12, v13, v14);
    }

    /**
     * Skip 10 degrees from this tuple.
     */
    public final Tuple4<T11, T12, T13, T14> skip10() {
        return new Tuple4<>(v11, v12, v13, v14);
    }

    /**
     * Skip 11 degrees from this tuple.
     */
    public final Tuple3<T12, T13, T14> skip11() {
        return new Tuple3<>(v12, v13, v14);
    }

    /**
     * Skip 12 degrees from this tuple.
     */
    public final Tuple2<T13, T14> skip12() {
        return new Tuple2<>(v13, v14);
    }

    /**
     * Skip 13 degrees from this tuple.
     */
    public final Tuple1<T14> skip13() {
        return new Tuple1<>(v14);
    }

    /**
     * Skip 14 degrees from this tuple.
     */
    public final Tuple0 skip14() {
        return new Tuple0();
    }

    /**
     * Apply this tuple as arguments to a function.
     */
    public final <R> R map(Function14<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? super T13, ? super T14, ? extends R> function) {
        return function.apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 1 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U1> Tuple14<U1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> map1(Function<? super T1, ? extends U1> function) {
        return Tuple.tuple(function.apply(v1), v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 2 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U2> Tuple14<T1, U2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> map2(Function<? super T2, ? extends U2> function) {
        return Tuple.tuple(v1, function.apply(v2), v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 3 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U3> Tuple14<T1, T2, U3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> map3(Function<? super T3, ? extends U3> function) {
        return Tuple.tuple(v1, v2, function.apply(v3), v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 4 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U4> Tuple14<T1, T2, T3, U4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> map4(Function<? super T4, ? extends U4> function) {
        return Tuple.tuple(v1, v2, v3, function.apply(v4), v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 5 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U5> Tuple14<T1, T2, T3, T4, U5, T6, T7, T8, T9, T10, T11, T12, T13, T14> map5(Function<? super T5, ? extends U5> function) {
        return Tuple.tuple(v1, v2, v3, v4, function.apply(v5), v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 6 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U6> Tuple14<T1, T2, T3, T4, T5, U6, T7, T8, T9, T10, T11, T12, T13, T14> map6(Function<? super T6, ? extends U6> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, function.apply(v6), v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 7 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U7> Tuple14<T1, T2, T3, T4, T5, T6, U7, T8, T9, T10, T11, T12, T13, T14> map7(Function<? super T7, ? extends U7> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, v6, function.apply(v7), v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 8 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U8> Tuple14<T1, T2, T3, T4, T5, T6, T7, U8, T9, T10, T11, T12, T13, T14> map8(Function<? super T8, ? extends U8> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, v6, v7, function.apply(v8), v9, v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 9 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U9> Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, U9, T10, T11, T12, T13, T14> map9(Function<? super T9, ? extends U9> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, v6, v7, v8, function.apply(v9), v10, v11, v12, v13, v14);
    }

    /**
     * Apply attribute 10 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U10> Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, U10, T11, T12, T13, T14> map10(Function<? super T10, ? extends U10> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, v6, v7, v8, v9, function.apply(v10), v11, v12, v13, v14);
    }

    /**
     * Apply attribute 11 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U11> Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, U11, T12, T13, T14> map11(Function<? super T11, ? extends U11> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, function.apply(v11), v12, v13, v14);
    }

    /**
     * Apply attribute 12 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U12> Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, U12, T13, T14> map12(Function<? super T12, ? extends U12> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, function.apply(v12), v13, v14);
    }

    /**
     * Apply attribute 13 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U13> Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, U13, T14> map13(Function<? super T13, ? extends U13> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, function.apply(v13), v14);
    }

    /**
     * Apply attribute 14 as argument to a function and return a new tuple with the substituted argument.
     */
    public final <U14> Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, U14> map14(Function<? super T14, ? extends U14> function) {
        return Tuple.tuple(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, function.apply(v14));
    }

    @Override
    @Deprecated
    public final Object[] array() {
        return toArray();
    }

    @Override
    public final Object[] toArray() {
        return new Object[] { v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14 };
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
        Supplier<? extends K> keySupplier7,
        Supplier<? extends K> keySupplier8,
        Supplier<? extends K> keySupplier9,
        Supplier<? extends K> keySupplier10,
        Supplier<? extends K> keySupplier11,
        Supplier<? extends K> keySupplier12,
        Supplier<? extends K> keySupplier13,
        Supplier<? extends K> keySupplier14
    ) {
        Map<K, Object> result = new LinkedHashMap<>();

        result.put(keySupplier1.get(), v1);
        result.put(keySupplier2.get(), v2);
        result.put(keySupplier3.get(), v3);
        result.put(keySupplier4.get(), v4);
        result.put(keySupplier5.get(), v5);
        result.put(keySupplier6.get(), v6);
        result.put(keySupplier7.get(), v7);
        result.put(keySupplier8.get(), v8);
        result.put(keySupplier9.get(), v9);
        result.put(keySupplier10.get(), v10);
        result.put(keySupplier11.get(), v11);
        result.put(keySupplier12.get(), v12);
        result.put(keySupplier13.get(), v13);
        result.put(keySupplier14.get(), v14);

        return result;
    }

    public final <K> Map<K, ?> toMap(
        K key1,
        K key2,
        K key3,
        K key4,
        K key5,
        K key6,
        K key7,
        K key8,
        K key9,
        K key10,
        K key11,
        K key12,
        K key13,
        K key14
    ) {
        Map<K, Object> result = new LinkedHashMap<>();

        result.put(key1, v1);
        result.put(key2, v2);
        result.put(key3, v3);
        result.put(key4, v4);
        result.put(key5, v5);
        result.put(key6, v6);
        result.put(key7, v7);
        result.put(key8, v8);
        result.put(key9, v9);
        result.put(key10, v10);
        result.put(key11, v11);
        result.put(key12, v12);
        result.put(key13, v13);
        result.put(key14, v14);

        return result;
    }

    /**
     * The degree of this tuple: 14.
     */
    @Override
    public final int degree() {
        return 14;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Iterator<Object> iterator() {
        return (Iterator<Object>) list().iterator();
    }

    @Override
    public int compareTo(Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> other) {
        int result = 0;

        result = Tuples.compare(v1, other.v1); if (result != 0) return result;
        result = Tuples.compare(v2, other.v2); if (result != 0) return result;
        result = Tuples.compare(v3, other.v3); if (result != 0) return result;
        result = Tuples.compare(v4, other.v4); if (result != 0) return result;
        result = Tuples.compare(v5, other.v5); if (result != 0) return result;
        result = Tuples.compare(v6, other.v6); if (result != 0) return result;
        result = Tuples.compare(v7, other.v7); if (result != 0) return result;
        result = Tuples.compare(v8, other.v8); if (result != 0) return result;
        result = Tuples.compare(v9, other.v9); if (result != 0) return result;
        result = Tuples.compare(v10, other.v10); if (result != 0) return result;
        result = Tuples.compare(v11, other.v11); if (result != 0) return result;
        result = Tuples.compare(v12, other.v12); if (result != 0) return result;
        result = Tuples.compare(v13, other.v13); if (result != 0) return result;
        result = Tuples.compare(v14, other.v14); if (result != 0) return result;

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Tuple14))
            return false;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> that = (Tuple14) o;

        if (!Objects.equals(v1, that.v1)) return false;
        if (!Objects.equals(v2, that.v2)) return false;
        if (!Objects.equals(v3, that.v3)) return false;
        if (!Objects.equals(v4, that.v4)) return false;
        if (!Objects.equals(v5, that.v5)) return false;
        if (!Objects.equals(v6, that.v6)) return false;
        if (!Objects.equals(v7, that.v7)) return false;
        if (!Objects.equals(v8, that.v8)) return false;
        if (!Objects.equals(v9, that.v9)) return false;
        if (!Objects.equals(v10, that.v10)) return false;
        if (!Objects.equals(v11, that.v11)) return false;
        if (!Objects.equals(v12, that.v12)) return false;
        if (!Objects.equals(v13, that.v13)) return false;
        if (!Objects.equals(v14, that.v14)) return false;

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
        result = prime * result + ((v8 == null) ? 0 : v8.hashCode());
        result = prime * result + ((v9 == null) ? 0 : v9.hashCode());
        result = prime * result + ((v10 == null) ? 0 : v10.hashCode());
        result = prime * result + ((v11 == null) ? 0 : v11.hashCode());
        result = prime * result + ((v12 == null) ? 0 : v12.hashCode());
        result = prime * result + ((v13 == null) ? 0 : v13.hashCode());
        result = prime * result + ((v14 == null) ? 0 : v14.hashCode());

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
             + ", " + v8
             + ", " + v9
             + ", " + v10
             + ", " + v11
             + ", " + v12
             + ", " + v13
             + ", " + v14
             + ")";
    }

    @Override
    public Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> clone() {
        return new Tuple14<>(this);
    }
}
