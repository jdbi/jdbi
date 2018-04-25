/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.collector;

import java.util.function.Function;
import java.util.function.Supplier;

class OptionalBuilder<T, O> {
    private final Supplier<O> empty;
    private final Function<T, O> factory;

    boolean set;
    T value;

    OptionalBuilder(Supplier<O> empty, Function<T, O> factory) {
        this.empty = empty;
        this.factory = factory;
    }

    void set(T value) {
        if (set) {
            throw tooManyValues(this.value, value);
        }

        this.value = value;
        this.set = true;
    }

    O build() {
        return value == null ? empty.get() : factory.apply(value);
    }

    static <T, OPT_T> OptionalBuilder<T, OPT_T> combine(
        OptionalBuilder<T, OPT_T> left,
        OptionalBuilder<T, OPT_T> right
    ) {
        if (left.set && right.set) {
            throw tooManyValues(left.value, right.value);
        }

        return left.set ? left : right;
    }

    private static <T> IllegalStateException tooManyValues(T first, T second) {
        return new IllegalStateException(
            String.format("Multiple values for optional: [%s, %s, ...]",
                stringify(first),
                stringify(second)));
    }

    private static String stringify(Object value) {
        return value == null ? null : "'" + value + "'";
    }
}
