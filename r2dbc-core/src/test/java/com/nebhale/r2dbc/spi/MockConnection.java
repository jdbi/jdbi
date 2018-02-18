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

package com.nebhale.r2dbc.spi;

import reactor.core.publisher.Mono;

import static java.util.Objects.requireNonNull;

public final class MockConnection implements Connection {

    public static final MockConnection EMPTY = builder().build();

    private final Batch batch;

    private final Statement statement;

    private boolean beginTransactionCalled = false;

    private boolean closeCalled = false;

    private boolean commitTransactionCalled = false;

    private String createSavepointName;

    private String createStatementSql;

    private String releaseSavepointName;

    private boolean rollbackTransactionCalled = false;

    private String rollbackTransactionToSavepointName;

    private IsolationLevel setTransactionIsolationLevelIsolationLevel;

    private Mutability setTransactionMutabilityMutability;

    private MockConnection(Batch batch, Statement statement) {
        this.batch = batch;
        this.statement = statement;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Mono<Void> beginTransaction() {
        this.beginTransactionCalled = true;
        return Mono.empty();
    }

    @Override
    public Mono<Void> close() {
        this.closeCalled = true;
        return Mono.empty();
    }

    @Override
    public Mono<Void> commitTransaction() {
        this.commitTransactionCalled = true;
        return Mono.empty();
    }

    @Override
    public Batch createBatch() {
        if (this.batch == null) {
            throw new AssertionError("Unexpected call to createBatch()");
        }

        return this.batch;
    }

    @Override
    public Mono<Void> createSavepoint(String name) {
        this.createSavepointName = name;
        return Mono.empty();
    }

    @Override
    public Statement createStatement(String sql) {
        if (this.statement == null) {
            throw new AssertionError("Unexpected call to createStatement(String)");
        }

        this.createStatementSql = sql;
        return this.statement;
    }

    public String getCreateSavepointName() {
        return this.createSavepointName;
    }

    public String getCreateStatementSql() {
        return this.createStatementSql;
    }

    public String getReleaseSavepointName() {
        return this.releaseSavepointName;
    }

    public String getRollbackTransactionToSavepointName() {
        return this.rollbackTransactionToSavepointName;
    }

    public IsolationLevel getSetTransactionIsolationLevelIsolationLevel() {
        return this.setTransactionIsolationLevelIsolationLevel;
    }

    public Mutability getSetTransactionMutabilityMutability() {
        return this.setTransactionMutabilityMutability;
    }

    public boolean isBeginTransactionCalled() {
        return this.beginTransactionCalled;
    }

    public boolean isCloseCalled() {
        return this.closeCalled;
    }

    public boolean isCommitTransactionCalled() {
        return this.commitTransactionCalled;
    }

    public boolean isRollbackTransactionCalled() {
        return this.rollbackTransactionCalled;
    }

    @Override
    public Mono<Void> releaseSavepoint(String name) {
        this.releaseSavepointName = name;
        return Mono.empty();
    }

    @Override
    public Mono<Void> rollbackTransaction() {
        this.rollbackTransactionCalled = true;
        return Mono.empty();
    }

    @Override
    public Mono<Void> rollbackTransactionToSavepoint(String name) {
        this.rollbackTransactionToSavepointName = name;
        return Mono.empty();
    }

    @Override
    public Mono<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel) {
        this.setTransactionIsolationLevelIsolationLevel = isolationLevel;
        return Mono.empty();
    }

    @Override
    public Mono<Void> setTransactionMutability(Mutability mutability) {
        this.setTransactionMutabilityMutability = mutability;
        return Mono.empty();
    }

    @Override
    public String toString() {
        return "MockConnection{" +
            "batch=" + this.batch +
            ", statement=" + this.statement +
            ", beginTransactionCalled=" + this.beginTransactionCalled +
            ", closeCalled=" + this.closeCalled +
            ", commitTransactionCalled=" + this.commitTransactionCalled +
            ", createSavepointName='" + this.createSavepointName + '\'' +
            ", createStatementSql='" + this.createStatementSql + '\'' +
            ", releaseSavepointName='" + this.releaseSavepointName + '\'' +
            ", rollbackTransactionCalled=" + this.rollbackTransactionCalled +
            ", rollbackTransactionToSavepointName='" + this.rollbackTransactionToSavepointName + '\'' +
            ", setTransactionIsolationLevelIsolationLevel=" + this.setTransactionIsolationLevelIsolationLevel +
            ", setTransactionMutabilityMutability=" + this.setTransactionMutabilityMutability +
            '}';
    }

    public static final class Builder {

        private Batch batch;

        private Statement statement;

        private Builder() {
        }

        public Builder batch(Batch batch) {
            this.batch = requireNonNull(batch);
            return this;
        }

        public MockConnection build() {
            return new MockConnection(this.batch, this.statement);
        }

        public Builder statement(Statement statement) {
            this.statement = requireNonNull(statement);
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "batch=" + this.batch +
                ", statement=" + this.statement +
                '}';
        }

    }

}
