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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

abstract class AbstractCopyResponse implements BackendMessage {

    private final List<Format> columnFormats;

    private final Format overallFormat;

    AbstractCopyResponse(List<Format> columnFormats, Format overallFormat) {
        this.columnFormats = Objects.requireNonNull(columnFormats, "columnFormats must not be null");
        this.overallFormat = Objects.requireNonNull(overallFormat, "overallFormat must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractCopyResponse that = (AbstractCopyResponse) o;
        return Objects.equals(this.columnFormats, that.columnFormats) &&
            this.overallFormat == that.overallFormat;
    }

    public final List<Format> getColumnFormats() {
        return this.columnFormats;
    }

    public final Format getOverallFormat() {
        return this.overallFormat;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.columnFormats, this.overallFormat);
    }

    @Override
    public String toString() {
        return "AbstractCopyResponse{" +
            "columnFormats=" + this.columnFormats +
            ", overallFormat=" + this.overallFormat +
            '}';
    }

    static List<Format> readColumnFormats(ByteBuf in) {
        return IntStream.range(0, in.readShort())
            .mapToObj(i -> Format.valueOf(in.readShort()))
            .collect(Collectors.toList());
    }

}
