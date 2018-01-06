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
import com.nebhale.r2dbc.postgresql.message.backend.BackendMessageDecoder;
import com.nebhale.r2dbc.postgresql.message.backend.ErrorResponse;
import com.nebhale.r2dbc.postgresql.message.backend.Field;
import com.nebhale.r2dbc.postgresql.message.backend.NoticeResponse;
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.nebhale.r2dbc.postgresql.Util.not;

final class TcpClientClient implements Client {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final EmitterProcessor<FrontendMessage> requestProcessor = EmitterProcessor.create();

    private final FluxSink<FrontendMessage> requests = this.requestProcessor.sink();

    private final EmitterProcessor<Flux<BackendMessage>> responseProcessor = EmitterProcessor.create();

    private final Flux<Flux<BackendMessage>> responses = this.responseProcessor.publish().autoConnect();

    /**
     * Creates a new frame processor connected to a given host.
     *
     * @param host    the host to connect to
     * @param port    the port to connect to
     * @param decoder the decoder to use to decode {@link BackendMessage}s
     * @throws NullPointerException if {@code host} or {@code decoder} is {@code null}
     */
    TcpClientClient(String host, int port, BackendMessageDecoder decoder) {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(decoder, "decoder must not be null");

        TcpClient.create(host, port)
            .start((inbound, outbound) -> {
                inbound.receive()
                    .concatMap(decoder::decode)
                    .doOnNext(message -> this.logger.debug("Response: {}", message))
                    .windowWhile(not(ReadyForQuery.class::isInstance))
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
    public Flux<BackendMessage> exchange(Publisher<FrontendMessage> publisher) {
        Objects.requireNonNull(publisher, "publisher must not be null");

        return Flux.defer(() -> {
            Flux.from(publisher)
                .subscribe(this.requests::next, this.requests::error);

            return this.responses
                .next()
                .flatMapMany(flux -> flux
                    .handle(this::handleWarningsAndErrors));
        });
    }

    private static String toString(List<Field> fields) {
        return fields.stream()
            .map(field -> String.format("%s=%s", field.getType().name(), field.getValue()))
            .collect(Collectors.joining(", "));
    }

    private void handleWarningsAndErrors(BackendMessage message, SynchronousSink<BackendMessage> sink) {
        if (message instanceof NoticeResponse) {
            NoticeResponse noticeResponse = (NoticeResponse) message;
            this.logger.warn("Notice: {}", toString(noticeResponse.getFields()));
        } else if (message instanceof ErrorResponse) {
            ErrorResponse errorResponse = (ErrorResponse) message;
            this.logger.error("Error: {}", toString(errorResponse.getFields()));

            sink.error(new ServerErrorException(errorResponse.getFields()));
        } else {
            sink.next(message);
        }
    }
}
