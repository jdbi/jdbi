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
import io.netty.buffer.ByteBuf;

/**
 * Encodes and decodes objects.
 */
public interface Codecs {

    /**
     * Decode a data to a value.
     *
     * @param byteBuf  the {@link ByteBuf} to decode
     * @param dataType the data type of the data
     * @param format   the format of the data
     * @param type     the type to decode to
     * @param <T>      the type of item being returned
     * @return the decoded value
     */
    <T> T decode(ByteBuf byteBuf, int dataType, Format format, Class<? extends T> type);

    /**
     * Encode a value.
     *
     * @param value the value to encode
     * @return the encoded value
     */
    Parameter encode(Object value);

}
