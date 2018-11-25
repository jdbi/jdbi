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

import java.util.function.Predicate;

import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.Seq;

/**
 * @author Lukas Eder
 */
public final class Functions {

    /**
     * Negate a predicate.
     */
    public static final <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

    /**
     * Negate a predicate.
     */
    @SafeVarargs
    public static final <T> Predicate<T> and(Predicate<T>... predicates) {
        return Seq.of(predicates).reduce(t -> true, (t1, t2) -> t1.and(t2));
    }

    /**
     * Negate a predicate.
     */
    @SafeVarargs
    public static final <T> Predicate<T> or(Predicate<T>... predicates) {
        return Seq.of(predicates).reduce(t -> false, (t1, t2) -> t1.or(t2));
    }
}
