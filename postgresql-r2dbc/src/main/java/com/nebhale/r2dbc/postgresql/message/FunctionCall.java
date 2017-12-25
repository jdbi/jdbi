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

import java.util.List;
import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeByte;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeBytes;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeInt;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeShort;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeSize;

public final class FunctionCall implements FrontendMessage {

    private static final int NULL = -1;

    private final List<Format> argumentFormats;

    private final List<ByteBuf> arguments;

    private final int functionId;

    private final Format resultFormat;

    public FunctionCall(List<Format> argumentFormats, List<ByteBuf> arguments, int functionId, Format resultFormat) {
        this.argumentFormats = Objects.requireNonNull(argumentFormats, "argumentFormats must not be null");
        this.arguments = Objects.requireNonNull(arguments, "arguments must not be null");
        this.functionId = functionId;
        this.resultFormat = Objects.requireNonNull(resultFormat, "resultFormat must not be null");
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        return Mono.defer(() -> {
            ByteBuf out = allocator.ioBuffer();

            writeByte(out, 'F');
            writeLengthPlaceholder(out);
            writeInt(out, this.functionId);

            writeShort(out, this.argumentFormats.size());
            this.argumentFormats.forEach(format -> writeShort(out, format.getDiscriminator()));

            writeShort(out, this.arguments.size());
            this.arguments.forEach(argument -> {
                if (argument == null) {
                    writeInt(out, NULL);
                } else {
                    writeInt(out, argument.readableBytes());
                    writeBytes(out, argument);
                }
            });

            writeShort(out, this.resultFormat.getDiscriminator());

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
        FunctionCall that = (FunctionCall) o;
        return this.functionId == that.functionId &&
            Objects.equals(this.argumentFormats, that.argumentFormats) &&
            Objects.equals(this.arguments, that.arguments) &&
            this.resultFormat == that.resultFormat;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.argumentFormats, this.arguments, this.functionId, this.resultFormat);
    }

    @Override
    public String toString() {
        return "FunctionCall{" +
            "argumentFormats=" + this.argumentFormats +
            ", arguments=" + this.arguments +
            ", functionId=" + this.functionId +
            ", resultFormat=" + this.resultFormat +
            '}';
    }

}
