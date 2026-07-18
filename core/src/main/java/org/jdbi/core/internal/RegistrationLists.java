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
package org.jdbi.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Helpers for the copy-on-write registration lists used by the immutable {@code JdbiConfig} values.
 * <p>
 * Registration lists are consulted most-recently-registered first (last-registered wins), so a new
 * registration is prepended to the front. These helpers return a fresh immutable list, leaving the input
 * untouched, which is the derivation step each config's {@code register} wither performs.
 */
public final class RegistrationLists {

    private RegistrationLists() {
        throw new UtilityClassException();
    }

    /**
     * Returns an immutable copy of {@code base} with {@code head} prepended (highest priority).
     *
     * @param base the existing registration list
     * @param head the element to prepend
     * @param <T>  the element type
     * @return a new immutable list with {@code head} at the front
     */
    public static <T> List<T> prepend(final List<T> base, final T head) {
        final List<T> result = new ArrayList<>(base.size() + 1);
        result.add(head);
        result.addAll(base);
        return List.copyOf(result);
    }

    /**
     * Returns an immutable copy of {@code base} with each element of {@code heads} adapted and prepended in
     * iteration order, so that — as with successive {@link #prepend} calls — the last element of {@code heads}
     * ends up at the front (highest priority).
     *
     * @param base    the existing registration list
     * @param heads   the elements to prepend, in registration order
     * @param adapter converts each source element into the list's element type
     * @param <S>     the source element type
     * @param <T>     the list element type
     * @return a new immutable list with the adapted heads prepended
     */
    public static <S, T> List<T> prependAll(final List<T> base,
            final Collection<? extends S> heads,
            final Function<? super S, ? extends T> adapter) {
        final List<T> result = new ArrayList<>(base.size() + heads.size());
        // Prepend in iteration order so that, as with successive prepend() calls, the last head wins.
        for (final S head : heads) {
            result.add(0, adapter.apply(head));
        }
        result.addAll(base);
        return List.copyOf(result);
    }

    /**
     * Returns an immutable copy of {@code base} with {@code tail} appended. Use this for lists whose consultation
     * order is registration order (first-registered wins, or the caller iterates in reverse for last-wins),
     * as opposed to the prepend lists above.
     *
     * @param base the existing registration list
     * @param tail the element to append
     * @param <T>  the element type
     * @return a new immutable list with {@code tail} at the end
     */
    public static <T> List<T> append(final List<T> base, final T tail) {
        final List<T> result = new ArrayList<>(base.size() + 1);
        result.addAll(base);
        result.add(tail);
        return List.copyOf(result);
    }

    /**
     * Returns an immutable copy of {@code base} with {@code tail} appended, unless {@code base} already contains
     * an equal element, in which case {@code base} is returned unchanged. This reproduces the insertion-order,
     * no-duplicates semantics of a set while keeping an immutable list representation.
     *
     * @param base the existing registration list
     * @param tail the element to append if not already present
     * @param <T>  the element type
     * @return a new immutable list with {@code tail} appended, or {@code base} if it was already present
     */
    public static <T> List<T> appendDistinct(final List<T> base, final T tail) {
        if (base.contains(tail)) {
            return base;
        }
        return append(base, tail);
    }
}
