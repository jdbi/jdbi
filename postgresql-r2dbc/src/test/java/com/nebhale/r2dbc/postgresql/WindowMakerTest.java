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

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class WindowMakerTest {

    @Test
    public void apply() {
        WindowCollector<String> windows = new WindowCollector<>();

        Flux.just("ALPHA", "ZULU", "BRAVO", "ZULU")
            .transform(WindowMaker.create("ZULU"::equals))
            .as(StepVerifier::create)
            .recordWith(windows)
            .expectNextCount(2)
            .verifyComplete();

        windows.next()
            .as(StepVerifier::create)
            .expectNext("ALPHA")
            .verifyComplete();

        windows.next()
            .as(StepVerifier::create)
            .expectNext("BRAVO")
            .verifyComplete();
    }

    @Test
    public void applyNoFlux() {
        assertThatNullPointerException().isThrownBy(() -> WindowMaker.create(i -> true).apply(null))
            .withMessage("flux must not be null");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createNoBoundary() {
        assertThatNullPointerException().isThrownBy(() -> WindowMaker.create((Class) null))
            .withMessage("boundary must not be null");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createNoIsBoundary() {
        assertThatNullPointerException().isThrownBy(() -> WindowMaker.create((Predicate) null))
            .withMessage("isBoundary must not be null");
    }

}