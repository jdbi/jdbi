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
import com.nebhale.r2dbc.postgresql.message.backend.ReadyForQuery;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;

/**
 * An abstraction that wraps the networking part of exchanging methods.
 */
public interface Client {

    /**
     * Release any resources held by the {@link Client}.
     */
    void close();

    /**
     * Perform an exchange of messages.
     *
     * @param requests the publisher of outbound messages
     * @return a {@link Flux} of incoming messages that ends with the end of the frame (i.e. reception of a {@link ReadyForQuery} message.
     */
    Flux<BackendMessage> exchange(Publisher<FrontendMessage> requests);

    /**
     * Returns a snapshot of the current parameter statuses.
     *
     * @return a snapshot of the current parameter statuses
     */
    Map<String, String> getParameterStatus();

    /**
     * Returns the connected process id if it has been communicated.
     *
     * @return the connected process id if it has been communicated
     */
    Optional<Integer> getProcessId();

    /**
     * Returns the connected process secret key if it has been communicated.
     *
     * @return the connected process secret key if it has been communicated
     */
    Optional<Integer> getSecretKey();

}
