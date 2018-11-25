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
import java.util.function.IntSupplier;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Sneaky;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Unchecked;

/**
 * A {@link IntSupplier} that allows for checked exceptions.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface CheckedIntSupplier {

    /**
     * Gets a result.
     *
     * @return a result
     */
    int getAsInt() throws Throwable;

    /**
     * @see {@link Sneaky#intSupplier(CheckedIntSupplier)}
     */
    static IntSupplier sneaky(CheckedIntSupplier supplier) {
        return Sneaky.intSupplier(supplier);
    }

    /**
     * @see {@link Unchecked#intSupplier(CheckedIntSupplier)}
     */
    static IntSupplier unchecked(CheckedIntSupplier supplier) {
        return Unchecked.intSupplier(supplier);
    }

    /**
     * @see {@link Unchecked#intSupplier(CheckedIntSupplier, Consumer)}
     */
    static IntSupplier unchecked(CheckedIntSupplier supplier, Consumer<Throwable> handler) {
        return Unchecked.intSupplier(supplier, handler);
    }
}
