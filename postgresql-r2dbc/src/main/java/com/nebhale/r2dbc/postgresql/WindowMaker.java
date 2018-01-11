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

package com.nebhale.r2dbc.postgresql;

import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Function;
import java.util.function.Predicate;

final class WindowMaker<T> implements Function<Flux<T>, Flux<Flux<T>>> {

    private static final int FALSE = 0;

    private static final AtomicIntegerFieldUpdater<WindowMaker> IS_IN_WINDOW = AtomicIntegerFieldUpdater.newUpdater(WindowMaker.class, "isInWindow");

    private static final int TRUE = 1;

    private final Predicate<T> isBoundary;

    private final EmitterProcessor<Integer> opener = EmitterProcessor.create();

    private volatile EmitterProcessor<Integer> closer = EmitterProcessor.create(false);

    private volatile int isInWindow;

    private WindowMaker(Predicate<T> isBoundary) {
        this.isBoundary = isBoundary;
    }

    @Override
    public Flux<Flux<T>> apply(Flux<T> flux) {
        return flux
            .doOnNext(t -> {  // TODO: Ask Stephane if putting these together is the right choice
                if (this.isBoundary.test(t) && IS_IN_WINDOW.getAndSet(this, FALSE) == TRUE) {
                    this.closer.onNext(0);
                } else if (this.isBoundary.negate().test(t) && IS_IN_WINDOW.getAndSet(this, TRUE) == FALSE) {
                    this.opener.onNext(0);
                }
            })
            .windowWhen(this.opener::subscribe, i -> this.closer);
    }


    static WindowMaker<BackendMessage> create(Class<? extends BackendMessage> boundary) {
        return create(boundary::isInstance);
    }

    static <T> WindowMaker<T> create(Predicate<T> isBoundary) {
        return new WindowMaker<>(isBoundary);
    }

}
