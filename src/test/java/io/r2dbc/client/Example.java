/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.client;

import io.r2dbc.spi.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

interface Example<T> {

    static Mono<List<Integer>> extractColumns(Result result) {
        return Flux.from(result
            .map((row, rowMetadata) -> row.get("value", Integer.class)))
            .collectList();
    }

    static Mono<List<Integer>> extractIds(Result result) {
        return Flux.from(result
            .map((row, rowMetadata) -> row.get("id", Integer.class)))
            .collectList();
    }

    @Test
    default void batch() {
        getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        getR2dbc()
            .withHandle(handle -> handle

                .createBatch()
                .add("INSERT INTO test VALUES(200)")
                .add("SELECT value FROM test")
                .mapResult(Mono::just))

            .as(StepVerifier::create)
            .expectNextCount(2).as("one result for each statement")
            .verifyComplete();
    }

    @Test
    default void compoundStatement() {
        getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        getR2dbc()
            .withHandle(handle -> handle

                .createQuery("SELECT value FROM test; SELECT value FROM test")
                .mapResult(Example::extractColumns))

            .as(StepVerifier::create)
            .expectNext(Collections.singletonList(100)).as("value from first statement")
            .expectNext(Collections.singletonList(100)).as("value from second statement")
            .verifyComplete();
    }

    @BeforeEach
    default void createTable() {
        getJdbcOperations().execute("CREATE TABLE test ( value INTEGER )");
    }

    @AfterEach
    default void dropTable() {
        getJdbcOperations().execute("DROP TABLE test");
    }

    /**
     * Returns the bind identifier for a given substitution.
     *
     * @param index the zero-index number of the substitution
     * @return the bind identifier for a given substitution
     */
    T getIdentifier(int index);

    /**
     * Returns a {@link JdbcOperations} for the connected database.
     *
     * @return a {@link JdbcOperations} for the connected database
     */
    JdbcOperations getJdbcOperations();

    /**
     * Returns the database-specific placeholder for a given substitution.
     *
     * @param index the zero-index number of the substitution
     * @return the database-specific placeholder for a given substitution
     */
    String getPlaceholder(int index);

    /**
     * Returns a {@link R2dbc} for the connected database.
     *
     * @return a {@link R2dbc} for the connected database
     */
    R2dbc getR2dbc();

    @Test
    default void prepareStatement() {
        getR2dbc()
            .withHandle(handle -> {
                Update update = handle.createUpdate(String.format("INSERT INTO test VALUES(%s)", getPlaceholder(0)));

                IntStream.range(0, 10)
                    .forEach(i -> update.bind(getIdentifier(0), i).add());

                return update.execute();
            })
            .as(StepVerifier::create)
            .expectNextCount(10).as("values from insertions")
            .verifyComplete();
    }

    @Test
    default void savePoint() {
        getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        getR2dbc()
            .withHandle(handle -> handle
                .inTransaction(h1 -> h1
                    .select("SELECT value FROM test")
                    .<Object>mapResult(Example::extractColumns)

                    .concatWith(h1.execute(String.format("INSERT INTO test VALUES (%s)", getPlaceholder(0)), 200))
                    .concatWith(h1.select("SELECT value FROM test")
                        .mapResult(Example::extractColumns))

                    .concatWith(h1.createSavepoint("test_savepoint"))
                    .concatWith(h1.execute(String.format("INSERT INTO test VALUES (%s)", getPlaceholder(0)), 300))
                    .concatWith(h1.select("SELECT value FROM test")
                        .mapResult(Example::extractColumns))

                    .concatWith(h1.rollbackTransactionToSavepoint("test_savepoint"))
                    .concatWith(h1.select("SELECT value FROM test")
                        .mapResult(Example::extractColumns))))

            .as(StepVerifier::create)
            .expectNext(Collections.singletonList(100)).as("value from select")
            .expectNext(1).as("rows inserted")
            .expectNext(Arrays.asList(100, 200)).as("values from select")
            .expectNext(1).as("rows inserted")
            .expectNext(Arrays.asList(100, 200, 300)).as("values from select")
            .expectNext(Arrays.asList(100, 200)).as("values from select")
            .verifyComplete();
    }

    @Test
    default void transactionCommit() {
        getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        getR2dbc()
            .withHandle(handle -> handle
                .inTransaction(h1 -> h1
                    .select("SELECT value FROM test")
                    .<Object>mapResult(Example::extractColumns)

                    .concatWith(h1.execute(String.format("INSERT INTO test VALUES (%s)", getPlaceholder(0)), 200))
                    .concatWith(h1.select("SELECT value FROM test")
                        .mapResult(Example::extractColumns)))

                .concatWith(handle.select("SELECT value FROM test")
                    .mapResult(Example::extractColumns)))

            .as(StepVerifier::create)
            .expectNext(Collections.singletonList(100)).as("value from select")
            .expectNext(1).as("rows inserted")
            .expectNext(Arrays.asList(100, 200)).as("values from select")
            .expectNext(Arrays.asList(100, 200)).as("values from select")
            .verifyComplete();
    }

    @Test
    default void transactionRollback() {
        getJdbcOperations().execute("INSERT INTO test VALUES (100)");

        getR2dbc()
            .withHandle(handle -> handle
                .inTransaction(h1 -> h1
                    .select("SELECT value FROM test")
                    .<Object>mapResult(Example::extractColumns)

                    .concatWith(h1.execute(String.format("INSERT INTO test VALUES (%s)", getPlaceholder(0)), 200))
                    .concatWith(h1.select("SELECT value FROM test")
                        .mapResult(Example::extractColumns))

                    .concatWith(Mono.error(new Exception())))

                .onErrorResume(t -> handle.select("SELECT value FROM test")
                    .mapResult(Example::extractColumns)))

            .as(StepVerifier::create)
            .expectNext(Collections.singletonList(100)).as("value from select")
            .expectNext(1).as("rows inserted")
            .expectNext(Arrays.asList(100, 200)).as("values from select")
            .expectNext(Collections.singletonList(100)).as("value from select")
            .verifyComplete();
    }

}
