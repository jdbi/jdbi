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

package com.nebhale.r2dbc.postgresql.codec;

import com.nebhale.r2dbc.postgresql.client.Parameter;
import com.nebhale.r2dbc.postgresql.message.Format;
import com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.UUID;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static java.util.Objects.requireNonNull;

final class UuidCodec extends AbstractCodec<UUID> {

    private final ByteBufAllocator byteBufAllocator;

    UuidCodec(ByteBufAllocator byteBufAllocator) {
        super(UUID.class);
        this.byteBufAllocator = requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
    }

    @Override
    public UUID decode(ByteBuf byteBuf, Format format, Class<? extends UUID> type) {
        requireNonNull(byteBuf, "byteBuf must not be null");

        return new UUID(byteBuf.readLong(), byteBuf.readLong());
    }

    @Override
    public Parameter doEncode(UUID value) {
        requireNonNull(value, "value must not be null");

        ByteBuf encoded = this.byteBufAllocator.buffer(16).writeLong(value.getMostSignificantBits()).writeLong(value.getLeastSignificantBits());
        return create(BINARY, PostgresqlObjectId.UUID, encoded);
    }

    @Override
    boolean doCanDecode(Format format, PostgresqlObjectId type) {
        requireNonNull(format, "format must not be null");
        requireNonNull(type, "type must not be null");

        return BINARY == format && PostgresqlObjectId.UUID == type;
    }

}
