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
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.fi.util.function;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Sneaky;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Unchecked;

/**
 * A {@link BiPredicate} that allows for checked exceptions.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface CheckedBiPredicate<T, U> {

    /**
     * Evaluates this predicate on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @return {@code true} if the input arguments match the predicate,
     * otherwise {@code false}
     */
    boolean test(T t, U u) throws Throwable;

    /**
     * @see {@link Sneaky#biPredicate(CheckedBiPredicate)}
     */
    static <T, U> BiPredicate<T, U> sneaky(CheckedBiPredicate<T, U> predicate) {
        return Sneaky.biPredicate(predicate);
    }

    /**
     * @see {@link Unchecked#biPredicate(CheckedBiPredicate)}
     */
    static <T, U> BiPredicate<T, U> unchecked(CheckedBiPredicate<T, U> predicate) {
        return Unchecked.biPredicate(predicate);
    }

    /**
     * @see {@link Unchecked#biPredicate(CheckedBiPredicate, Consumer)}
     */
    static <T, U> BiPredicate<T, U> unchecked(CheckedBiPredicate<T, U> predicate, Consumer<Throwable> handler) {
        return Unchecked.biPredicate(predicate, handler);
    }
}
