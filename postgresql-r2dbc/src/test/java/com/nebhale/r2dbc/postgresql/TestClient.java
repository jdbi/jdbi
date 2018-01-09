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
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

final class TestClient implements Client {

    private final EmitterProcessor<FrontendMessage> requests = EmitterProcessor.create();

    private final EmitterProcessor<BackendMessage> responses = EmitterProcessor.create(false);

    private TestClient(Queue<Tuple2<FrontendMessage, Flux<BackendMessage>>> exchanges) {
        Objects.requireNonNull(exchanges);

        this.requests
            .flatMap(request -> {
                Tuple2<FrontendMessage, Flux<BackendMessage>> exchange = exchanges.poll();
                if (exchange == null) {
                    return Mono.error(new AssertionError("No more exchange expected"));
                }

                FrontendMessage expectedRequest = exchange.getT1();
                if (!expectedRequest.equals(request)) {
                    return Mono.error(new AssertionError(String.format("Request %s was not the expected request %s", request, expectedRequest)));
                }

                return exchange.getT2();
            })
            .doOnTerminate(() -> {
                System.out.println("CHARLIE");
                if (!exchanges.isEmpty()) {
                    throw new AssertionError(String.format("The following requests were never received: %s", getRequestMessages(exchanges)));
                }
            })
            .subscribe(this.responses);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Flux<BackendMessage> exchange(Publisher<FrontendMessage> publisher) {
        Objects.requireNonNull(publisher);

        return Flux.defer(() -> {
            Flux.from(publisher)
                .subscribe(this.requests);

            return this.responses;
        });
    }

    static Builder builder() {
        return new Builder();
    }

    private static String getRequestMessages(Queue<Tuple2<FrontendMessage, Flux<BackendMessage>>> exchanges) {
        return exchanges.stream()
            .map(tuple -> tuple.getT1().toString())
            .collect(Collectors.joining(", "));
    }

    static final class Builder {

        private final Queue<Tuple2<FrontendMessage, Flux<BackendMessage>>> exchanges = new LinkedList<>();

        TestClient build() {
            return new TestClient(this.exchanges);
        }

        Exchange when(FrontendMessage request) {
            Objects.requireNonNull(request);

            return new Exchange(this, request);
        }

    }

    static final class Exchange {

        private final Builder builder;

        private FrontendMessage request;

        private Exchange(Builder builder, FrontendMessage request) {
            this.builder = Objects.requireNonNull(builder);
            this.request = Objects.requireNonNull(request);
        }

        Builder thenRespond(BackendMessage... responses) {
            Objects.requireNonNull(responses);

            this.builder.exchanges.add(Tuples.of(this.request, Flux.just(responses)));
            return this.builder;
        }

    }

}
