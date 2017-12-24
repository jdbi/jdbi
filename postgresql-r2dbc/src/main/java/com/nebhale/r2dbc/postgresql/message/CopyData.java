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
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.MESSAGE_OVERHEAD;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeByte;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeBytes;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeSize;

public final class CopyData implements BackendMessage, FrontendMessage {

    private final ByteBuf data;

    public CopyData(ByteBuf data) {
        this.data = Objects.requireNonNull(data, "data must not be ");
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        return Mono.defer(() -> {
            ByteBuf out = allocator.ioBuffer(MESSAGE_OVERHEAD + (this.data.writerIndex() - this.data.readerIndex()));

            writeByte(out, 'd');
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
        CopyData copyData = (CopyData) o;
        return Objects.equals(this.data, copyData.data);
    }

    public ByteBuf getData() {
        return this.data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data);
    }

    @Override
    public String toString() {
        return "CopyData{" +
            "data=" + this.data +
            '}';
    }

    static CopyData decode(ByteBuf in) {
        return new CopyData(in.readRetainedSlice(in.readableBytes()));
    }

}
