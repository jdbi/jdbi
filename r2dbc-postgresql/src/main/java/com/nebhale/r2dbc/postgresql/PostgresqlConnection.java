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

import com.nebhale.r2dbc.postgresql.client.Client;
import com.nebhale.r2dbc.postgresql.client.PortalNameSupplier;
import com.nebhale.r2dbc.postgresql.client.SimpleQueryMessageFlow;
import com.nebhale.r2dbc.postgresql.client.TransactionStatus;
import com.nebhale.r2dbc.spi.Connection;
import com.nebhale.r2dbc.spi.IsolationLevel;
import com.nebhale.r2dbc.spi.Mutability;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.nebhale.r2dbc.postgresql.client.TransactionStatus.IDLE;
import static com.nebhale.r2dbc.postgresql.client.TransactionStatus.OPEN;
import static java.util.Objects.requireNonNull;

/**
 * An implementation of {@link Connection} for connecting to a PostgreSQL database.
 */
public final class PostgresqlConnection implements Connection {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Client client;

    private final PortalNameSupplier portalNameSupplier;

    private final StatementCache statementCache;

    PostgresqlConnection(Client client, PortalNameSupplier portalNameSupplier, StatementCache statementCache) {
        this.client = requireNonNull(client, "client must not be null");
        this.portalNameSupplier = requireNonNull(portalNameSupplier, "portalNameSupplier must not be null");
        this.statementCache = requireNonNull(statementCache, "statementCache must not be null");
    }

    @Override
    public Mono<Void> beginTransaction() {
        return useTransactionStatus(transactionStatus -> {
            if (IDLE == transactionStatus) {
                return SimpleQueryMessageFlow.exchange(this.client, "BEGIN")
                    .handle(PostgresqlServerErrorException::handleErrorResponse);
            } else {
                this.logger.debug("Skipping begin transaction because status is {}", transactionStatus);
                return Mono.empty();
            }
        });
    }

    @Override
    public Mono<Void> close() {
        return this.client.close()
            .then(Mono.empty());
    }

    @Override
    public Mono<Void> commitTransaction() {
        return useTransactionStatus(transactionStatus -> {
            if (OPEN == transactionStatus) {
                return SimpleQueryMessageFlow.exchange(this.client, "COMMIT")
                    .handle(PostgresqlServerErrorException::handleErrorResponse);
            } else {
                this.logger.debug("Skipping commit transaction because status is {}", transactionStatus);
                return Mono.empty();
            }
        });
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
        requireNonNull(name, "name must not be null");

        return useTransactionStatus(transactionStatus -> {
            if (OPEN == transactionStatus) {
                return SimpleQueryMessageFlow.exchange(this.client, String.format("SAVEPOINT %s", name))
                    .handle(PostgresqlServerErrorException::handleErrorResponse);
            } else {
                this.logger.debug("Skipping create savepoint because status is {}", transactionStatus);
                return Mono.empty();
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code sql} is {@code null}
     */
    @Override
    public PostgresqlStatement createStatement(String sql) {
        requireNonNull(sql, "sql must not be null");

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
        requireNonNull(name, "name must not be null");

        return useTransactionStatus(transactionStatus -> {
            if (OPEN == transactionStatus) {
                return SimpleQueryMessageFlow.exchange(this.client, String.format("RELEASE SAVEPOINT %s", name))
                    .handle(PostgresqlServerErrorException::handleErrorResponse);
            } else {
                this.logger.debug("Skipping release savepoint because status is {}", transactionStatus);
                return Mono.empty();
            }
        });
    }

    @Override
    public Mono<Void> rollbackTransaction() {
        return useTransactionStatus(transactionStatus -> {
            if (OPEN == transactionStatus) {
                return SimpleQueryMessageFlow.exchange(this.client, "ROLLBACK")
                    .handle(PostgresqlServerErrorException::handleErrorResponse);
            } else {
                this.logger.debug("Skipping rollback transaction because status is {}", transactionStatus);
                return Mono.empty();
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @Override
    public Mono<Void> rollbackTransactionToSavepoint(String name) {
        requireNonNull(name, "name must not be null");

        return useTransactionStatus(transactionStatus -> {
            if (OPEN == transactionStatus) {
                return SimpleQueryMessageFlow.exchange(this.client, String.format("ROLLBACK TO SAVEPOINT %s", name))
                    .handle(PostgresqlServerErrorException::handleErrorResponse);
            } else {
                this.logger.debug("Skipping rollback transaction to savepoint because status is {}", transactionStatus);
                return Mono.empty();
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code isolationLevel} is {@code null}
     */
    @Override
    public Mono<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel) {
        requireNonNull(isolationLevel, "isolationLevel must not be null");

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
        requireNonNull(mutability, "mutability must not be null");

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

    private Mono<Void> useTransactionStatus(Function<TransactionStatus, Publisher<?>> f) {
        return Flux.defer(() -> f.apply(this.client.getTransactionStatus()))
            .then();
    }

    private <T> Mono<T> withTransactionStatus(Function<TransactionStatus, T> f) {
        return Mono.defer(() -> Mono.just(f.apply(this.client.getTransactionStatus())));
    }

}
