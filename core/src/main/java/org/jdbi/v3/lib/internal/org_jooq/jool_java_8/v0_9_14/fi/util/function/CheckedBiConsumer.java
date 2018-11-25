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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Sneaky;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Unchecked;

/**
 * A {@link BiConsumer} that allows for checked exceptions.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface CheckedBiConsumer<T, U> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    void accept(T t, U u) throws Throwable;

    /**
     * @see {@link Sneaky#biConsumer(CheckedBiConsumer)}
     */
    static <T, U> BiConsumer<T, U> sneaky(CheckedBiConsumer<T, U> consumer) {
        return Sneaky.biConsumer(consumer);
    }

    /**
     * @see {@link Unchecked#biConsumer(CheckedBiConsumer)}
     */
    static <T, U> BiConsumer<T, U> unchecked(CheckedBiConsumer<T, U> consumer) {
        return Unchecked.biConsumer(consumer);
    }

    /**
     * @see {@link Unchecked#biConsumer(CheckedBiConsumer, Consumer)}
     */
    static <T, U> BiConsumer<T, U> unchecked(CheckedBiConsumer<T, U> consumer, Consumer<Throwable> handler) {
        return Unchecked.biConsumer(consumer, handler);
    }
}
