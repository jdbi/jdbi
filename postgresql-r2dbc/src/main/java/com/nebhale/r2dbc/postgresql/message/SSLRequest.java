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

import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeInt;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeSize;

public final class SSLRequest implements FrontendMessage {

    public static final SSLRequest INSTANCE = new SSLRequest();

    private static final int REQUEST_CODE = 80877103;

    private SSLRequest() {
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        ByteBuf out = allocator.ioBuffer(8);

        writeLengthPlaceholder(out);
        writeInt(out, REQUEST_CODE);

        return Mono.just(writeSize(out));
    }

    @Override
    public String toString() {
        return "SSLRequest{}";
    }

}
