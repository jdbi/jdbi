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

package com.nebhale.r2dbc.postgresql.message.frontend;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeByte;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeBytes;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeCStringUTF8;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeInt;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeSize;
import static java.util.Objects.requireNonNull;

/**
 * The SASLInitialResponse Message.
 */
public final class SASLInitialResponse implements FrontendMessage {

    private final ByteBuf initialResponse;

    private final String name;

    /**
     * Creates a new message.
     *
     * @param initialResponse SASL mechanism specific "Initial Response"
     * @param name            name of the SASL authentication mechanism that the client selected
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public SASLInitialResponse(ByteBuf initialResponse, String name) {
        this.initialResponse = initialResponse;
        this.name = requireNonNull(name, "name must not be null");
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        requireNonNull(allocator, "allocator must not be null");

        return Mono.defer(() -> {
            ByteBuf out = allocator.ioBuffer();

            writeByte(out, 'p');
            writeLengthPlaceholder(out);
            writeCStringUTF8(out, this.name);

            if (this.initialResponse == null) {
                writeInt(out, -1);
            } else {
                writeInt(out, this.initialResponse.readableBytes());
                writeBytes(out, this.initialResponse);
            }

            return Mono.just(writeSize(out));
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SASLInitialResponse that = (SASLInitialResponse) o;
        return Objects.equals(this.initialResponse, that.initialResponse) &&
            Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.initialResponse, this.name);
    }

    @Override
    public String toString() {
        return "SASLInitialResponse{" +
            "initialResponse=" + initialResponse +
            ", name='" + name + '\'' +
            '}';
    }

}
