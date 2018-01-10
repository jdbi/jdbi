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
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

final class TestClient implements Client {

    static final TestClient NO_OP = new TestClient(new LinkedList<>(), true, false);

    private final boolean complete;

    private final boolean expectClose;

    private final EmitterProcessor<FrontendMessage> requestProcessor = EmitterProcessor.create();

    private final FluxSink<FrontendMessage> requests = this.requestProcessor.sink();

    private final EmitterProcessor<BackendMessage> responseProcessor = EmitterProcessor.create(false);

    private TestClient(Queue<Tuple2<FrontendMessage, Flux<BackendMessage>>> exchanges, boolean complete, boolean expectClose) {
        this.complete = complete;
        Objects.requireNonNull(exchanges);
        this.expectClose = expectClose;

        this.requestProcessor
            .flatMap(request -> {
                Tuple2<FrontendMessage, Flux<BackendMessage>> exchange = exchanges.poll();
                if (exchange == null) {
                    return Mono.error(new AssertionError("No more exchange expected"));
                }

                FrontendMessage expectedRequest = exchange.getT1();
                if (!expectedRequest.equals(request)) {
                    return Mono.error(new AssertionError(String.format("Request %s was not the expected request %s", request, expectedRequest)));
                }

                return exchange.getT2()
                    .doOnComplete(() -> {
                        if (this.complete && exchanges.isEmpty()) {
                            this.responseProcessor.onComplete();
                        }
                    });
            })
            .subscribe(this.responseProcessor);
    }

    @Override
    public void close() {
        if (!this.expectClose) {
            throw new AssertionError("close called unexpectedly");
        }
    }

    @Override
    public Flux<BackendMessage> exchange(Publisher<FrontendMessage> publisher) {
        Objects.requireNonNull(publisher);

        return Flux.defer(() -> {
            Flux.from(publisher)
                .subscribe(this.requests::next, this.requests::error);

            return this.responseProcessor;
        });
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {

        private final Queue<Tuple2<FrontendMessage, Flux<BackendMessage>>> exchanges = new LinkedList<>();

        private boolean complete = true;

        private boolean expectClose = false;

        TestClient build() {
            return new TestClient(this.exchanges, this.complete, this.expectClose);
        }

        Builder expectClose() {
            this.expectClose = true;
            return this;
        }

        Exchange expectRequest(FrontendMessage request) {
            Objects.requireNonNull(request);

            return new Exchange(this, request);
        }

        Builder noComplete() {
            this.complete = false;
            return this;
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

            return thenRespond(Flux.just(responses));
        }

        Builder thenRespond(Flux<BackendMessage> responses) {
            Objects.requireNonNull(responses);

            this.builder.exchanges.add(Tuples.of(this.request, responses));
            return this.builder;
        }

    }

}
