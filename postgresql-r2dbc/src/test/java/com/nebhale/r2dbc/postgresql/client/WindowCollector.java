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

package com.nebhale.r2dbc.postgresql.client;

import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Supplier;

public final class WindowCollector<T> implements Supplier<Collection<Flux<T>>> {

    private final Queue<Flux<T>> windows = new LinkedList<>();

    @Override
    public Collection<Flux<T>> get() {
        return this.windows;
    }

    public Flux<T> next() {
        return Optional.ofNullable(this.windows.poll()).orElseThrow(() -> new AssertionError("No more windows were emitted"));
    }

}
