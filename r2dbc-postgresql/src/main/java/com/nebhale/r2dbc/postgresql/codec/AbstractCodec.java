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

import static java.util.Objects.requireNonNull;

abstract class AbstractCodec<T> implements Codec<T> {

    private final Class<T> type;

    AbstractCodec(Class<T> type) {
        this.type = requireNonNull(type, "type must not be null");
    }

    @Override
    public final boolean canDecode(ByteBuf byteBuf, int dataType, Format format, Class<?> type) {
        requireNonNull(format, "format must not be null");
        requireNonNull(type, "type must not be null");

        return byteBuf != null &&
            this.type.isAssignableFrom(type) &&
            doCanDecode(format, PostgresqlObjectId.valueOf(dataType));
    }

    @Override
    public final boolean canEncode(Object value) {
        requireNonNull(value, "value must not be null");

        return this.type.isInstance(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Parameter encode(Object value) {
        requireNonNull(value, "value must not be null");

        return doEncode((T) value);
    }

    static Parameter create(Format format, PostgresqlObjectId type, ByteBuf value) {
        requireNonNull(format, "format must not be null");
        requireNonNull(type, "type must not be null");

        return new Parameter(format, type.getObjectId(), value);
    }

    abstract boolean doCanDecode(Format format, PostgresqlObjectId type);

    abstract Parameter doEncode(T value);

}
