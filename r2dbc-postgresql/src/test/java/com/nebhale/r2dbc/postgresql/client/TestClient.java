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

import com.nebhale.r2dbc.core.nullability.Nullable;
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import com.nebhale.r2dbc.postgresql.util.TestByteBufAllocator;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.nebhale.r2dbc.postgresql.client.TransactionStatus.IDLE;

public final class TestClient implements Client {

    public static final TestClient NO_OP = new TestClient(false, Collections.emptyMap(), null, null, Flux.empty(), IDLE);

    private final boolean expectClose;

    private final Map<String, String> parameterStatus;

    private final Integer processId;

    private final EmitterProcessor<FrontendMessage> requestProcessor = EmitterProcessor.create(false);

    private final FluxSink<FrontendMessage> requests = this.requestProcessor.sink();

    private final EmitterProcessor<Flux<BackendMessage>> responseProcessor = EmitterProcessor.create(false);

    private final Integer secretKey;

    private final TransactionStatus transactionStatus;

    private TestClient(boolean expectClose, Map<String, String> parameterStatus, @Nullable Integer processId, @Nullable Integer secretKey, Flux<Window> windows, TransactionStatus transactionStatus) {
        this.expectClose = expectClose;
        this.parameterStatus = Objects.requireNonNull(parameterStatus);
        this.processId = processId;
        this.secretKey = secretKey;
        this.transactionStatus = Objects.requireNonNull(transactionStatus);

        FluxSink<Flux<BackendMessage>> responses = this.responseProcessor.sink();

        Objects.requireNonNull(windows)
            .map(window -> window.exchanges)
            .map(exchanges -> exchanges
                .concatMap(exchange ->

                    this.requestProcessor.zipWith(exchange.requests)
                        .handle((tuple, sink) -> {
                            FrontendMessage actual = tuple.getT1();
                            FrontendMessage expected = tuple.getT2();

                            if (!actual.equals(expected)) {
                                sink.error(new AssertionError(String.format("Request %s was not the expected request %s", actual, expected)));
                            }
                        })
                        .thenMany(exchange.responses)))
            .subscribe(responses::next, responses::error, responses::complete);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Mono<Void> close() {
        return this.expectClose ? Mono.empty() : Mono.error(new AssertionError("close called unexpectedly"));
    }

    @Override
    public Flux<BackendMessage> exchange(Publisher<FrontendMessage> requests) {
        Objects.requireNonNull(requests, "requests must not be null");

        return this.responseProcessor
            .doOnSubscribe(s ->
                Flux.from(requests)
                    .subscribe(this.requests::next, this.requests::error))
            .next()
            .flatMapMany(Function.identity());
    }

    @Override
    public ByteBufAllocator getByteBufAllocator() {
        return TestByteBufAllocator.TEST;
    }

    @Override
    public Map<String, String> getParameterStatus() {
        return this.parameterStatus;
    }

    @Override
    public Optional<Integer> getProcessId() {
        return Optional.ofNullable(this.processId);
    }

    @Override
    public Optional<Integer> getSecretKey() {
        return Optional.ofNullable(this.secretKey);
    }

    @Override
    public TransactionStatus getTransactionStatus() {
        return this.transactionStatus;
    }

    public static final class Builder {

        private final Map<String, String> parameterStatus = new HashMap<>();

        private final List<Window.Builder<?>> windows = new ArrayList<>();

        private boolean expectClose = false;

        private Integer processId = null;

        private Integer secretKey = null;

        private TransactionStatus transactionStatus = IDLE;

        private Builder() {
        }

        public TestClient build() {
            return new TestClient(this.expectClose, parameterStatus, this.processId, this.secretKey, Flux.fromIterable(this.windows).map(Window.Builder::build), this.transactionStatus);
        }

        public Builder expectClose() {
            this.expectClose = true;
            return this;
        }

        public Exchange.Builder<Builder> expectRequest(FrontendMessage... requests) {
            Objects.requireNonNull(requests);

            Window.Builder<Builder> window = new Window.Builder<>(this);
            this.windows.add(window);

            Exchange.Builder<Builder> exchange = new Exchange.Builder<>(this, requests);
            window.exchanges.add(exchange);

            return exchange;
        }

        public Builder parameterStatus(String key, String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            this.parameterStatus.put(key, value);
            return this;
        }

        public Builder processId(Integer processId) {
            this.processId = Objects.requireNonNull(processId);
            return this;
        }

        public Builder secretKey(Integer secretKey) {
            this.secretKey = Objects.requireNonNull(secretKey);
            return this;
        }

        public Builder transactionStatus(TransactionStatus transactionStatus) {
            this.transactionStatus = Objects.requireNonNull(transactionStatus);
            return this;
        }

        public Window.Builder<Builder> window() {
            Window.Builder<Builder> window = new Window.Builder<>(this);
            this.windows.add(window);
            return window;
        }

    }

    private static final class Exchange {

        private final Flux<FrontendMessage> requests;

        private final Publisher<BackendMessage> responses;

        private Exchange(Flux<FrontendMessage> requests, Publisher<BackendMessage> responses) {
            this.requests = Objects.requireNonNull(requests);
            this.responses = Objects.requireNonNull(responses);
        }

        public static final class Builder<T> {

            private final T chain;

            private final Flux<FrontendMessage> requests;

            private Publisher<BackendMessage> responses;

            private Builder(T chain, FrontendMessage... requests) {
                this.chain = Objects.requireNonNull(chain);
                this.requests = Flux.just(Objects.requireNonNull(requests));
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
                return new Exchange(this.requests, this.responses);
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
