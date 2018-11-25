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
import java.util.function.IntToLongFunction;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Sneaky;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Unchecked;

/**
 * A {@link IntToLongFunction} that allows for checked exceptions.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface CheckedIntToLongFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    long applyAsLong(int value) throws Throwable;

    /**
     * @see {@link Sneaky#intToLongFunction(CheckedIntToLongFunction)}
     */
    static IntToLongFunction sneaky(CheckedIntToLongFunction function) {
        return Sneaky.intToLongFunction(function);
    }

    /**
     * @see {@link Unchecked#intToLongFunction(CheckedIntToLongFunction)}
     */
    static IntToLongFunction unchecked(CheckedIntToLongFunction function) {
        return Unchecked.intToLongFunction(function);
    }

    /**
     * @see {@link Unchecked#intToLongFunction(CheckedIntToLongFunction, Consumer)}
     */
    static IntToLongFunction unchecked(CheckedIntToLongFunction function, Consumer<Throwable> handler) {
        return Unchecked.intToLongFunction(function, handler);
    }
}
