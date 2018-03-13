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

import com.nebhale.r2dbc.core.nullability.Nullable;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class MockConnection implements Connection {

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

    private MockConnection(@Nullable Batch batch, @Nullable Statement statement) {
        this.batch = batch;
        this.statement = statement;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MockConnection empty() {
        return builder().build();
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
        this.createSavepointName = Objects.requireNonNull(name);
        return Mono.empty();
    }

    @Override
    public Statement createStatement(String sql) {
        Objects.requireNonNull(sql);

        if (this.statement == null) {
            throw new AssertionError("Unexpected call to createStatement(String)");
        }

        this.createStatementSql = sql;
        return this.statement;
    }

    @Nullable
    public String getCreateSavepointName() {
        return this.createSavepointName;
    }

    @Nullable
    public String getCreateStatementSql() {
        return this.createStatementSql;
    }

    @Nullable
    public String getReleaseSavepointName() {
        return this.releaseSavepointName;
    }

    @Nullable
    public String getRollbackTransactionToSavepointName() {
        return this.rollbackTransactionToSavepointName;
    }

    @Nullable
    public IsolationLevel getSetTransactionIsolationLevelIsolationLevel() {
        return this.setTransactionIsolationLevelIsolationLevel;
    }

    @Nullable
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
        this.releaseSavepointName = Objects.requireNonNull(name);
        return Mono.empty();
    }

    @Override
    public Mono<Void> rollbackTransaction() {
        this.rollbackTransactionCalled = true;
        return Mono.empty();
    }

    @Override
    public Mono<Void> rollbackTransactionToSavepoint(String name) {
        this.rollbackTransactionToSavepointName = Objects.requireNonNull(name);
        return Mono.empty();
    }

    @Override
    public Mono<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel) {
        this.setTransactionIsolationLevelIsolationLevel = Objects.requireNonNull(isolationLevel);
        return Mono.empty();
    }

    @Override
    public Mono<Void> setTransactionMutability(Mutability mutability) {
        this.setTransactionMutabilityMutability = Objects.requireNonNull(mutability);
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
            this.batch = Objects.requireNonNull(batch);
            return this;
        }

        public MockConnection build() {
            return new MockConnection(this.batch, this.statement);
        }

        public Builder statement(Statement statement) {
            this.statement = Objects.requireNonNull(statement);
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
