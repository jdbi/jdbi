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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

final class UrlCodec extends AbstractCodec<URL> {

    private final StringCodec delegate;

    UrlCodec(ByteBufAllocator byteBufAllocator) {
        super(URL.class);

        Objects.requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
        this.delegate = new StringCodec(byteBufAllocator);
    }

    @Override
    public URL decode(ByteBuf byteBuf, @Nullable Format format, @Nullable Class<? extends URL> type) {
        Objects.requireNonNull(byteBuf, "byteBuf must not be null");

        try {
            return new URL(this.delegate.decode(byteBuf, format, String.class));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Parameter doEncode(URL value) {
        Objects.requireNonNull(value, "value must not be null");

        return this.delegate.doEncode(value.toString());
    }

    @Override
    boolean doCanDecode(Format format, PostgresqlObjectId type) {
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(type, "type must not be null");

        return this.delegate.doCanDecode(format, type);
    }

}
