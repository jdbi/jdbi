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
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.nio.channels.SelectableChannel;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ClientAssert {

    private final Function<Client, Flux<BackendMessage>> clientUser;

    private Consumer<StepVerifier.FirstStep<BackendMessage>> emissionVerifier = v -> {
    };

    private Consumer<StepVerifier.FirstStep<FrontendMessage>> requestVerifier = v -> {
    };

    private Flux<BackendMessage> serverMessages = Flux.empty();

    private ClientAssert(Function<Client, Flux<BackendMessage>> clientUser) {
        this.clientUser = Objects.requireNonNull(clientUser);
    }

    static ClientAssert assertThat(Function<Client, Flux<BackendMessage>> clientUser) {
        return new ClientAssert(clientUser);
    }

    ClientAssert andEmits(Consumer<StepVerifier.FirstStep<BackendMessage>> emissionVerifier) {
        this.emissionVerifier = Objects.requireNonNull(emissionVerifier);
        return this;
    }

    ClientAssert makesRequests(Consumer<StepVerifier.FirstStep<FrontendMessage>> requestVerifier) {
        this.requestVerifier = Objects.requireNonNull(requestVerifier);
        return this;
    }

    ClientAssert receivingServerMessages(BackendMessage... serverMessages) {
        this.serverMessages = Flux.just(Objects.requireNonNull(serverMessages));
        return this;
    }

    @SuppressWarnings("unchecked")
    void verify() {
        Client client = mock(Client.class, RETURNS_SMART_NULLS);
        ArgumentCaptor<Publisher<FrontendMessage>> requests = ArgumentCaptor.forClass(Publisher.class);

        when(client.exchange(requests.capture())).thenReturn(this.serverMessages);

        Flux<BackendMessage> emissions = this.clientUser.apply(client);

        this.emissionVerifier.accept(StepVerifier.create(emissions));
        this.requestVerifier.accept(StepVerifier.create(requests.getValue()));
    }

}
