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

package com.nebhale.r2dbc.postgresql;

import com.nebhale.r2dbc.Column;
import io.netty.buffer.ByteBuf;

import java.util.Objects;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * An implementation of {@link Column} for a PostgreSQL database.
 */
public final class PostgresqlColumn implements Column {

    private final ByteBuf byteBuf;

    PostgresqlColumn(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostgresqlColumn that = (PostgresqlColumn) o;
        return Objects.equals(this.byteBuf, that.byteBuf);
    }

    @Override
    public Integer getInteger() {
        String s = getString();
        return s == null ? null : Integer.parseInt(s);
    }

    @Override
    public String getString() {
        return this.byteBuf == null ? null : String.valueOf(this.byteBuf.readCharSequence(this.byteBuf.readableBytes(), UTF_8));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.byteBuf);
    }

    @Override
    public String toString() {
        return "PostgresqlColumn{" +
            "byteBuf=" + this.byteBuf +
            '}';
    }

}
