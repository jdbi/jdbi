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
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeCStringUTF8;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeInt;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeShort;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeSize;

public final class Bind implements FrontendMessage {

    public static final String UNNAMED_PORTAL = "";

    public static final String UNNAMED_STATEMENT = "";

    private static final int NULL = -1;

    private final String name;

    private final List<Format> parameterFormats;

    private final List<ByteBuf> parameters;

    private final List<Format> resultFormats;

    private final String source;

    public Bind(String name, List<Format> parameterFormats, List<ByteBuf> parameters, List<Format> resultFormats, String source) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.parameterFormats = Objects.requireNonNull(parameterFormats, "parameterFormats must not be null");
        this.parameters = Objects.requireNonNull(parameters, "parameters must not be null");
        this.resultFormats = Objects.requireNonNull(resultFormats, "resultFormats must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        return Mono.defer(() -> {
            ByteBuf out = allocator.ioBuffer();

            writeByte(out, 'B');
            writeLengthPlaceholder(out);
            writeCStringUTF8(out, this.name);
            writeCStringUTF8(out, this.source);

            writeShort(out, this.parameterFormats.size());
            this.parameterFormats.forEach(format -> writeShort(out, format.getDiscriminator()));

            writeShort(out, this.parameters.size());
            this.parameters.forEach(parameters -> {
                if (parameters == null) {
                    writeInt(out, NULL);
                } else {
                    writeInt(out, parameters.readableBytes());
                    writeBytes(out, parameters);
                }
            });

            writeShort(out, this.resultFormats.size());
            this.resultFormats.forEach(format -> writeShort(out, format.getDiscriminator()));

            return Mono.just(writeSize(out));
        });
    }

}
