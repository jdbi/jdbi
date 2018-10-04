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

package io.r2dbc.client;

import static io.r2dbc.spi.Mutability.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.postgresql.PostgresqlServerErrorException;
import io.r2dbc.spi.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class H2Examples {

	private static final String DATABASE_NAME = "mem:r2dbc-examples";
	private static final String JDBC_CONNECTION_URL = "jdbc:h2:" + DATABASE_NAME;

	@RegisterExtension
	static final H2ServerExtension SERVER = new H2ServerExtension(JDBC_CONNECTION_URL);

	private final H2ConnectionFactory connectionFactory = new H2ConnectionFactory(Mono.defer(() -> Mono.just(DATABASE_NAME)));

	private final R2dbc r2dbc = new R2dbc(connectionFactory);

	@BeforeEach
	void createTable() {
		SERVER.getJdbcOperations().execute("CREATE TABLE test ( value INTEGER )");
	}

	@AfterEach
	void dropTable() {
		SERVER.getJdbcOperations().execute("DROP TABLE test");
	}
	
	@Test
	void batch() {

		SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

		this.r2dbc
			.withHandle(handle -> handle

				.createBatch()
				.add("INSERT INTO test VALUES(200)")
				.add("SELECT value FROM test")
				.mapResult(Mono::just))

			.as(StepVerifier::create)
			.expectNextCount(2)
			.verifyComplete();
	}

	@Test
	void compoundStatement() {

		SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

		this.r2dbc
			.withHandle(handle -> handle

				.createQuery("SELECT value FROM test; SELECT value FROM test")
				.mapResult(H2Examples::extractColumns))

			.as(StepVerifier::create)
			.expectNext(Collections.singletonList(100))
			.expectNext(Collections.singletonList(100))
			.verifyComplete();
	}

	@Test
	@Disabled("H2 support for setTransactionMutability not yet implemented.")
	void connectionMutability() {

		this.r2dbc
			.useHandle(handle -> Mono.from(handle

				.setTransactionMutability(READ_ONLY))
				.thenMany(handle.execute("INSERT INTO test VALUES ($1)", 200)))

			.as(StepVerifier::create)
			.verifyError(PostgresqlServerErrorException.class);
	}

	@Test
	void generatedKeys() {

		SERVER.getJdbcOperations().execute("CREATE TABLE test2 (id SERIAL PRIMARY KEY, value INTEGER)");

		this.r2dbc
			.withHandle(handle -> handle

				.createUpdate("INSERT INTO test2(value) VALUES ($1)")
				.bind("$1", 100)
				.add()
				.bind("$1", 200)
				.add()
				.executeReturningGeneratedKeys()
				.flatMap(resultBearing -> resultBearing
					.mapResult(H2Examples::extractIds)))

			.as(StepVerifier::create)
			.expectNext(Collections.singletonList(1))
			.expectNext(Collections.singletonList(2))
			.verifyComplete();
	}

	@Test
	void prepareStatement() {

		this.r2dbc
			.withHandle(handle -> {
				Update update = handle.createUpdate("INSERT INTO test VALUES($1)");

				IntStream.range(0, 10)
					.forEach(i -> update
						.bind("$1", i)
						.add());

				return update.execute();
			})
			.as(StepVerifier::create)
			.expectNextCount(10)
			.verifyComplete();
	}

	@Test
	void savePoint() {

		SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

		this.r2dbc
			.withHandle(handle -> handle
				.inTransaction(h1 -> h1
					.select("SELECT value FROM test")
					.<Object> mapResult(H2Examples::extractColumns)

					.concatWith(h1.execute("INSERT INTO test VALUES ($1)", 200))
					.concatWith(h1.select("SELECT value FROM test")
						.mapResult(H2Examples::extractColumns))

					.concatWith(h1.createSavepoint("test_savepoint"))
					.concatWith(h1.execute("INSERT INTO test VALUES ($1)", 300))
					.concatWith(h1.select("SELECT value FROM test")
						.mapResult(H2Examples::extractColumns))

					.concatWith(h1.rollbackTransactionToSavepoint("test_savepoint"))
					.concatWith(h1.select("SELECT value FROM test")
						.mapResult(H2Examples::extractColumns))))

			.as(StepVerifier::create)
			.expectNext(Collections.singletonList(100))
			.expectNextCount(1)
			.expectNext(Arrays.asList(100, 200))
			.expectNextCount(1)
			.expectNext(Arrays.asList(100, 200, 300))
			.expectNext(Arrays.asList(100, 200))
			.verifyComplete();
	}

	@Test
	void transactionCommit() {

		SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

		this.r2dbc
			.withHandle(handle -> handle
				.inTransaction(h1 -> h1
					.select("SELECT value FROM test")
					.<Object> mapResult(H2Examples::extractColumns)

					.concatWith(h1.execute("INSERT INTO test VALUES ($1)", 200))
					.concatWith(h1.select("SELECT value FROM test")
						.mapResult(H2Examples::extractColumns)))

				.concatWith(handle.select("SELECT value FROM test")
					.mapResult(H2Examples::extractColumns)))

			.as(StepVerifier::create)
			.expectNext(Collections.singletonList(100))
			.expectNextCount(1)
			.expectNext(Arrays.asList(100, 200))
			.expectNext(Arrays.asList(100, 200))
			.verifyComplete();
	}

	@Test
	@Disabled("H2 support for setTransactionMutability not yet implemented.")
	void transactionMutability() {

		this.r2dbc
			.inTransaction(handle -> Mono.from(handle

				.setTransactionMutability(READ_ONLY))
				.thenMany(handle.execute("INSERT INTO test VALUES ($1)", 200)))

			.as(StepVerifier::create)
			.verifyError(PostgresqlServerErrorException.class);
	}

	@Test
	void transactionRollback() {

		SERVER.getJdbcOperations().execute("INSERT INTO test VALUES (100)");

		this.r2dbc
			.withHandle(handle -> handle
				.inTransaction(h1 -> h1
					.select("SELECT value FROM test")
					.<Object> mapResult(H2Examples::extractColumns)

					.concatWith(h1.execute("INSERT INTO test VALUES ($1)", 200))
					.concatWith(h1.select("SELECT value FROM test")
						.mapResult(H2Examples::extractColumns))

					.concatWith(Mono.error(new Exception())))

				.onErrorResume(t -> handle.select("SELECT value FROM test")
					.mapResult(H2Examples::extractColumns)))

			.as(StepVerifier::create)
			.expectNext(Collections.singletonList(100))
			.expectNextCount(1)
			.expectNext(Arrays.asList(100, 200))
			.expectNext(Collections.singletonList(100))
			.verifyComplete();
	}

	private static Mono<List<Integer>> extractColumns(Result result) {

		return Flux.from(result
			.map((row, rowMetadata) -> row.get("value", Integer.class)))
			.collectList();
	}

	private static Mono<List<Integer>> extractIds(Result result) {

		return Flux.from(result
			.map((row, rowMetadata) -> row.get("id", Integer.class)))
			.collectList();
	}
}
