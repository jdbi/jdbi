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

/**
 * A consumer with 3 arguments.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface Consumer3<T1, T2, T3> {

    /**
     * Performs this operation on the given argument.
     *
     * @param args The arguments as a tuple.
     */
    default void accept(Tuple3<? extends T1, ? extends T2, ? extends T3> args) {
        accept(args.v1, args.v2, args.v3);
    }

    /**
     * Performs this operation on the given argument.
     */
    void accept(T1 v1, T2 v2, T3 v3);

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer2<T2, T3> acceptPartially(T1 v1) {
        return (v2, v3) -> accept(v1, v2, v3);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer1<T3> acceptPartially(T1 v1, T2 v2) {
        return (v3) -> accept(v1, v2, v3);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer0 acceptPartially(T1 v1, T2 v2, T3 v3) {
        return () -> accept(v1, v2, v3);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer2<T2, T3> acceptPartially(Tuple1<? extends T1> args) {
        return (v2, v3) -> accept(args.v1, v2, v3);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer1<T3> acceptPartially(Tuple2<? extends T1, ? extends T2> args) {
        return (v3) -> accept(args.v1, args.v2, v3);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer0 acceptPartially(Tuple3<? extends T1, ? extends T2, ? extends T3> args) {
        return () -> accept(args.v1, args.v2, args.v3);
    }
}
