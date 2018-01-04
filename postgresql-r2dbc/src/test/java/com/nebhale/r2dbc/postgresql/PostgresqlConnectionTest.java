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

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class PostgresqlConnectionTest {

    private final PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
        .applicationName("test-application-name")
        .database("test_database")
        .host("localhost")
        .password("test_password")
        .username("test_user")
        .build();

    private final PostgresqlConnectionFactory connectionFactory = new PostgresqlConnectionFactory(this.configuration);

    @Test
    public void test() {
        Flux.from(this.connectionFactory.create())
            .log("stream.connection")
            .flatMap(connection ->
                Flux.from(connection.query("SELECT * FROM test_table"))
                    .concatMap(publisher -> Flux.from(publisher)
                        .map(row -> row.getInteger(0))
                        .log("stream.query.1"))
                    .thenMany(connection.query("SELECT * FROM test_table"))
                    .concatMap(publisher -> Flux.from(publisher)
                        .map(row -> row.getInteger(0))
                        .log("stream.query.2"))
                    .then())
            .as(StepVerifier::create)
            .verifyComplete();
    }

}
