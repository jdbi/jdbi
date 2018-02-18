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

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * The NoticeResponse message.
 */
public final class NoticeResponse implements BackendMessage {

    private final List<Field> fields;

    /**
     * Creates a new message.
     *
     * @param fields the fields
     * @throws NullPointerException if {@code fields} is {@code null}
     */
    public NoticeResponse(List<Field> fields) {
        this.fields = requireNonNull(fields, "fields must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NoticeResponse that = (NoticeResponse) o;
        return Objects.equals(this.fields, that.fields);
    }

    /**
     * Returns the fields.
     *
     * @return the fields
     */
    public List<Field> getFields() {
        return this.fields;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fields);
    }

    @Override
    public String toString() {
        return "NoticeResponse{" +
            "fields=" + this.fields +
            '}';
    }

    static NoticeResponse decode(ByteBuf in) {
        requireNonNull(in, "in must not be null");

        return new NoticeResponse(Field.decode(in));
    }

}
