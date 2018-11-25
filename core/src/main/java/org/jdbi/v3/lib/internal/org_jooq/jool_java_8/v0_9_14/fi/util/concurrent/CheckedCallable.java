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
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.fi.util.concurrent;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Sneaky;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Unchecked;

/**
 * A {@link Callable} that allows for checked exceptions.
 *
 * @author Zack Young
 */
@FunctionalInterface
public interface CheckedCallable<T> {

    /**
     * Run this callable.
     */
    T call() throws Throwable;

    /**
     * @see {@link Sneaky#callable(CheckedCallable)}
     */
    static <T> Callable<T> sneaky(CheckedCallable<T> callable) {
        return Sneaky.callable(callable);
    }

    /**
     * @see {@link Unchecked#callable(CheckedCallable)}
     */
    static <T> Callable<T> unchecked(CheckedCallable<T> callable) {
        return Unchecked.callable(callable);
    }

    /**
     * @see {@link Unchecked#callable(CheckedCallable, Consumer)}
     */
    static <T> Callable<T> unchecked(CheckedCallable<T> callable, Consumer<Throwable> handler) {
        return Unchecked.callable(callable, handler);
    }
}
