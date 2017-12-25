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

public final class DataRow implements BackendMessage {

    private static final int NULL = -1;

    private final List<ByteBuf> columns;

    public DataRow(List<ByteBuf> columns) {
        this.columns = Objects.requireNonNull(columns, "columns must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataRow dataRow = (DataRow) o;
        return Objects.equals(this.columns, dataRow.columns);
    }

    public List<ByteBuf> getColumns() {
        return columns;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.columns);
    }

    @Override
    public String toString() {
        return "DataRow{" +
            "columns=" + this.columns +
            '}';
    }

    static DataRow decode(ByteBuf in) {
        List<ByteBuf> columns = IntStream.range(0, in.readShort())
            .mapToObj(i -> decodeColumn(in))
            .collect(Collectors.toList());

        return new DataRow(columns);
    }

    private static ByteBuf decodeColumn(ByteBuf in) {
        int length = in.readInt();
        return NULL == length ? null : in.readRetainedSlice(length);
    }

}
