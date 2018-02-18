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
import com.nebhale.r2dbc.postgresql.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.time.LocalTime;

import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.TIME;
import static java.util.Objects.requireNonNull;

final class LocalTimeCodec extends AbstractCodec<LocalTime> {

    private final ByteBufAllocator byteBufAllocator;

    LocalTimeCodec(ByteBufAllocator byteBufAllocator) {
        super(LocalTime.class);
        this.byteBufAllocator = requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
    }

    @Override
    public Parameter doEncode(LocalTime value) {
        requireNonNull(value, "value must not be null");

        ByteBuf encoded = ByteBufUtils.encode(this.byteBufAllocator, value.toString());
        return create(TEXT, TIME, encoded);
    }

}
