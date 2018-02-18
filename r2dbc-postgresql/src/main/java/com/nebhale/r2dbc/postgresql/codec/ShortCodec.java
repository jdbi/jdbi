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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static com.nebhale.r2dbc.postgresql.message.Format.BINARY;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.INT2;
import static java.util.Objects.requireNonNull;

final class ShortCodec extends AbstractCodec<Short> {

    private final ByteBufAllocator byteBufAllocator;

    ShortCodec(ByteBufAllocator byteBufAllocator) {
        super(Short.class);
        this.byteBufAllocator = requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
    }

    @Override
    public Parameter doEncode(Short value) {
        requireNonNull(value, "value must not be null");

        ByteBuf encoded = this.byteBufAllocator.buffer(2).writeShort(value);
        return create(BINARY, INT2, encoded);
    }

}
