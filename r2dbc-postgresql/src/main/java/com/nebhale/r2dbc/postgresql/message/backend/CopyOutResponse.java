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

import com.nebhale.r2dbc.postgresql.message.Format;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Objects;

/**
 * The CopyOutResponse message.
 */
public final class CopyOutResponse extends AbstractCopyResponse {

    /**
     * Creates a new message.
     *
     * @param columnFormats the column formats
     * @param overallFormat the overall format
     */
    public CopyOutResponse(List<Format> columnFormats, Format overallFormat) {
        super(columnFormats, overallFormat);
    }

    @Override
    public String toString() {
        return "CopyOutResponse{} " + super.toString();
    }

    static CopyOutResponse decode(ByteBuf in) {
        Objects.requireNonNull(in, "in must not be null");

        Format overallFormat = Format.valueOf(in.readByte());
        List<Format> columnFormats = readColumnFormats(in);

        return new CopyOutResponse(columnFormats, overallFormat);
    }

}
