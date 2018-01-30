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
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeByte;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeCStringUTF8;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeInt;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeShort;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeSize;

/**
 * The Parse message.
 */
public final class Parse implements FrontendMessage {

    /**
     * The unnamed statement.
     */
    public static final String UNNAMED_STATEMENT = "";

    /**
     * The unspecified data type.
     */
    public static final int UNSPECIFIED = 0;

    private final String name;

    private final List<Integer> parameters;

    private final String query;

    /**
     * Creates a new message.
     *
     * @param name       the name of the destination prepared statement (an empty string selects the unnamed prepared statement)
     * @param parameters the object IDs of the parameter data types. Placing a zero here is equivalent to leaving the type unspecified.
     * @param query      the query string to be parsed
     * @throws NullPointerException if {@code name}, {@code parameters}, or {@code query} is {@code null}
     * @see #UNNAMED_STATEMENT
     * @see #UNSPECIFIED
     */
    public Parse(String name, List<Integer> parameters, String query) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.parameters = Objects.requireNonNull(parameters, "parameters must not be null");
        this.query = Objects.requireNonNull(query, "query must not be null");
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        Objects.requireNonNull(allocator, "allocator must not be null");

        return Mono.defer(() -> {
            ByteBuf out = allocator.ioBuffer();

            writeByte(out, 'P');
            writeLengthPlaceholder(out);
            writeCStringUTF8(out, this.name);
            writeCStringUTF8(out, this.query);

            writeShort(out, this.parameters.size());
            this.parameters.forEach(parameter -> writeInt(out, parameter));

            return Mono.just(writeSize(out));
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Parse that = (Parse) o;
        return Objects.equals(this.name, that.name) &&
            Objects.equals(this.parameters, that.parameters) &&
            Objects.equals(this.query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.parameters, this.query);
    }

    @Override
    public String toString() {
        return "Parse{" +
            "name='" + this.name + '\'' +
            ", parameters=" + this.parameters +
            ", query='" + this.query + '\'' +
            '}';
    }

}
