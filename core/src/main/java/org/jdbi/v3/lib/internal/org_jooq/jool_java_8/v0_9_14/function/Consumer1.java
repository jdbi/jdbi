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

import java.util.function.Consumer;

import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple1;

/**
 * A consumer with 1 arguments.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface Consumer1<T1> extends Consumer<T1> {

    /**
     * Performs this operation on the given argument.
     *
     * @param args The arguments as a tuple.
     */
    default void accept(Tuple1<? extends T1> args) {
        accept(args.v1);
    }

    /**
     * Performs this operation on the given argument.
     */
    @Override
    void accept(T1 v1);

    /**
     * Convert this consumer to a {@link java.util.function.Consumer}.
     */
    default Consumer<T1> toConsumer() {
        return this::accept;
    }

    /**
     * Convert to this consumer from a {@link java.util.function.Consumer}.
     */
    static <T1> Consumer1<T1> from(Consumer<? super T1> consumer) {
        return consumer::accept;
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer0 acceptPartially(T1 v1) {
        return () -> accept(v1);
    }

    /**
     * Let this consumer partially accept the arguments.
     */
    default Consumer0 acceptPartially(Tuple1<? extends T1> args) {
        return () -> accept(args.v1);
    }
}
