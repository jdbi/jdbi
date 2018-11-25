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

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14.tuple.Tuple2;

/**
 * @author Lukas Eder
 */
class Partition<T> {

    final List<Tuple2<T, Long>> list;
    final Map<Object, Object>   cache;

    Partition(Collection<Tuple2<T, Long>> list) {
        this.list = list instanceof ArrayList ? (List<Tuple2<T, Long>>) list : new ArrayList<>(list);
        this.cache = new HashMap<>();
    }

    <R> R cacheIf(boolean condition, Object key, Supplier<? extends R> value) {
        return cacheIf(() -> condition, () -> key, value);
    }

    <R> R cacheIf(boolean condition, Supplier<?> key, Supplier<? extends R> value) {
        if (condition)
            return cache(key, value);
        else
            return value.get();
    }

    <R> R cacheIf(BooleanSupplier condition, Object key, Supplier<? extends R> value) {
        return cacheIf(condition, () -> key, value);
    }

    <R> R cacheIf(BooleanSupplier condition, Supplier<?> key, Supplier<? extends R> value) {
        if (condition.getAsBoolean())
            return cache(key, value);
        else
            return value.get();
    }

    @SuppressWarnings("unchecked")
    <R> R cache(Object key, Supplier<? extends R> value) {
        return cache(() -> key, value);
    }

    @SuppressWarnings("unchecked")
    <R> R cache(Supplier<?> key, Supplier<? extends R> value) {
        return (R) cache.computeIfAbsent(key.get(), k -> value.get());
    }
}
