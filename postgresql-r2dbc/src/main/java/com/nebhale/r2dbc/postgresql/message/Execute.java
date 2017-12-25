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

import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeByte;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeCStringUTF8;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeInt;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.writeSize;

public final class Execute implements FrontendMessage {

    public static final int NO_LIMIT = 0;

    public static final String UNNAMED_PORTAL = "";

    private final String name;

    private final int rows;

    public Execute(String name, int rows) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.rows = rows;
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        return Mono.defer(() -> {
            ByteBuf out = allocator.ioBuffer();

            writeByte(out, 'E');
            writeLengthPlaceholder(out);
            writeCStringUTF8(out, this.name);
            writeInt(out, this.rows);

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
        Execute that = (Execute) o;
        return this.rows == that.rows &&
            Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, rows);
    }

    @Override
    public String toString() {
        return "Execute{" +
            "name='" + this.name + '\'' +
            ", rows=" + this.rows +
            '}';
    }

}
