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

import java.util.function.Consumer;
import java.util.function.ToLongBiFunction;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Sneaky;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Unchecked;

/**
 * A {@link ToLongBiFunction} that allows for checked exceptions.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface CheckedToLongBiFunction<T, U> {

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result
     */
    long applyAsLong(T t, U u) throws Throwable;

    /**
     * @see {@link Sneaky#toLongBiFunction(CheckedToLongBiFunction)}
     */
    static <T, U> ToLongBiFunction<T, U> sneaky(CheckedToLongBiFunction<T, U> function) {
        return Sneaky.toLongBiFunction(function);
    }

    /**
     * @see {@link Unchecked#toLongBiFunction(CheckedToLongBiFunction)}
     */
    static <T, U> ToLongBiFunction<T, U> unchecked(CheckedToLongBiFunction<T, U> function) {
        return Unchecked.toLongBiFunction(function);
    }

    /**
     * @see {@link Unchecked#toLongBiFunction(CheckedToLongBiFunction, Consumer)}
     */
    static <T, U> ToLongBiFunction<T, U> unchecked(CheckedToLongBiFunction<T, U> function, Consumer<Throwable> handler) {
        return Unchecked.toLongBiFunction(function, handler);
    }
}
