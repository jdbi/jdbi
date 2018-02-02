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

import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.MESSAGE_OVERHEAD;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeByte;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeBytes;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeSize;
import static java.util.Objects.requireNonNull;

/**
 * The GSSResponse message.
 */
public final class GSSResponse implements FrontendMessage {

    private final ByteBuf data;

    /**
     * Creates a new message.
     *
     * @param data GSSAPI/SSPI specific message data
     * @throws NullPointerException if {@code data} is {@code null}
     */
    public GSSResponse(ByteBuf data) {
        this.data = requireNonNull(data, "data must not be null");
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        requireNonNull(allocator, "allocator must not be null");

        return Mono.defer(() -> {
            ByteBuf out = allocator.ioBuffer(MESSAGE_OVERHEAD + this.data.readableBytes());

            writeByte(out, 'p');
            writeLengthPlaceholder(out);
            writeBytes(out, this.data);

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
        GSSResponse that = (GSSResponse) o;
        return Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data);
    }

    @Override
    public String toString() {
        return "GSSResponse{" +
            "data=" + this.data +
            '}';
    }

}
