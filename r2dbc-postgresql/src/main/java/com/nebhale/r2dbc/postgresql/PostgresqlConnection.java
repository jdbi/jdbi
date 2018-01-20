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

import com.nebhale.r2dbc.Connection;
import com.nebhale.r2dbc.IsolationLevel;
import com.nebhale.r2dbc.Mutability;
import com.nebhale.r2dbc.postgresql.client.Client;
import com.nebhale.r2dbc.postgresql.client.PortalNameSupplier;
import com.nebhale.r2dbc.postgresql.client.SimpleQueryMessageFlow;
import com.nebhale.r2dbc.postgresql.client.TransactionStatus;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.nebhale.r2dbc.postgresql.client.TransactionStatus.IDLE;
import static com.nebhale.r2dbc.postgresql.client.TransactionStatus.OPEN;

/**
 * An implementation of {@link Connection} for connecting to a PostgreSQL database.
 */
public final class PostgresqlConnection implements Connection {

    private final Client client;

    private final PortalNameSupplier portalNameSupplier;

    private final StatementCache statementCache;

    PostgresqlConnection(Client client, PortalNameSupplier portalNameSupplier, StatementCache statementCache) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.portalNameSupplier = Objects.requireNonNull(portalNameSupplier, "portalNameSupplier must not be null");
        this.statementCache = Objects.requireNonNull(statementCache, "statementCache must not be null");
    }

    @Override
    public Mono<Void> beginTransaction() {
        return assertTransactionStatus(IDLE::equals, () -> "Connection must not have an open transaction in order to open a transaction")
            .thenMany(SimpleQueryMessageFlow.exchange(this.client, "BEGIN"))
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then();
    }

    @Override
    public Mono<Void> close() {
        return this.client.close()
            .then(Mono.empty());
    }

    @Override
    public Mono<Void> commitTransaction() {
        return assertTransactionStatus(OPEN::equals, () -> "Connection must have an open transaction in order to commit a transaction")
            .thenMany(SimpleQueryMessageFlow.exchange(this.client, "COMMIT"))
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then();
    }

    @Override
    public PostgresqlBatch createBatch() {
        return new PostgresqlBatch(this.client);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @Override
    public Mono<Void> createSavepoint(String name) {
        Objects.requireNonNull(name, "name must not be null");

        return assertTransactionStatus(OPEN::equals, () -> String.format("Connection must have an open transaction in order to create the %s savepoint", name))
            .thenMany(SimpleQueryMessageFlow.exchange(this.client, String.format("SAVEPOINT %s", name)))
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code sql} is {@code null}
     */
    @Override
    public PostgresqlStatement createStatement(String sql) {
        Objects.requireNonNull(sql, "sql must not be null");

        if (SimpleQueryPostgresqlStatement.supports(sql)) {
            return new SimpleQueryPostgresqlStatement(this.client, sql);
        } else if (ExtendedQueryPostgresqlStatement.supports(sql)) {
            return new ExtendedQueryPostgresqlStatement(this.client, this.portalNameSupplier, sql, this.statementCache);
        } else {
            throw new IllegalArgumentException(String.format("Statement '%s' cannot be created. This is often due to the presence of both multiple statements and parameters at the same time.", sql));
        }
    }

    /**
     * Returns a snapshot of the current parameter statuses.
     *
     * @return a snapshot of the current parameter statuses
     */
    public Map<String, String> getParameterStatus() {
        return this.client.getParameterStatus();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @Override
    public Mono<Void> releaseSavepoint(String name) {
        Objects.requireNonNull(name, "name must not be null");

        return assertTransactionStatus(OPEN::equals, () -> String.format("Connection must have an open transaction in order to release the %s savepoint", name))
            .thenMany(SimpleQueryMessageFlow.exchange(this.client, String.format("RELEASE SAVEPOINT %s", name)))
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then();
    }

    @Override
    public Mono<Void> rollbackTransaction() {
        return assertTransactionStatus(OPEN::equals, () -> "Connection must have an open transaction in order to rollback a transaction")
            .thenMany(SimpleQueryMessageFlow.exchange(this.client, "ROLLBACK"))
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @Override
    public Mono<Void> rollbackTransactionToSavepoint(String name) {
        Objects.requireNonNull(name, "name must not be null");

        return assertTransactionStatus(OPEN::equals, () -> String.format("Connection must have an open transaction in order to rollback to the %s savepoint", name))
            .thenMany(SimpleQueryMessageFlow.exchange(this.client, String.format("ROLLBACK TO SAVEPOINT %s", name)))
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code isolationLevel} is {@code null}
     */
    @Override
    public Mono<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel) {
        Objects.requireNonNull(isolationLevel, "isolationLevel must not be null");

        return withTransactionStatus(getTransactionIsolationLevelQuery(isolationLevel))
            .flatMapMany(query -> SimpleQueryMessageFlow.exchange(this.client, query))
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code mutability} is {@code null}
     */
    @Override
    public Mono<Void> setTransactionMutability(Mutability mutability) {
        Objects.requireNonNull(mutability, "mutability must not be null");

        return withTransactionStatus(getTransactionMutabilityQuery(mutability))
            .flatMapMany(query -> SimpleQueryMessageFlow.exchange(this.client, query))
            .handle(PostgresqlServerErrorException::handleErrorResponse)
            .then();
    }

    @Override
    public String toString() {
        return "PostgresqlConnection{" +
            "client=" + this.client +
            '}';
    }

    private static Function<TransactionStatus, String> getTransactionIsolationLevelQuery(IsolationLevel isolationLevel) {
        return transactionStatus -> {
            if (transactionStatus == OPEN) {
                return String.format("SET TRANSACTION ISOLATION LEVEL %s", isolationLevel.asSql());
            } else {
                return String.format("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL %s", isolationLevel.asSql());
            }
        };
    }

    private static Function<TransactionStatus, String> getTransactionMutabilityQuery(Mutability mutability) {
        return transactionStatus -> {
            if (transactionStatus == OPEN) {
                return String.format("SET TRANSACTION %s", mutability.asSql());
            } else {
                return String.format("SET SESSION CHARACTERISTICS AS TRANSACTION %s", mutability.asSql());
            }
        };
    }

    private Mono<Void> assertTransactionStatus(Predicate<TransactionStatus> transactionStatus, Supplier<String> exceptionMessage) {
        return Mono.defer(() -> transactionStatus.test(this.client.getTransactionStatus()) ? Mono.empty() : Mono.error(new IllegalStateException(exceptionMessage.get())));
    }

    private <T> Mono<T> withTransactionStatus(Function<TransactionStatus, T> f) {
        return Mono.defer(() -> Mono.just(f.apply(this.client.getTransactionStatus())));
    }

}
