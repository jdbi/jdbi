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

import com.nebhale.r2dbc.core.nullability.Nullable;
import com.nebhale.r2dbc.postgresql.client.Parameter;
import com.nebhale.r2dbc.postgresql.message.Format;
import com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

final class DateCodec extends AbstractCodec<Date> {

    private final InstantCodec delegate;

    DateCodec(ByteBufAllocator byteBufAllocator) {
        super(Date.class);

        Objects.requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
        this.delegate = new InstantCodec(byteBufAllocator);
    }

    @Override
    public Date decode(ByteBuf byteBuf, @Nullable Format format, @Nullable Class<? extends Date> type) {
        Objects.requireNonNull("byteBuf must not be null");

        return Date.from(this.delegate.decode(byteBuf, format, Instant.class));
    }

    @Override
    public Parameter doEncode(Date value) {
        Objects.requireNonNull(value, "value must not be null");

        return this.delegate.doEncode(value.toInstant());
    }

    @Override
    boolean doCanDecode(Format format, PostgresqlObjectId type) {
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(type, "type must not be null");

        return this.delegate.doCanDecode(format, type);
    }

}
