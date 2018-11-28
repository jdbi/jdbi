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

/**
 * A function with 4 arguments.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface Function4<T1, T2, T3, T4, R> {

    /**
     * Apply this function to the arguments.
     *
     * @param args The arguments as a tuple.
     */
    default R apply(Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> args) {
        return apply(args.v1, args.v2, args.v3, args.v4);
    }

    /**
     * Apply this function to the arguments.
     */
    R apply(T1 v1, T2 v2, T3 v3, T4 v4);

    /**
     * Partially apply this function to the arguments.
     */
    default Function3<T2, T3, T4, R> applyPartially(T1 v1) {
        return (v2, v3, v4) -> apply(v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function2<T3, T4, R> applyPartially(T1 v1, T2 v2) {
        return (v3, v4) -> apply(v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function1<T4, R> applyPartially(T1 v1, T2 v2, T3 v3) {
        return (v4) -> apply(v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function0<R> applyPartially(T1 v1, T2 v2, T3 v3, T4 v4) {
        return () -> apply(v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function3<T2, T3, T4, R> applyPartially(Tuple1<? extends T1> args) {
        return (v2, v3, v4) -> apply(args.v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function2<T3, T4, R> applyPartially(Tuple2<? extends T1, ? extends T2> args) {
        return (v3, v4) -> apply(args.v1, args.v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function1<T4, R> applyPartially(Tuple3<? extends T1, ? extends T2, ? extends T3> args) {
        return (v4) -> apply(args.v1, args.v2, args.v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     */
    default Function0<R> applyPartially(Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> args) {
        return () -> apply(args.v1, args.v2, args.v3, args.v4);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object)} instead.
     */
    @Deprecated
    default Function3<T2, T3, T4, R> curry(T1 v1) {
        return (v2, v3, v4) -> apply(v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object)} instead.
     */
    @Deprecated
    default Function2<T3, T4, R> curry(T1 v1, T2 v2) {
        return (v3, v4) -> apply(v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object)} instead.
     */
    @Deprecated
    default Function1<T4, R> curry(T1 v1, T2 v2, T3 v3) {
        return (v4) -> apply(v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Object, Object, Object, Object)} instead.
     */
    @Deprecated
    default Function0<R> curry(T1 v1, T2 v2, T3 v3, T4 v4) {
        return () -> apply(v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple1)} instead.
     */
    @Deprecated
    default Function3<T2, T3, T4, R> curry(Tuple1<? extends T1> args) {
        return (v2, v3, v4) -> apply(args.v1, v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple2)} instead.
     */
    @Deprecated
    default Function2<T3, T4, R> curry(Tuple2<? extends T1, ? extends T2> args) {
        return (v3, v4) -> apply(args.v1, args.v2, v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple3)} instead.
     */
    @Deprecated
    default Function1<T4, R> curry(Tuple3<? extends T1, ? extends T2, ? extends T3> args) {
        return (v4) -> apply(args.v1, args.v2, args.v3, v4);
    }

    /**
     * Partially apply this function to the arguments.
     *
     * @deprecated - Use {@link #applyPartially(Tuple4)} instead.
     */
    @Deprecated
    default Function0<R> curry(Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> args) {
        return () -> apply(args.v1, args.v2, args.v3, args.v4);
    }
}
