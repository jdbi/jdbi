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

package com.nebhale.r2dbc.postgresql.message.backend;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.Objects;

/**
 * The ReadyForQuery message.
 */
public final class ReadyForQuery implements BackendMessage {

    private final TransactionStatus transactionStatus;

    /**
     * Creates a new message.
     *
     * @param transactionStatus the current backend transaction status
     * @throws NullPointerException if {@code transactionStatus} is {@code null}
     */
    public ReadyForQuery(TransactionStatus transactionStatus) {
        this.transactionStatus = Objects.requireNonNull(transactionStatus, "transactionStatus must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReadyForQuery that = (ReadyForQuery) o;
        return this.transactionStatus == that.transactionStatus;
    }

    /**
     * Returns the current backend transaction status.
     *
     * @return the current backend transaction status
     */
    public TransactionStatus getTransactionStatus() {
        return this.transactionStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.transactionStatus);
    }

    @Override
    public String toString() {
        return "ReadyForQuery{" +
            "transactionStatus=" + this.transactionStatus +
            '}';
    }

    static ReadyForQuery decode(ByteBuf in) {
        Objects.requireNonNull(in, "in must not be null");

        return new ReadyForQuery(TransactionStatus.valueOf(in.readByte()));
    }

    /**
     * An enumeration of backend transaction statuses.
     */
    public enum TransactionStatus {

        /**
         * The failed transaction status, represented by the {@code E} byte.
         */
        ERROR('E'),

        /**
         * The idle transaction status, represented by the {@code I} byte.
         */
        IDLE('I'),

        /**
         * The transaction transaction status, represented by the {@code T} byte.
         */
        TRANSACTION('T');

        private final char discriminator;

        TransactionStatus(char discriminator) {
            this.discriminator = discriminator;
        }

        @Override
        public String toString() {
            return "TransactionStatus{" +
                "discriminator=" + this.discriminator +
                "} " + super.toString();
        }

        static TransactionStatus valueOf(byte b) {
            return Arrays.stream(values())
                .filter(type -> type.discriminator == b)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("%c is not a valid transaction status", b)));
        }

    }

}
