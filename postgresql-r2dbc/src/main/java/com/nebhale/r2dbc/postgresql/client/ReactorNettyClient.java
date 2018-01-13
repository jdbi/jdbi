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

import com.nebhale.r2dbc.postgresql.ServerErrorException;
import com.nebhale.r2dbc.postgresql.message.backend.BackendKeyData;
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessageDecoder;
import com.nebhale.r2dbc.postgresql.message.backend.ErrorResponse;
import com.nebhale.r2dbc.postgresql.message.backend.Field;
import com.nebhale.r2dbc.postgresql.message.backend.NoticeResponse;
import com.nebhale.r2dbc.postgresql.message.backend.ParameterStatus;
import com.nebhale.r2dbc.postgresql.message.backend.ReadyForQuery;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SynchronousSink;
import reactor.ipc.netty.tcp.TcpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * An implementation of client based on the Reactor Netty project.
 *
 * @see TcpClient
 */
public final class ReactorNettyClient implements Client {

    private static final AtomicReferenceFieldUpdater<ReactorNettyClient, Integer> PROCESS_ID = AtomicReferenceFieldUpdater.newUpdater(ReactorNettyClient.class, Integer.class, "processId");

    private static final AtomicReferenceFieldUpdater<ReactorNettyClient, Integer> SECRET_KEY = AtomicReferenceFieldUpdater.newUpdater(ReactorNettyClient.class, Integer.class, "secretKey");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleBackendKeyData = handleBackendMessage(BackendKeyData.class, (message, sink) -> {
        PROCESS_ID.set(this, message.getProcessId());
        SECRET_KEY.set(this, message.getSecretKey());
    });

    private final BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleErrorResponse = handleBackendMessage(ErrorResponse.class, (message, sink) -> {
        this.logger.error("Error: {}", toString(message.getFields()));
        sink.error(new ServerErrorException(message.getFields()));
    });

    private final BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleNoticeResponse = handleBackendMessage(NoticeResponse.class, (message, sink) ->
        this.logger.warn("Notice: {}", toString(message.getFields())));

    private final ConcurrentMap<String, String> parameterStatus = new ConcurrentHashMap<>();

    private final BiConsumer<BackendMessage, SynchronousSink<BackendMessage>> handleParameterStatus = handleBackendMessage(ParameterStatus.class, (message, sink) ->
        this.parameterStatus.put(message.getName(), message.getValue()));

    private final EmitterProcessor<FrontendMessage> requestProcessor = EmitterProcessor.create();

    private final FluxSink<FrontendMessage> requests = this.requestProcessor.sink();

    private final EmitterProcessor<Flux<BackendMessage>> responseProcessor = EmitterProcessor.create(false);

    private volatile Integer processId;

    private volatile Integer secretKey;

    /**
     * Creates a new frame processor connected to a given host.
     *
     * @param host    the host to connect to
     * @param port    the port to connect to
     * @param decoder the decoder to use to decode {@link BackendMessage}s
     * @throws NullPointerException if {@code host} or {@code decoder} is {@code null}
     */
    public ReactorNettyClient(String host, int port, BackendMessageDecoder decoder) {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(decoder, "decoder must not be null");

        TcpClient.create(host, port)
            .start((inbound, outbound) -> {
                inbound.receive()
                    .concatMap(decoder::decode)
                    .doOnNext(message -> this.logger.debug("Response: {}", message))
                    .handle(this.handleNoticeResponse)
                    .handle(this.handleBackendKeyData)
                    .handle(this.handleParameterStatus)
                    .transform(WindowMaker.create(ReadyForQuery.class))
                    .subscribe(this.responseProcessor);

                return this.requestProcessor
                    .doOnNext(message -> this.logger.debug("Request:  {}", message))
                    .concatMap(message -> outbound.send(message.encode(outbound.alloc())));
            });
    }

    @Override
    public void close() {
        this.requestProcessor.onComplete();
        this.responseProcessor.onComplete();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code publisher} is {@code null}
     */
    @Override
    public Flux<BackendMessage> exchange(Publisher<FrontendMessage> requests) {
        Objects.requireNonNull(requests, "requests must not be null");

        return Flux.defer(() -> {
            Flux.from(requests)
                .subscribe(this.requests::next, this.requests::error);

            return this.responseProcessor
                .next()
                .flatMapMany(flux -> flux
                    .handle(this.handleErrorResponse));
        });
    }

    @Override
    public Map<String, String> getParameterStatus() {
        return new HashMap<>(this.parameterStatus);
    }

    @Override
    public Optional<Integer> getProcessId() {
        return Optional.ofNullable(PROCESS_ID.get(this));
    }

    @Override
    public Optional<Integer> getSecretKey() {
        return Optional.ofNullable(SECRET_KEY.get(this));
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
