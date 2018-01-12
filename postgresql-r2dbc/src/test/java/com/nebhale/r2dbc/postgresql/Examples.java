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

import com.nebhale.r2dbc.postgresql.client.PostgresqlServerResource;
import com.nebhale.r2dbc.postgresql.client.WindowCollector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.nebhale.r2dbc.Mutability.READ_ONLY;

public class Examples {

    @ClassRule
    public static final PostgresqlServerResource SERVER = new PostgresqlServerResource();

    private final PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
        .database(SERVER.getDatabase())
        .host(SERVER.getHost())
        .port(SERVER.getPort())
        .password(SERVER.getPassword())
        .username(SERVER.getUsername())
        .build();

    private final PostgresqlConnectionFactory connectionFactory = new PostgresqlConnectionFactory(this.configuration);

    @BeforeClass
    public static void createSchema() {
        SERVER.getJdbcOperations().execute("CREATE TABLE test ( value INTEGER )");
    }

    @Before
    public void cleanTable() {
        SERVER.getJdbcOperations().execute("DELETE FROM test");
    }

    @Test
    public void query() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        WindowCollector<PostgresqlRow> windows = new WindowCollector<>();

        this.connectionFactory.create()
            .flatMapMany(connection ->
                connection.query("SELECT value FROM test; SELECT value FROM test"))
            .as(StepVerifier::create)
            .recordWith(windows)
            .expectNextCount(2)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();
    }

    @Test
    public void savePoint() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        WindowCollector<PostgresqlRow> windows = new WindowCollector<>();

        this.connectionFactory.create()
            .flatMapMany(connection ->
                connection.begin()
                    .flatMapMany(transaction ->
                        transaction.query("SELECT value FROM test")
                            .concatWith(transaction.query("INSERT INTO test VALUES (200)"))
                            .concatWith(transaction.query("SELECT value FROM test"))
                            .concatWith(transaction.createSavepoint("test_savepoint"))
                            .concatWith(transaction.query("INSERT INTO test VALUES (300)"))
                            .concatWith(transaction.query("SELECT value FROM test"))
                            .concatWith(transaction.rollbackToSavepoint("test_savepoint"))
                            .concatWith(transaction.query("SELECT value FROM test"))))
            .as(StepVerifier::create)
            .recordWith(windows)
            .expectNextCount(4)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .expectNext(200)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .expectNext(200)
            .expectNext(300)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .expectNext(200)
            .verifyComplete();
    }

    @Test
    public void transactionAutomaticCommit() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        WindowCollector<PostgresqlRow> windows = new WindowCollector<>();

        this.connectionFactory.create()
            .flatMapMany(connection ->
                connection.withTransaction(transaction ->
                    transaction.query("SELECT value FROM test")
                        .concatWith(transaction.query("INSERT INTO test VALUES (200)"))
                        .concatWith(transaction.query("SELECT value FROM test")))
                    .concatWith(connection.query("SELECT value FROM test")))
            .as(StepVerifier::create)
            .recordWith(windows)
            .expectNextCount(3)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .expectNext(200)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .expectNext(200)
            .verifyComplete();
    }

    @Test
    public void transactionAutomaticRollback() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        WindowCollector<PostgresqlRow> windows = new WindowCollector<>();

        this.connectionFactory.create()
            .flatMapMany(connection ->
                connection.withTransaction(transaction ->
                    transaction.query("SELECT value FROM test")
                        .concatWith(transaction.query("INSERT INTO test VALUES (200)"))
                        .concatWith(transaction.query("SELECT value FROM test"))
                        .concatWith(Mono.error(new Exception())))
                    .onErrorResume(t -> connection.query("SELECT value FROM test")))
            .as(StepVerifier::create)
            .recordWith(windows)
            .expectNextCount(3)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .expectNext(200)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();
    }

    @Test
    public void transactionManualCommit() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        WindowCollector<PostgresqlRow> windows = new WindowCollector<>();

        this.connectionFactory.create()
            .flatMapMany(connection ->
                connection.begin()
                    .flatMapMany(transaction ->
                        transaction.query("SELECT value FROM test")
                            .concatWith(transaction.query("INSERT INTO test VALUES (200)"))
                            .concatWith(transaction.query("SELECT value FROM test"))
                            .concatWith(transaction.commit()))
                    .concatWith(connection.query("SELECT value FROM test"))
            )
            .as(StepVerifier::create)
            .recordWith(windows)
            .expectNextCount(3)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .expectNext(200)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .expectNext(200)
            .verifyComplete();
    }

    @Test
    public void transactionManualRollback() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        WindowCollector<PostgresqlRow> windows = new WindowCollector<>();

        this.connectionFactory.create()
            .flatMapMany(connection ->
                connection.begin()
                    .flatMapMany(transaction ->
                        transaction.query("SELECT value FROM test")
                            .concatWith(transaction.query("INSERT INTO test VALUES (200)"))
                            .concatWith(transaction.query("SELECT value FROM test"))
                            .concatWith(transaction.rollback()))
                    .concatWith(connection.query("SELECT value FROM test")))
            .as(StepVerifier::create)
            .recordWith(windows)
            .expectNextCount(3)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .expectNext(200)
            .verifyComplete();

        windows.next()
            .map(row -> row.getInteger(0))
            .as(StepVerifier::create)
            .expectNext(100)
            .verifyComplete();
    }

    @Test
    public void transactionMutability() {
        this.connectionFactory.create()
            .flatMapMany(connection ->
                connection.withTransaction(transaction ->
                    transaction.setMutability(READ_ONLY)
                        .concatWith(transaction.query("INSERT INTO test VALUES (200)"))))
            .as(StepVerifier::create)
            .verifyError(ServerErrorException.class);
    }

    @Test
    public void transactionMutabilityConnection() {
        this.connectionFactory.create()
            .flatMapMany(connection ->
                connection.setMutability(READ_ONLY)
                    .concatWith(connection.query("INSERT INTO test VALUES (200)")))
            .as(StepVerifier::create)
            .verifyError(ServerErrorException.class);
    }

}
