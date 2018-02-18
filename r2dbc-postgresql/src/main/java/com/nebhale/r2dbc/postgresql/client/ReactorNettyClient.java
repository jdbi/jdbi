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

import com.nebhale.r2dbc.postgresql.message.backend.BackendKeyData;
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessageDecoder;
import com.nebhale.r2dbc.postgresql.message.backend.ErrorResponse;
import com.nebhale.r2dbc.postgresql.message.backend.Field;
import com.nebhale.r2dbc.postgresql.message.backend.NoticeResponse;
import com.nebhale.r2dbc.postgresql.message.backend.ParameterStatus;
import com.nebhale.r2dbc.postgresql.message.backend.ReadyForQuery;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.ipc.netty.tcp.BlockingNettyContext;
import reactor.ipc.netty.tcp.TcpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.nebhale.r2dbc.postgresql.client.TransactionStatus.IDLE;
import static com.nebhale.r2dbc.postgresql.util.PredicateUtils.not;
import static java.util.Objects.requireNonNull;

/**
 * An implementation of client based on the Reactor Netty project.
 *
 * @see TcpClient
 */
public final class ReactorNettyClient implements Client {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AtomicReference<ByteBufAllocator> byteBufAllocator = new AtomicReference<>();

    private final BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleErrorResponse = handleBackendMessage(ErrorResponse.class,
        (message, sink) -> {
            this.logger.error("Error: {}", toString(message.getFields()));
            sink.next(message);
        });

    private final BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleNoticeResponse = handleBackendMessage(NoticeResponse.class,
        (message, sink) -> this.logger.warn("Notice: {}", toString(message.getFields())));

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final AtomicReference<BlockingNettyContext> nettyContext = new AtomicReference<>();

    private final ConcurrentMap<String, String> parameterStatus = new ConcurrentHashMap<>();

    private final BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleParameterStatus = handleBackendMessage(ParameterStatus.class,
        (message, sink) -> this.parameterStatus.put(message.getName(), message.getValue()));

    private final AtomicReference<Integer> processId = new AtomicReference<>();

    private final EmitterProcessor<FrontendMessage> requestProcessor = EmitterProcessor.create(false);

    private final FluxSink<FrontendMessage> requests = this.requestProcessor.sink();

    private final EmitterProcessor<Flux<BackendMessage>> responseProcessor = EmitterProcessor.create(false);

    private final AtomicReference<Integer> secretKey = new AtomicReference<>();

    private final BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleBackendKeyData = handleBackendMessage(BackendKeyData.class,
        (message, sink) -> {
            this.processId.set(message.getProcessId());
            this.secretKey.set(message.getSecretKey());
        });

    private final AtomicReference<TransactionStatus> transactionStatus = new AtomicReference<>(IDLE);

    private final BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleReadyForQuery = handleBackendMessage(ReadyForQuery.class,
        (message, sink) -> {
            this.transactionStatus.set(TransactionStatus.valueOf(message.getTransactionStatus()));
            sink.next(message);
        });

    /**
     * Creates a new frame processor connected to a given host.
     *
     * @param host the host to connect to
     * @param port the port to connect to
     * @throws NullPointerException if {@code host} or {@code decoder} is {@code null}
     */
    public ReactorNettyClient(String host, int port) {
        requireNonNull(host, "host must not be null");

        BackendMessageDecoder decoder = new BackendMessageDecoder();
        FluxSink<Flux<BackendMessage>> responses = this.responseProcessor.sink();

        BlockingNettyContext nettyContext = TcpClient.create(host, port)
            .start((inbound, outbound) -> {
                this.byteBufAllocator.set(outbound.alloc());

                inbound.receive()
                    .concatMap(decoder::decode)
                    .doOnNext(message -> this.logger.debug("Response: {}", message))
                    .handle(this.handleNoticeResponse)
                    .handle(this.handleErrorResponse)
                    .handle(this.handleBackendKeyData)
                    .handle(this.handleParameterStatus)
                    .handle(this.handleReadyForQuery)
                    .windowWhile(not(ReadyForQuery.class::isInstance))
                    .subscribe(responses::next, responses::error, responses::complete);

                return this.requestProcessor
                    .doOnNext(message -> this.logger.debug("Request:  {}", message))
                    .concatMap(message -> outbound.send(message.encode(outbound.alloc())));
            });

        this.nettyContext.set(nettyContext);
    }

    @Override
    public Mono<Void> close() {
        return Mono.defer(() -> {
            BlockingNettyContext nettyContext = this.nettyContext.getAndSet(null);

            if (nettyContext == null) {
                return Mono.empty();
            }

            return TerminationMessageFlow.exchange(this)
                .doOnComplete(() -> {
                    nettyContext.shutdown();
                    this.isClosed.set(true);
                })
                .then();
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code publisher} is {@code null}
     */
    @Override
    public Flux<BackendMessage> exchange(Publisher<FrontendMessage> requests) {
        requireNonNull(requests, "requests must not be null");

        return Flux.defer(() -> {
            if (this.isClosed.get()) {
                return Flux.error(new IllegalStateException("Cannot exchange messages because the connection is closed"));
            }

            return this.responseProcessor
                .doOnSubscribe(s ->
                    Flux.from(requests)
                        .subscribe(this.requests::next, this.requests::error))
                .next()
                .flatMapMany(Function.identity());
        });
    }

    @Override
    public ByteBufAllocator getByteBufAllocator() {
        return this.byteBufAllocator.get();
    }

    @Override
    public Map<String, String> getParameterStatus() {
        return new HashMap<>(this.parameterStatus);
    }

    @Override
    public Optional<Integer> getProcessId() {
        return Optional.ofNullable(this.processId.get());
    }

    @Override
    public Optional<Integer> getSecretKey() {
        return Optional.ofNullable(this.secretKey.get());
    }

    @Override
    public TransactionStatus getTransactionStatus() {
        return this.transactionStatus.get();
    }

    @SuppressWarnings("unchecked")
    private static <T extends BackendMessage> BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleBackendMessage(Class<T> type, BiConsumer<T, SynchronousSink<BackendMessage>> consumer) {
        return (message, sink) -> {
            if (type.isInstance(message)) {
                consumer.accept((T) message, sink);
            } else {
                sink.next(message);
            }
        };
    }

    private static String toString(List<Field> fields) {
        return fields.stream()
            .map(field -> String.format("%s=%s", field.getType().name(), field.getValue()))
            .collect(Collectors.joining(", "));
    }

}
