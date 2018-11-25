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
import java.util.function.IntBinaryOperator;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Sneaky;
import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Unchecked;

/**
 * A {@link IntBinaryOperator} that allows for checked exceptions.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface CheckedIntBinaryOperator {

    /**
     * Applies this operator to the given operands.
     *
     * @param left the first operand
     * @param right the second operand
     * @return the operator result
     */
    int applyAsInt(int left, int right) throws Throwable;

    /**
     * @see {@link Sneaky#intBinaryOperator(CheckedIntBinaryOperator)}
     */
    static IntBinaryOperator sneaky(CheckedIntBinaryOperator operator) {
        return Sneaky.intBinaryOperator(operator);
    }

    /**
     * @see {@link Unchecked#intBinaryOperator(CheckedIntBinaryOperator)}
     */
    static IntBinaryOperator unchecked(CheckedIntBinaryOperator operator) {
        return Unchecked.intBinaryOperator(operator);
    }

    /**
     * @see {@link Unchecked#intBinaryOperator(CheckedIntBinaryOperator, Consumer)}
     */
    static IntBinaryOperator unchecked(CheckedIntBinaryOperator operator, Consumer<Throwable> handler) {
        return Unchecked.intBinaryOperator(operator, handler);
    }
}
