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

package com.nebhale.r2dbc.postgresql.message.frontend;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import static io.netty.util.CharsetUtil.UTF_8;

final class FrontendMessageUtils {

    static final int MESSAGE_OVERHEAD = 1 + 4;

    private static final int LENGTH_PLACEHOLDER = 0;

    private static final byte TERMINAL = 0;

    private FrontendMessageUtils() {
    }

    static ByteBuf writeByte(ByteBuf out, int... values) {
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(values, "values must not be null");

        Arrays.stream(values)
            .forEach(out::writeByte);
        return out;
    }

    static ByteBuf writeBytes(ByteBuf out, ByteBuf in) {
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(in, "in must not be null");

        out.writeBytes(in);
        return out;
    }

    static ByteBuf writeBytes(ByteBuf out, ByteBuffer in) {
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(in, "in must not be null");

        out.writeBytes(in);
        return out;
    }

    static ByteBuf writeCString(ByteBuf out, ByteBuf in) {
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(in, "in must not be null");

        out.writeBytes(in, in.readerIndex(), in.readableBytes());
        out.writeByte(TERMINAL);
        return out;
    }

    static ByteBuf writeCStringUTF8(ByteBuf out, String s) {
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(s, "s must not be null");

        out.writeCharSequence(s, UTF_8);
        out.writeByte(TERMINAL);
        return out;
    }

    static ByteBuf writeInt(ByteBuf out, int... values) {
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(values, "values must not be null");

        Arrays.stream(values)
            .forEach(out::writeInt);
        return out;
    }

    static ByteBuf writeLengthPlaceholder(ByteBuf out) {
        Objects.requireNonNull(out, "out must not be null");

        out.writeInt(LENGTH_PLACEHOLDER);
        return out;
    }

    static ByteBuf writeShort(ByteBuf out, int... values) {
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(values, "values must not be null");

        Arrays.stream(values)
            .forEach(out::writeShort);
        return out;
    }

    static ByteBuf writeSize(ByteBuf out) {
        Objects.requireNonNull(out, "out must not be null");

        return writeSize(out, 1);
    }

    static ByteBuf writeSize(ByteBuf out, int startIndex) {
        Objects.requireNonNull(out, "out must not be null");

        out.setInt(startIndex, out.writerIndex() - startIndex);
        return out;
    }

}
