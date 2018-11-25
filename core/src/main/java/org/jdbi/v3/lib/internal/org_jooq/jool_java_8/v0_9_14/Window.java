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
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * A window containing the data for its partition, to perform calculations upon.
 * <p>
 * Window functions as exposed in this type are inspired by their counterparts
 * in SQL. They include:
 * <h3>Ranking functions</h3>
 * Ranking functions are useful to determine the "rank" of any given row within
 * the partition, given a specific ordering. The following table explains
 * individual ranking functions:
 * <table>
 * <tr>
 * <th>Function</th><th>Description</th>
 * </tr>
 * <tr>
 * <td>{@link #rowNumber()}</td><td>The distinct row number of the row within
 * the partition, counting from <code>0</code>.</td>
 * </tr>
 * <tr>
 * <td>{@link #rank()}</td><td>The rank with gaps of a row within the partition,
 * counting from <code>0</code>.</td>
 * </tr>
 * <tr>
 * <td>{@link #denseRank()}</td><td>The rank without gaps of a row within the
 * partition, counting from <code>0</code>.</td>
 * </tr>
 * <tr>
 * <td>{@link #percentRank()}</td><td>Relative rank of a row:
 * {@link #rank()} / {@link #count()}.</td>
 * </tr>
 * <tr>
 * <td>{@link #ntile(long)}</td><td>Divides the partition in equal buckets and
 * assigns values between <code>0</code> and <code>buckets - 1</code>.</td>
 * </tr>
 * <tr>
 * <td>{@link #lead()}</td><td>Gets the value after the current row.</td>
 * </tr>
 * <tr>
 * <td>{@link #lag()}</td><td>Gets the value before the current row.</td>
 * </tr>
 * <tr>
 * <td>{@link #firstValue()}</td><td>Gets the first value in the window.</td>
 * </tr>
 * <tr>
 * <td>{@link #lastValue()}</td><td>Gets the last value in the window.</td>
 * </tr>
 * <tr>
 * <td>{@link #nthValue(long)}</td><td>Gets the nth value in the window,
 * counting from <code>0</code>.</td>
 * </tr>
 * </table>
 * <p>
 * <strong>Note:</strong> In Java, indexes are always counted from
 * <code>0</code>, not from <code>1</code> as in SQL. This means that the above
 * ranking functions also rank rows from zero. This is particularly true for:
 * <ul>
 * <li>{@link #rowNumber()}</li>
 * <li>{@link #rank()}</li>
 * <li>{@link #denseRank()}</li>
 * <li>{@link #ntile(long)}</li>
 * <li>{@link #nthValue(long)}</li>
 * </ul>
 *
 * <h3>Aggregate functions</h3>
 * Each aggregate function from {@link Seq} or from {@link Agg} is also
 * available as an aggregate function on the window. For instance,
 * {@link #count()} is the same as calling {@link Seq#count()} on
 * {@link #window()}
 *
 * @author Lukas Eder
 */
public interface Window<T> extends Collectable<T> {

    static <T> WindowSpecification<T> of() {
        return new WindowSpecificationImpl<>(t -> SeqImpl.NULL, null, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    static <T> WindowSpecification<T> of(long lower, long upper) {
        return new WindowSpecificationImpl<>(t -> SeqImpl.NULL, null, lower, upper);
    }

    static <T> WindowSpecification<T> of(Comparator<? super T> orderBy) {
        return new WindowSpecificationImpl<>(t -> SeqImpl.NULL, orderBy, Long.MIN_VALUE, 0);
    }

    static <T> WindowSpecification<T> of(Comparator<? super T> orderBy, long lower, long upper) {
        return new WindowSpecificationImpl<>(t -> SeqImpl.NULL, orderBy, lower, upper);
    }

    static <T, U> WindowSpecification<T> of(Function<? super T, ? extends U> partitionBy) {
        return new WindowSpecificationImpl<>(partitionBy, null, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    static <T, U> WindowSpecification<T> of(Function<? super T, ? extends U> partitionBy, long lower, long upper) {
        return new WindowSpecificationImpl<>(partitionBy, null, lower, upper);
    }

    static <T, U> WindowSpecification<T> of(Function<? super T, ? extends U> partitionBy, Comparator<? super T> orderBy) {
        return new WindowSpecificationImpl<>(partitionBy, orderBy, Long.MIN_VALUE, 0);
    }

    static <T, U> WindowSpecification<T> of(Function<? super T, ? extends U> partitionBy, Comparator<? super T> orderBy, long lower, long upper) {
        return new WindowSpecificationImpl<>(partitionBy, orderBy, lower, upper);
    }

    /**
     * The value of the current row in the window.
     */
    T value();

    /**
     * Stream all elements in the window.
     */
    Seq<T> window();

    /**
     * The row number of the current row within the partition.
     * <p>
     * <pre><code>
     * // (1, 2, 3, 4, 5)
     * Seq.of(1, 2, 4, 2, 3).window().map(w -> w.rowNumber());
     * </code></pre>
     */
    long rowNumber();

    /**
     * The rank of the current row within the partition.
     * <p>
     * <pre><code>
     * // (1, 2, 2, 4, 5)
     * Seq.of(1, 2, 2, 3, 4).window(naturalOrder()).map(w -> w.rank());
     * </code></pre>
     */
    long rank();

    /**
     * The dense rank of the current row within the partition.
     * <p>
     * <pre><code>
     * // (1, 2, 2, 3, 4)
     * Seq.of(1, 2, 2, 3, 4).window(naturalOrder()).map(w -> w.denseRank());
     * </code></pre>
     */
    long denseRank();

    /**
     * The precent rank of the current row within the partition.
     * <p>
     * <pre><code>
     * // (0.0, 0.25, 0.25, 0.75, 1.0)
     * Seq.of(1, 2, 2, 3, 4).window(naturalOrder()).map(w -> w.percentRank());
     * </code></pre>
     */
    double percentRank();

    /**
     * The bucket number ("ntile") of the current row within the partition.
     * <p>
     * <pre><code>
     * // (0, 0, 1, 1, 2)
     * Seq.of(1, 2, 2, 3, 4).window(naturalOrder()).map(w -> w.ntile(3));
     * </code></pre>
     */
    long ntile(long buckets);

    /**
     * The next value in the window.
     * <p>
     * This is the same as calling <code>lead(1)</code>
     * <p>
     * <pre><code>
     * // (2, 2, 3, 4, empty)
     * Seq.of(1, 2, 2, 3, 4).window().map(w -> w.lead());
     * </code></pre>
     */
    Optional<T> lead();

    /**
     * The next value by <code>lead</code> in the window.
     * <p>
     * <pre><code>
     * // (2, 2, 3, 4, empty)
     * Seq.of(1, 2, 2, 3, 4).window().map(w -> w.lead());
     * </code></pre>
     */
    Optional<T> lead(long lead);

    /**
     * The previous value in the window.
     * <p>
     * This is the same as calling <code>lag(1)</code>
     * <p>
     * <pre><code>
     * // (empty, 1, 2, 2, 3)
     * Seq.of(1, 2, 2, 3, 4).window().map(w -> w.lag());
     * </code></pre>
     */
    Optional<T> lag();

    /**
     * The previous value by <code>lag</code> in the window.
     * <p>
     * <pre><code>
     * // (empty, 1, 2, 2, 3)
     * Seq.of(1, 2, 2, 3, 4).window().map(w -> w.lag());
     * </code></pre>
     */
    Optional<T> lag(long lag);

    /**
     * The first value in the window.
     * <p>
     * <pre><code>
     * // (1, 1, 1, 1, 1)
     * Seq.of(1, 2, 4, 2, 3).window().map(w -> w.firstValue());
     * </code></pre>
     */
    Optional<T> firstValue();

    /**
     * The first value in the window.
     * <p>
     * <pre><code>
     * // (1, 1, 1, 1, 1)
     * Seq.of(1, 2, 4, 2, 3).window().map(w -> w.firstValue());
     * </code></pre>
     */
    <U> Optional<U> firstValue(Function<? super T, ? extends U> function);

    /**
     * The last value in the window.
     * <p>
     * <pre><code>
     * // (3, 3, 3, 3, 3)
     * Seq.of(1, 2, 4, 2, 3).window().map(w -> w.lastValue());
     * </code></pre>
     */
    Optional<T> lastValue();

    /**
     * The last value in the window.
     * <p>
     * <pre><code>
     * // (3, 3, 3, 3, 3)
     * Seq.of(1, 2, 4, 2, 3).window().map(w -> w.lastValue());
     * </code></pre>
     */
    <U> Optional<U> lastValue(Function<? super T, ? extends U> function);

    /**
     * The nth value in the window.
     * <p>
     * <pre><code>
     * // (4, 4, 4, 4, 4)
     * Seq.of(1, 2, 4, 2, 3).window().map(w -> w.nthValue(2));
     * </code></pre>
     */
    Optional<T> nthValue(long n);

    /**
     * The nth value in the window.
     * <p>
     * <pre><code>
     * // (4, 4, 4, 4, 4)
     * Seq.of(1, 2, 4, 2, 3).window().map(w -> w.nthValue(2));
     * </code></pre>
     */
    <U> Optional<U> nthValue(long n, Function<? super T, ? extends U> function);
}
