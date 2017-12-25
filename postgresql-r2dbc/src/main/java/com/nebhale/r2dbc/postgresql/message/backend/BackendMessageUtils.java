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

package com.nebhale.r2dbc.postgresql.message.backend;

import io.netty.buffer.ByteBuf;

import static io.netty.util.CharsetUtil.UTF_8;

final class BackendMessageUtils {

    private static final byte TERMINAL = 0;

    private BackendMessageUtils() {
    }

    static ByteBuf getBody(ByteBuf in) {
        return in.readSlice(in.readInt() - 4);
    }

    static String readCStringUTF8(ByteBuf src) {
        String s = src.readCharSequence(src.bytesBefore(TERMINAL), UTF_8).toString();
        src.readByte();
        return s;
    }

}
