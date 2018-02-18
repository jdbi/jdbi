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

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * The BackendKeyData message.
 */
public final class BackendKeyData implements BackendMessage {

    private final int processId;

    private final int secretKey;

    /**
     * Creates a new message.
     *
     * @param processId the process ID of this backend
     * @param secretKey the secret key of this backend
     */
    public BackendKeyData(int processId, int secretKey) {
        this.processId = processId;
        this.secretKey = secretKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BackendKeyData that = (BackendKeyData) o;
        return this.processId == that.processId &&
            this.secretKey == that.secretKey;
    }

    /**
     * Returns the process ID of this backend.
     *
     * @return the process ID of this backend
     */
    public int getProcessId() {
        return this.processId;
    }

    /**
     * Returns the secret key of this backend.
     *
     * @return the secret key of this backend
     */
    public int getSecretKey() {
        return this.secretKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.processId, this.secretKey);
    }

    @Override
    public String toString() {
        return "BackendKeyData{" +
            "processId=" + this.processId +
            ", secretKey=" + this.secretKey +
            '}';
    }

    static BackendKeyData decode(ByteBuf in) {
        requireNonNull(in, "in must not be null");

        return new BackendKeyData(in.readInt(), in.readInt());
    }

}
