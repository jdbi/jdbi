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

/**
 * A consumer with 8 arguments.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface Consumer8<T1, T2, T3, T4, T5, T6, T7, T8> {

    /**
     * Performs this operation on the given argument.
     *
     * @param args The arguments as a tuple.
     */
    default void accept(Tuple8<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8> args) {
        accept(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8);
    }

    /**
     * Performs this operation on the given argument.
     */
    void accept(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8);

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer7<T2, T3, T4, T5, T6, T7, T8> acceptPartially(T1 v1) {
        return (v2, v3, v4, v5, v6, v7, v8) -> accept(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer6<T3, T4, T5, T6, T7, T8> acceptPartially(T1 v1, T2 v2) {
        return (v3, v4, v5, v6, v7, v8) -> accept(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer5<T4, T5, T6, T7, T8> acceptPartially(T1 v1, T2 v2, T3 v3) {
        return (v4, v5, v6, v7, v8) -> accept(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer4<T5, T6, T7, T8> acceptPartially(T1 v1, T2 v2, T3 v3, T4 v4) {
        return (v5, v6, v7, v8) -> accept(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer3<T6, T7, T8> acceptPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5) {
        return (v6, v7, v8) -> accept(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer2<T7, T8> acceptPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6) {
        return (v7, v8) -> accept(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer1<T8> acceptPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7) {
        return (v8) -> accept(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer0 acceptPartially(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8) {
        return () -> accept(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer7<T2, T3, T4, T5, T6, T7, T8> acceptPartially(Tuple1<? extends T1> args) {
        return (v2, v3, v4, v5, v6, v7, v8) -> accept(args.v1, v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer6<T3, T4, T5, T6, T7, T8> acceptPartially(Tuple2<? extends T1, ? extends T2> args) {
        return (v3, v4, v5, v6, v7, v8) -> accept(args.v1, args.v2, v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer5<T4, T5, T6, T7, T8> acceptPartially(Tuple3<? extends T1, ? extends T2, ? extends T3> args) {
        return (v4, v5, v6, v7, v8) -> accept(args.v1, args.v2, args.v3, v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer4<T5, T6, T7, T8> acceptPartially(Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> args) {
        return (v5, v6, v7, v8) -> accept(args.v1, args.v2, args.v3, args.v4, v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer3<T6, T7, T8> acceptPartially(Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5> args) {
        return (v6, v7, v8) -> accept(args.v1, args.v2, args.v3, args.v4, args.v5, v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer2<T7, T8> acceptPartially(Tuple6<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6> args) {
        return (v7, v8) -> accept(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer1<T8> acceptPartially(Tuple7<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7> args) {
        return (v8) -> accept(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, v8);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer0 acceptPartially(Tuple8<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5, ? extends T6, ? extends T7, ? extends T8> args) {
        return () -> accept(args.v1, args.v2, args.v3, args.v4, args.v5, args.v6, args.v7, args.v8);
    }
}
