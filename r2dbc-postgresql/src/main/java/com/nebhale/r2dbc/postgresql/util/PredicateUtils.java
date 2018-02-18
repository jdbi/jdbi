/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nebhale.r2dbc.postgresql.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Utilities for working with {@link Predicate}s.
 */
public final class PredicateUtils {

    private PredicateUtils() {
    }

    /**
     * Negates a {@link Predicate}.  Exists primarily to enable negation of method references that are {@link Predicate}s.
     *
     * @param t   the predicate to negate
     * @param <T> the type of element being tested
     * @return a negated predicate
     * @see Predicate#negate()
     */
    public static <T> Predicate<T> not(Predicate<T> t) {
        requireNonNull(t, "t must not be null");
        return t.negate();
    }

    /**
     * Logical OR a collection of {@link Predicate}s.  Exists primarily to enable the logical OR of method references that are {@link Predicate}s.
     *
     * @param ts  the predicates to logical OR
     * @param <T> the type of element being tested
     * @return a local ORd collection of predicates
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> Predicate<T> or(Predicate<T>... ts) {
        requireNonNull(ts, "ts must not be null");
        return Arrays.stream(ts).reduce(Predicate::or).orElseThrow(() -> new IllegalStateException("Unable to combine predicates together via logical OR"));
    }

}
