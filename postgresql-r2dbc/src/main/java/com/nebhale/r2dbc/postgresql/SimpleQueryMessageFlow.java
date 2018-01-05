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
import com.nebhale.r2dbc.postgresql.message.frontend.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

final class SimpleQueryMessageFlow {

    private SimpleQueryMessageFlow() {
    }

    static Flux<BackendMessage> exchange(Client client, String query) {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(query, "query must not be null");

        return client.exchange(Mono.just(new Query(query)));
    }

}
