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

import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static reactor.function.TupleUtils.function;

public final class TestClient implements Client {

    public static final TestClient NO_OP = new TestClient(false, Flux.empty());

    private final boolean expectClose;

    private final EmitterProcessor<FrontendMessage> requestProcessor = EmitterProcessor.create(false);

    private final FluxSink<FrontendMessage> requests = this.requestProcessor.sink();

    private final EmitterProcessor<Flux<BackendMessage>> responseProcessor = EmitterProcessor.create(false);

    private TestClient(boolean expectClose, Flux<Window> windows) {
        Objects.requireNonNull(windows);

        this.expectClose = expectClose;

        windows
            .map(window -> window.exchanges)
            .map(window -> this.requestProcessor.zipWith(window)
                .concatMap(function((request, exchange) -> {

                    FrontendMessage expectedRequest = exchange.request;
                    if (!expectedRequest.equals(request)) {
                        return Mono.error(new AssertionError(String.format("Request %s was not the expected request %s", request, expectedRequest)));
                    }

                    return Flux.from(exchange.responses);
                })))
            .subscribe(this.responseProcessor);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void close() {
        if (!this.expectClose) {
            throw new AssertionError("close called unexpectedly");
        }
    }

    @Override
    public Flux<BackendMessage> exchange(Publisher<FrontendMessage> publisher) {
        Objects.requireNonNull(publisher, "publisher must not be null");

        return Flux.defer(() -> {
            Flux.from(publisher)
                .subscribe(this.requests::next, this.requests::error);

            return this.responseProcessor
                .next()
                .flatMapMany(Function.identity());
        });
    }

    public static final class Builder {

        private final List<Window.Builder<?>> windows = new ArrayList<>();

        private boolean expectClose = false;

        private Builder() {
        }

        public TestClient build() {
            return new TestClient(this.expectClose, Flux.fromIterable(this.windows).map(Window.Builder::build));
        }

        public Builder expectClose() {
            this.expectClose = true;
            return this;
        }

        public Exchange.Builder<Builder> expectRequest(FrontendMessage request) {
            Objects.requireNonNull(request);

            Window.Builder<Builder> window = new Window.Builder<>(this);
            this.windows.add(window);

            Exchange.Builder<Builder> exchange = new Exchange.Builder<>(this, request);
            window.exchanges.add(exchange);

            return exchange;
        }

        public Window.Builder<Builder> window() {
            Window.Builder<Builder> window = new Window.Builder<>(this);
            this.windows.add(window);
            return window;
        }

    }

    private static final class Exchange {

        private final FrontendMessage request;

        private final Publisher<BackendMessage> responses;

        private Exchange(FrontendMessage request, Publisher<BackendMessage> responses) {
            this.request = Objects.requireNonNull(request);
            this.responses = Objects.requireNonNull(responses);
        }

        public static final class Builder<T> {

            private final T chain;

            private final FrontendMessage request;

            private Publisher<BackendMessage> responses;

            private Builder(T chain, FrontendMessage request) {
                this.chain = Objects.requireNonNull(chain);
                this.request = Objects.requireNonNull(request);
            }

            public T thenRespond(BackendMessage... responses) {
                Objects.requireNonNull(responses);

                return thenRespond(Flux.just(responses));
            }

            public T thenRespond(Publisher<BackendMessage> responses) {
                Objects.requireNonNull(responses);

                this.responses = responses;
                return this.chain;
            }

            private Exchange build() {
                return new Exchange(this.request, this.responses);
            }

        }

    }

    private static final class Window {

        private final Flux<Exchange> exchanges;

        private Window(Flux<Exchange> exchanges) {
            this.exchanges = Objects.requireNonNull(exchanges);
        }

        public static final class Builder<T> {

            private final T chain;

            private final List<Exchange.Builder<?>> exchanges = new ArrayList<>();

            private Builder(T chain) {
                this.chain = Objects.requireNonNull(chain);
            }

            public T done() {
                return this.chain;
            }

            public Exchange.Builder<Builder<T>> expectRequest(FrontendMessage request) {
                Objects.requireNonNull(request);

                Exchange.Builder<Builder<T>> exchange = new Exchange.Builder<>(this, request);
                this.exchanges.add(exchange);
                return exchange;
            }

            private Window build() {
                return new Window(Flux.fromIterable(this.exchanges).map(Exchange.Builder::build));
            }

        }

    }

}
