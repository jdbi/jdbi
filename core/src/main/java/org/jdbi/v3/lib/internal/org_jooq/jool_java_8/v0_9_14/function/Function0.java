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
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.function;

import java.util.function.Supplier;

import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple0;

/**
 * A function with 0 arguments.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface Function0<R> extends Supplier<R> {

    /**
     * Apply this function to the arguments.
     */
    default R apply() {
        return get();
    }

    /**
     * Apply this function to the arguments.
     *
     * @param args The arguments as a tuple.
     */
    default R apply(Tuple0 args) {
        return get();
    }

    /**
     * Apply this function to the arguments.
     */
    @Override
    R get();

    /**
     * Convert this function to a {@link java.util.function.Supplier}
     */
    default Supplier<R> toSupplier() {
        return this::apply;
    }

    /**
     * Convert to this function from a {@link java.util.function.Supplier}
     */
    static <R> Function0<R> from(Supplier<R> supplier) {
        return supplier::get;
    }

}
