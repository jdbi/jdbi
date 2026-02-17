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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jdbi.core.internal.exceptions.Unchecked;

@SuppressWarnings("rawtypes")
public final class CollectionCollectors {
    private static final Supplier<Collector> SET_COLLECTOR = setFactory();

    private CollectionCollectors() {}

    private static Supplier<Collector> setFactory() {
        try {
            MethodHandle mh = MethodHandles.publicLookup()
                    .findStatic(Collectors.class, "toUnmodifiableSet", MethodType.methodType(Collector.class));
            return Unchecked.supplier(() -> (Collector) mh.invokeExact());
        } catch (final ReflectiveOperationException ignore) {
            // ignored for java 8+9 compat
        }
        return () -> Collectors.collectingAndThen(
                Collectors.toSet(),
                Collections::unmodifiableSet);
    }

    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return SET_COLLECTOR.get();
    }
}
