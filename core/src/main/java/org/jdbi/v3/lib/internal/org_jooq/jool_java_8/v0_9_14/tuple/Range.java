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
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple;

import java.util.Optional;

/**
 * A range is a special {@link Tuple2} with two times the same type.
 *
 * @author Lukas Eder
 */
public class Range<T extends Comparable<T>> extends Tuple2<T, T> {

    private static final long serialVersionUID = 1L;

    public Range(T v1, T v2) {
        super(r(v1, v2));
    }

    public Range(Tuple2<T, T> tuple) {
        this(tuple.v1, tuple.v2);
    }

    private static <T extends Comparable<T>> Tuple2<T, T> r(T t1, T t2) {
        return t1.compareTo(t2) <= 0 ? new Tuple2<>(t1, t2) : new Tuple2<>(t2, t1);
    }

    /**
     * Whether two ranges overlap.
     * <p>
     * <code><pre>
     * // true
     * range(1, 3).overlaps(range(2, 4))
     *
     * // false
     * range(1, 3).overlaps(range(5, 8))
     * </pre></code>
     */
    public boolean overlaps(Tuple2<T, T> other) {
        return Tuple2.overlaps(this, other);
    }

    /**
     * Whether two ranges overlap.
     * <p>
     * <code><pre>
     * // true
     * range(1, 3).overlaps(2, 4)
     *
     * // false
     * range(1, 3).overlaps(5, 8)
     * </pre></code>
     */
    public boolean overlaps(T t1, T t2) {
        return overlaps(new Range<>(t1, t2));
    }

    /**
     * The intersection of two ranges.
     * <p>
     * <code><pre>
     * // (2, 3)
     * range(1, 3).intersect(range(2, 4))
     *
     * // none
     * range(1, 3).intersect(range(5, 8))
     * </pre></code>
     */
    public Optional<Range<T>> intersect(Tuple2<T, T> other) {
        return Tuple2.intersect(this, other).map(Range::new);
    }

    /**
     * The intersection of two ranges.
     * <p>
     * <code><pre>
     * // (2, 3)
     * range(1, 3).intersect(2, 4)
     *
     * // none
     * range(1, 3).intersect(5, 8)
     * </pre></code>
     */
    public Optional<Range<T>> intersect(T t1, T t2) {
        return intersect(new Range<>(t1, t2));
    }
}
