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

package com.nebhale.r2dbc.examples;

import com.nebhale.r2dbc.postgresql.PostgresqlConnection;
import com.nebhale.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import com.nebhale.r2dbc.postgresql.PostgresqlConnectionFactory;
import com.nebhale.r2dbc.postgresql.PostgresqlResult;
import com.nebhale.r2dbc.postgresql.PostgresqlServerErrorException;
import com.nebhale.r2dbc.postgresql.PostgresqlStatement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static com.nebhale.r2dbc.spi.Mutability.READ_ONLY;

public final class SpiExamples {

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

    @Test
    public void batch() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        this.connectionFactory.create()
            .flatMapMany(connection -> connection

                .createBatch()
                .add("INSERT INTO test VALUES(200)")
                .add("SELECT value FROM test")
                .execute()

                .concatWith(close(connection)))
            .as(StepVerifier::create)
            .expectNextCount(3)  // TODO: Decrease by 1 when https://github.com/reactor/reactor-core/issues/1033
            .verifyComplete();
    }

    @Before
    public void cleanTable() {
        SERVER.getJdbcOperations().execute("DELETE FROM test");
    }

    @Test
    public void compoundStatement() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        this.connectionFactory.create()
            .flatMapMany(connection -> connection

                .createStatement("SELECT value FROM test; SELECT value FROM test")
                .execute()
                .flatMap(SpiExamples::extractColumns)

                .concatWith(close(connection)))
            .as(StepVerifier::create)
            .expectNext(Collections.singletonList(100))
            .expectNext(Collections.singletonList(100))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .verifyComplete();
    }

    @Test
    public void connectionMutability() {
        this.connectionFactory.create()
            .flatMapMany(connection -> connection

                .setTransactionMutability(READ_ONLY)
                .thenMany(connection.createStatement("INSERT INTO test VALUES ($1)")
                    .bind("$1", 100)
                    .execute()
                    .flatMap(SpiExamples::extractRowsUpdated))

                .concatWith(close(connection)))
            .as(StepVerifier::create)
            .verifyError(PostgresqlServerErrorException.class);
    }

    @Test
    public void parameterStatusConnection() {
        this.connectionFactory.create()
            .flatMapMany(connection -> Mono.just(

                connection.getParameterStatus())

                .delayUntil(m -> connection.createStatement("SET application_name TO 'test-application'")
                    .execute())

                .concatWith(Flux.defer(() -> Flux.just(connection.getParameterStatus())))

                .concatWith(close(connection)))
            .map(m -> m.get("application_name"))
            .as(StepVerifier::create)
            .expectNext("postgresql-r2dbc")
            .expectNext("test-application")
            .verifyComplete();
    }

    @Test
    public void prepareStatement() {
        this.connectionFactory.create()
            .flatMapMany(connection -> {
                PostgresqlStatement statement = connection.createStatement("INSERT INTO test VALUES($1)");

                IntStream.range(0, 10)
                    .forEach(i -> statement
                        .bind("$1", i)
                        .add());

                return statement
                    .execute()
                    .concatWith(close(connection));
            })
            .as(StepVerifier::create)
            .expectNextCount(11) // TODO: Decrease by 1 when https://github.com/reactor/reactor-core/issues/1033
            .verifyComplete();
    }

    @Test
    public void savePoint() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        this.connectionFactory.create()
            .flatMapMany(connection -> connection

                .beginTransaction()
                .<Object>thenMany(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(connection.createStatement("INSERT INTO test VALUES ($1)")
                    .bind("$1", 200)
                    .execute()
                    .flatMap(SpiExamples::extractRowsUpdated))
                .concatWith(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(connection.createSavepoint("test_savepoint"))
                .concatWith(connection.createStatement("INSERT INTO test VALUES ($1)")
                    .bind("$1", 300)
                    .execute()
                    .flatMap(SpiExamples::extractRowsUpdated))
                .concatWith(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(connection.rollbackTransactionToSavepoint("test_savepoint"))
                .concatWith(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(close(connection)))
            .as(StepVerifier::create)
            .expectNext(Collections.singletonList(100))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .expectNext(1)
            .expectNext(Arrays.asList(100, 200))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .expectNext(1)
            .expectNext(Arrays.asList(100, 200, 300))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .expectNext(Arrays.asList(100, 200))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .verifyComplete();
    }

    @Test
    public void transactionCommit() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        this.connectionFactory.create()
            .flatMapMany(connection -> connection

                .beginTransaction()
                .<Object>thenMany(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(connection.createStatement("INSERT INTO test VALUES ($1)")
                    .bind("$1", 200)
                    .execute()
                    .flatMap(SpiExamples::extractRowsUpdated))
                .concatWith(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(connection.commitTransaction())
                .concatWith(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(close(connection)))
            .as(StepVerifier::create)
            .expectNext(Collections.singletonList(100))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .expectNext(1)
            .expectNext(Arrays.asList(100, 200))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .expectNext(Arrays.asList(100, 200))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .verifyComplete();
    }

    @Test
    public void transactionMutability() {
        this.connectionFactory.create()
            .flatMapMany(connection -> connection

                .beginTransaction()

                .then(connection.setTransactionMutability(READ_ONLY))
                .thenMany(connection.createStatement("INSERT INTO test VALUES ($1)")
                    .bind("$1", 200)
                    .execute()
                    .flatMap(SpiExamples::extractRowsUpdated))

                .concatWith(close(connection)))
            .as(StepVerifier::create)
            .verifyError(PostgresqlServerErrorException.class);
    }

    @Test
    public void transactionRollback() {
        SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        this.connectionFactory.create()
            .flatMapMany(connection -> connection

                .beginTransaction()
                .<Object>thenMany(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(connection.createStatement("INSERT INTO test VALUES ($1)")
                    .bind("$1", 200)
                    .execute()
                    .flatMap(SpiExamples::extractRowsUpdated))
                .concatWith(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(connection.rollbackTransaction())
                .concatWith(connection.createStatement("SELECT value FROM test")
                    .execute()
                    .flatMap(SpiExamples::extractColumns))

                .concatWith(close(connection)))
            .as(StepVerifier::create)
            .expectNext(Collections.singletonList(100))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .expectNext(1)
            .expectNext(Arrays.asList(100, 200))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .expectNext(Collections.singletonList(100))
            .expectNextCount(1)  // TODO: Remove when https://github.com/reactor/reactor-core/issues/1033
            .verifyComplete();
    }

    private static <T> Mono<T> close(PostgresqlConnection connection) {
        return connection
            .close()
            .then(Mono.empty());
    }

    private static Mono<List<Integer>> extractColumns(PostgresqlResult result) {
        return result
            .getRows()
            .map(r -> r.getColumns().get(0).getInteger())
            .collectList();
    }

    private static Mono<Integer> extractRowsUpdated(PostgresqlResult result) {
        return result.getRowsUpdated();
    }

}
