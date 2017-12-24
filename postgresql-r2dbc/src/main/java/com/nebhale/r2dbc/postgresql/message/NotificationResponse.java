/*
 * Copyright 2017-2017 the original author or authors.
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

package com.nebhale.r2dbc.postgresql.message;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.readCStringUTF8;

public final class NotificationResponse implements BackendMessage {

    private final String name;

    private final String payload;

    private final int processId;

    public NotificationResponse(String name, String payload, int processId) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.processId = processId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationResponse that = (NotificationResponse) o;
        return this.processId == that.processId &&
            Objects.equals(this.name, that.name) &&
            Objects.equals(this.payload, that.payload);
    }

    public String getName() {
        return this.name;
    }

    public String getPayload() {
        return this.payload;
    }

    public int getProcessId() {
        return this.processId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.payload, this.processId);
    }

    @Override
    public String toString() {
        return "NotificationResponse{" +
            "name='" + this.name + '\'' +
            ", payload='" + this.payload + '\'' +
            ", processId=" + this.processId +
            '}';
    }

    static NotificationResponse decode(ByteBuf in) {
        int processId = in.readInt();
        String name = readCStringUTF8(in);
        String payload = readCStringUTF8(in);

        return new NotificationResponse(name, payload, processId);
    }

}
