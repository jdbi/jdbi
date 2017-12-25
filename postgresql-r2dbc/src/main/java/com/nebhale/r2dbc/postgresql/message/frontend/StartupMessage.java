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
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeByte;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeCString;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeCStringUTF8;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeLengthPlaceholder;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeShort;
import static com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessageUtils.writeSize;
import static io.netty.util.CharsetUtil.UTF_8;

/**
 * The StartupMessage message.
 */
public final class StartupMessage implements FrontendMessage {

    private static final ByteBuf APPLICATION_NAME = Unpooled.copiedBuffer("application_name", UTF_8).asReadOnly();

    private static final ByteBuf CLIENT_ENCODING = Unpooled.copiedBuffer("client_encoding", UTF_8).asReadOnly();

    private static final ByteBuf DATABASE = Unpooled.copiedBuffer("database", UTF_8).asReadOnly();

    private static final ByteBuf DATE_STYLE = Unpooled.copiedBuffer("DateStyle", UTF_8).asReadOnly();

    private static final ByteBuf EXTRA_FLOAT_DIGITS = Unpooled.copiedBuffer("extra_float_digits", UTF_8).asReadOnly();

    private static final ByteBuf ISO = Unpooled.copiedBuffer("ISO", UTF_8).asReadOnly();

    private static final ByteBuf NUMERAL_2 = Unpooled.copiedBuffer("2", UTF_8).asReadOnly();

    private static final ByteBuf USER = Unpooled.copiedBuffer("user", UTF_8).asReadOnly();

    private static final ByteBuf UTF8 = Unpooled.copiedBuffer("utf8", UTF_8).asReadOnly();

    private final String applicationName;

    private final String database;

    private final String username;

    /**
     * Creates a new message.
     *
     * @param applicationName the name of the application connecting to the database
     * @param database        the database to connect to. Defaults to the user name.
     * @param username        the database user name to connect as
     * @throws NullPointerException if {@code applicationName} or {@code username} is {@code null}
     */
    public StartupMessage(String applicationName, String database, String username) {
        this.applicationName = Objects.requireNonNull(applicationName, "applicationName must not be null");
        this.database = database;
        this.username = Objects.requireNonNull(username, "username must not be null");
    }

    @Override
    public Publisher<ByteBuf> encode(ByteBufAllocator allocator) {
        Objects.requireNonNull(allocator, "allocator must not be null");

        return Mono.defer(() -> {
            ByteBuf out = allocator.ioBuffer();

            writeLengthPlaceholder(out);
            writeShort(out, 3, 0);
            writeParameter(out, USER, this.username);

            if (this.database != null) {
                writeParameter(out, DATABASE, this.database);
            }

            writeParameter(out, APPLICATION_NAME, this.applicationName);
            writeParameter(out, CLIENT_ENCODING, UTF8);
            writeParameter(out, DATE_STYLE, ISO);
            writeParameter(out, EXTRA_FLOAT_DIGITS, NUMERAL_2);
            writeByte(out, 0);

            return Mono.just(writeSize(out, 0));
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
        StartupMessage that = (StartupMessage) o;
        return Objects.equals(this.applicationName, that.applicationName) &&
            Objects.equals(this.database, that.database) &&
            Objects.equals(this.username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.applicationName, this.database, this.username);
    }

    @Override
    public String toString() {
        return "StartupMessage{" +
            "applicationName='" + applicationName + '\'' +
            ", database='" + database + '\'' +
            ", username='" + username + '\'' +
            '}';
    }

    private void writeParameter(ByteBuf out, ByteBuf key, String value) {
        writeCString(out, key);
        writeCStringUTF8(out, value);
    }

    private void writeParameter(ByteBuf out, ByteBuf key, ByteBuf value) {
        writeCString(out, key);
        writeCString(out, value);
    }

}
