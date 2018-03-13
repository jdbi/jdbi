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

package com.nebhale.r2dbc.postgresql.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.Objects;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * Utilities for working with {@link ByteBuf}s.
 */
public final class ByteBufUtils {

    private ByteBufUtils() {
    }

    /**
     * Decode a {@link ByteBuf} into a {@link String}.
     *
     * @param byteBuf the {@link ByteBuf} to decode
     * @return the {@link String} decoded from the {@link ByteBuf}
     * @throws NullPointerException if {@code byteBuf} is {@code null}
     */
    public static String decode(ByteBuf byteBuf) {
        Objects.requireNonNull(byteBuf, "byteBuf must not be null");

        return byteBuf.readCharSequence(byteBuf.readableBytes(), UTF_8).toString();
    }

    /**
     * Encode a {@link CharSequence} into a {@link ByteBuf}.
     *
     * @param byteBufAllocator the {@link ByteBufAllocator} to use to create a buffer
     * @param s                the {@link CharSequence} to encode
     * @return the {@link ByteBuf} with the {@link CharSequence} encoded within it
     * @throws NullPointerException if {@code byteBufAllocator} is {@code null}
     */
    public static ByteBuf encode(ByteBufAllocator byteBufAllocator, CharSequence s) {
        Objects.requireNonNull(byteBufAllocator, "byteBufAllocator must not be null");
        Objects.requireNonNull(s, "s must not be null");

        ByteBuf byteBuf = byteBufAllocator.buffer();
        byteBuf.writeCharSequence(s, UTF_8);
        return byteBuf;
    }

}
