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

import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.nebhale.r2dbc.IsolationLevel.READ_UNCOMMITTED;
import static com.nebhale.r2dbc.Mutability.READ_ONLY;

public class Examples {

    private final PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
        .applicationName("test-application-name")
        .database("test_database")
        .host("localhost")
        .password("test_password")
        .username("test_user")
        .build();

    private final PostgresqlConnectionFactory connectionFactory = new PostgresqlConnectionFactory(this.configuration);

    @Test
    public void query() {
        this.connectionFactory.create()
            .flatMapMany(connection -> connection
                .query("SELECT * FROM test_table; SELECT * FROM test_table;")
                .concatMap(Examples::printValues)
                .doOnTerminate(connection::close))
            .then()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void savePoint() {
        this.connectionFactory.create()
            .flatMapMany(connection -> connection
                .withTransaction(transaction ->
                    transaction.createSavepoint("foo")
                        .thenMany(transaction.query("INSERT INTO test_table(id) VALUES(200)")
                            .thenMany(transaction.query("SELECT * FROM test_table")
                                .concatMap(Examples::printValues)))
                        .thenEmpty(transaction.rollbackToSavepoint("foo"))
                        .thenMany(transaction.query("SELECT * FROM test_table")
                            .concatMap(Examples::printValues))
                        .then())
                .doOnTerminate(connection::close))
            .then()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    public void transactionAutomatic() {
        this.connectionFactory.create()
            .flatMapMany(connection -> connection
                .withTransaction(transaction ->
                    transaction.query("INSERT INTO test_table(id) VALUES(200)")
                        .thenMany(transaction.query("SELECT * FROM test_table")
                            .concatMap(Examples::printValues))
                        .then(Mono.error(new Exception())))
                .thenMany(connection.query("SELECT * FROM test_table")
                    .concatMap(Examples::printValues))
                .doOnTerminate(connection::close))
            .then()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Ignore("Not a valid test yet, because frame delimiting with server errors doesn't work")
    @Test
    public void transactionIsolation() {
        this.connectionFactory.create()
            .flatMapMany(connection -> connection
                .withTransaction(transaction ->
                    transaction.setMutability(READ_ONLY)
                        .thenEmpty(transaction.setIsolationLevel(READ_UNCOMMITTED))
                        .thenMany(transaction.query("INSERT INTO test_table(id) VALUES(200)"))
                        .then())
                .doOnTerminate(connection::close))
            .then()
            .as(StepVerifier::create)
            .verifyError(ServerErrorException.class);
    }

    @Ignore("Not a valid test yet, because frame delimiting with server errors doesn't work")
    @Test
    public void transactionIsolationDefault() {
        this.connectionFactory.create()
            .flatMapMany(connection -> connection
                .setMutability(READ_ONLY)
                .thenEmpty(connection.setIsolationLevel(READ_UNCOMMITTED))
                .thenMany(connection.query("INSERT INTO test_table(id) VALUES(200)")))
            .then()
            .as(StepVerifier::create)
            .verifyError(ServerErrorException.class);
    }

    @Test
    public void transactionManual() {
        this.connectionFactory.create()
            .flatMapMany(connection ->
                connection.begin()
                    .flatMap(transaction ->
                        transaction.query("INSERT INTO test_table(id) VALUES(200)")
                            .thenMany(connection.query("SELECT * FROM test_table")
                                .concatMap(Examples::printValues))
                            .thenEmpty(transaction.rollback()))
                    .thenMany(connection.query("SELECT * FROM test_table")
                        .concatMap(Examples::printValues))
                    .doOnTerminate(connection::close))
            .then()
            .as(StepVerifier::create)
            .verifyComplete();
    }

    private static Publisher<PostgresqlRow> printValues(Flux<PostgresqlRow> publisher) {
        return publisher
            .doOnNext(row -> System.out.printf("ROW VALUE: %d%n", row.getInteger(0)));
    }

}
