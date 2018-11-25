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
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function;


import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple1;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple2;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple3;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple4;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple5;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple6;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple7;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple8;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple9;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple10;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple11;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple12;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple13;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple14;

/**
 * A function with 14 arguments.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> {

    /**
     * Apply this function to the arguments.
     *
     * @param args The arguments as a tuple.
     */
    default R apply(Tuple14<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10, ? extends T11, ? extends T12, ? extends T13, ? extends T14> args) {
        return apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, args.v11, args.v12, args.v13, args.v14);
    }

    /**
     * Apply this function to the arguments.
     */
    R apply(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14);

    /**
     * Partially apply this function to the arguments.
     */
    default Function13<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(T1 v1) {
        return (v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function12<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(T1 v1, T2 v2) {
        return (v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function11<T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3) {
        return (v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function10<T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4) {
        return (v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function9<T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5) {
        return (v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function8<T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6) {
        return (v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function7<T8, T9, T10, T11, T12, T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7) {
        return (v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function6<T9, T10, T11, T12, T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8) {
        return (v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function5<T10, T11, T12, T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9) {
        return (v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function4<T11, T12, T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10) {
        return (v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function3<T12, T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11) {
        return (v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function2<T13, T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12) {
        return (v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function1<T14, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13) {
        return (v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function0<R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14) {
        return () -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function13<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(Tuple1<? extends T1> args) {
        return (v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function12<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(Tuple2<? extends T1, ? extends T2> args) {
        return (v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function11<T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(Tuple3<? extends T1, ? extends T2, ? extends T3> args) {
        return (v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function10<T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> args) {
        return (v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function9<T6, T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5> args) {
        return (v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function8<T7, T8, T9, T10, T11, T12, T13, T14, R> applyPartially(Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6> args) {
        return (v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function7<T8, T9, T10, T11, T12, T13, T14, R> applyPartially(Tuple7<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7> args) {
        return (v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function6<T9, T10, T11, T12, T13, T14, R> applyPartially(Tuple8<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8> args) {
        return (v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function5<T10, T11, T12, T13, T14, R> applyPartially(Tuple9<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9> args) {
        return (v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function4<T11, T12, T13, T14, R> applyPartially(Tuple10<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10> args) {
        return (v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function3<T12, T13, T14, R> applyPartially(Tuple11<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10, ? extends T11> args) {
        return (v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, args.v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function2<T13, T14, R> applyPartially(Tuple12<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10, ? extends T11, ? extends T12> args) {
        return (v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, args.v11, args.v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function1<T14, R> applyPartially(Tuple13<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10, ? extends T11, ? extends T12, ? extends T13> args) {
        return (v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, args.v11, args.v12, args.v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function0<R> applyPartially(Tuple14<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10, ? extends T11, ? extends T12, ? extends T13, ? extends T14> args) {
        return () -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, args.v11, args.v12, args.v13, args.v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object)} instead.
     */
    @Deprecated
    default Function13<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(T1 v1) {
        return (v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object)} instead.
     */
    @Deprecated
    default Function12<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(T1 v1, T2 v2) {
        return (v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object)} instead.
     */
    @Deprecated
    default Function11<T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(T1 v1, T2 v2, T3 v3) {
        return (v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function10<T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4) {
        return (v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function9<T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5) {
        return (v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function8<T7, T8, T9, T10, T11, T12, T13, T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6) {
        return (v7, v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function7<T8, T9, T10, T11, T12, T13, T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7) {
        return (v8, v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function6<T9, T10, T11, T12, T13, T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8) {
        return (v9, v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function5<T10, T11, T12, T13, T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9) {
        return (v10, v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function4<T11, T12, T13, T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10) {
        return (v11, v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function3<T12, T13, T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11) {
        return (v12, v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function2<T13, T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12) {
        return (v13, v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function1<T14, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13) {
        return (v14) -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function0<R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14) {
        return () -> apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple1)} instead.
     */
    @Deprecated
    default Function13<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(Tuple1<? extends T1> args) {
        return (v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple2)} instead.
     */
    @Deprecated
    default Function12<T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(Tuple2<? extends T1, ? extends T2> args) {
        return (v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple3)} instead.
     */
    @Deprecated
    default Function11<T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(Tuple3<? extends T1, ? extends T2, ? extends T3> args) {
        return (v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple4)} instead.
     */
    @Deprecated
    default Function10<T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> args) {
        return (v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple5)} instead.
     */
    @Deprecated
    default Function9<T6, T7, T8, T9, T10, T11, T12, T13, T14, R> curry(Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5> args) {
        return (v6, v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple6)} instead.
     */
    @Deprecated
    default Function8<T7, T8, T9, T10, T11, T12, T13, T14, R> curry(Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6> args) {
        return (v7, v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple7)} instead.
     */
    @Deprecated
    default Function7<T8, T9, T10, T11, T12, T13, T14, R> curry(Tuple7<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7> args) {
        return (v8, v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple8)} instead.
     */
    @Deprecated
    default Function6<T9, T10, T11, T12, T13, T14, R> curry(Tuple8<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8> args) {
        return (v9, v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple9)} instead.
     */
    @Deprecated
    default Function5<T10, T11, T12, T13, T14, R> curry(Tuple9<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9> args) {
        return (v10, v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple10)} instead.
     */
    @Deprecated
    default Function4<T11, T12, T13, T14, R> curry(Tuple10<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10> args) {
        return (v11, v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple11)} instead.
     */
    @Deprecated
    default Function3<T12, T13, T14, R> curry(Tuple11<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10, ? extends T11> args) {
        return (v12, v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, args.v11, v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple12)} instead.
     */
    @Deprecated
    default Function2<T13, T14, R> curry(Tuple12<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10, ? extends T11, ? extends T12> args) {
        return (v13, v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, args.v11, args.v12, v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple13)} instead.
     */
    @Deprecated
    default Function1<T14, R> curry(Tuple13<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10, ? extends T11, ? extends T12, ? extends T13> args) {
        return (v14) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, args.v11, args.v12, args.v13, v14);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple14)} instead.
     */
    @Deprecated
    default Function0<R> curry(Tuple14<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8, ? extends T9, ? extends T10, ? extends T11, ? extends T12, ? extends T13, ? extends T14> args) {
        return () -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8, args.v9, args.v10, args.v11, args.v12, args.v13, args.v14);
    }
}
