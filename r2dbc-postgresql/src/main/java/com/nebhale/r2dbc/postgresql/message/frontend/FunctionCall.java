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

import com.nebhale.r2dbc.postgresql.message.Format;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeByte;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeBytes;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeInt;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeShort;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeSize;
import static java.util.Objects.requireNonNull;

/**
 * The FunctionCall message.
 */
public final class FunctionCall implements FrontendMessage {

    private static final int NULL = -1;

    private final List<Format> argumentFormats;

    private final List<ByteBuf> arguments;

    private final int functionId;

    private final Format resultFormat;

    /**
     * Creates a new message.
     *
     * @param argumentFormats the argument formats
     * @param arguments       the value of the arguments, in the format indicated by the associated format
     * @param functionId      the object ID of the function to call
     * @param resultFormat    the format code for the function result
     * @throws NullPointerException if {@code argumentFormats}, {@code arguments}, or {@code resultFormat} is {@code null}
     */
    public FunctionCall(List<Format> argumentFormats, List<ByteBuf> arguments, int functionId, Format resultFormat) {
        this.argumentFormats = requireNonNull(argumentFormats, "argumentFormats must not be null");
        this.arguments = requireNonNull(arguments, "arguments must not be null");
        this.functionId = functionId;
        this.resultFormat = requireNonNull(resultFormat, "resultFormat must not be null");
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        requireNonNull(allocator, "allocator must not be null");

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
