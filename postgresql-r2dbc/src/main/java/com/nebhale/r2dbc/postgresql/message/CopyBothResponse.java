/*
 * Copyright 2017-2017 the original author or authors.
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

package com.nebhale.r2dbc.postgresql.message;

import io.netty.buffer.ByteBuf;

import java.util.List;

public final class CopyBothResponse extends AbstractCopyResponse {

    public CopyBothResponse(List<Format> columnFormats, Format overallFormat) {
        super(columnFormats, overallFormat);
    }

    @Override
    public String toString() {
        return "CopyBothResponse{} " + super.toString();
    }

    static CopyBothResponse decode(ByteBuf in) {
        Format overallFormat = Format.valueOf(in.readByte());
        List<Format> columnFormats = readColumnFormats(in);

        return new CopyBothResponse(columnFormats, overallFormat);
    }

}
