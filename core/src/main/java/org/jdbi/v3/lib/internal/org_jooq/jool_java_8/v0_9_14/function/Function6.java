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

/**
 * A function with 6 arguments.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface Function6<T1, T2, T3, T4, T5, T6, R> {

    /**
     * Apply this function to the arguments.
     *
     * @param args The arguments as a tuple.
     */
    default R apply(Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6> args) {
        return apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6);
    }

    /**
     * Apply this function to the arguments.
     */
    R apply(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6);

    /**
     * Partially apply this function to the arguments.
     */
    default Function5<T2, T3, T4, T5, T6, R> applyPartially(T1 v1) {
        return (v2, v3, v4, v5, v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function4<T3, T4, T5, T6, R> applyPartially(T1 v1, T2 v2) {
        return (v3, v4, v5, v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function3<T4, T5, T6, R> applyPartially(T1 v1, T2 v2, T3 v3) {
        return (v4, v5, v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function2<T5, T6, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4) {
        return (v5, v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function1<T6, R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5) {
        return (v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function0<R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6) {
        return () -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function5<T2, T3, T4, T5, T6, R> applyPartially(Tuple1<? extends T1> args) {
        return (v2, v3, v4, v5, v6) -> apply(args.v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function4<T3, T4, T5, T6, R> applyPartially(Tuple2<? extends T1, ? extends T2> args) {
        return (v3, v4, v5, v6) -> apply(args.v1, args.v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function3<T4, T5, T6, R> applyPartially(Tuple3<? extends T1, ? extends T2, ? extends T3> args) {
        return (v4, v5, v6) -> apply(args.v1, args.v2, args.v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function2<T5, T6, R> applyPartially(Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> args) {
        return (v5, v6) -> apply(args.v1, args.v2, args.v3, args.v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function1<T6, R> applyPartially(Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5> args) {
        return (v6) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function0<R> applyPartially(Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6> args) {
        return () -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object)} instead.
     */
    @Deprecated
    default Function5<T2, T3, T4, T5, T6, R> curry(T1 v1) {
        return (v2, v3, v4, v5, v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object)} instead.
     */
    @Deprecated
    default Function4<T3, T4, T5, T6, R> curry(T1 v1, T2 v2) {
        return (v3, v4, v5, v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object)} instead.
     */
    @Deprecated
    default Function3<T4, T5, T6, R> curry(T1 v1, T2 v2, T3 v3) {
        return (v4, v5, v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function2<T5, T6, R> curry(T1 v1, T2 v2, T3 v3, T4 v4) {
        return (v5, v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function1<T6, R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5) {
        return (v6) -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function0<R> curry(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6) {
        return () -> apply(v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple1)} instead.
     */
    @Deprecated
    default Function5<T2, T3, T4, T5, T6, R> curry(Tuple1<? extends T1> args) {
        return (v2, v3, v4, v5, v6) -> apply(args.v1, v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple2)} instead.
     */
    @Deprecated
    default Function4<T3, T4, T5, T6, R> curry(Tuple2<? extends T1, ? extends T2> args) {
        return (v3, v4, v5, v6) -> apply(args.v1, args.v2, v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple3)} instead.
     */
    @Deprecated
    default Function3<T4, T5, T6, R> curry(Tuple3<? extends T1, ? extends T2, ? extends T3> args) {
        return (v4, v5, v6) -> apply(args.v1, args.v2, args.v3, v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple4)} instead.
     */
    @Deprecated
    default Function2<T5, T6, R> curry(Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> args) {
        return (v5, v6) -> apply(args.v1, args.v2, args.v3, args.v4, v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple5)} instead.
     */
    @Deprecated
    default Function1<T6, R> curry(Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5> args) {
        return (v6) -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, v6);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple6)} instead.
     */
    @Deprecated
    default Function0<R> curry(Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6> args) {
        return () -> apply(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6);
    }
}
