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

import java.util.Objects;
import java.util.stream.Stream;

import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageUtils.readCStringUTF8;
import static java.util.Objects.requireNonNull;


/**
 * The CommandComplete message.
 */
public final class CommandComplete implements BackendMessage {

    private final String command;

    private final Integer rowId;

    private final Integer rows;

    /**
     * Creates a new message.
     *
     * @param command the command that was completed
     * @param rowId   the object ID of the inserted row
     * @param rows    the number of rows affected by the command
     * @throws NullPointerException if {@code command} is {@code null}
     */
    public CommandComplete(String command, Integer rowId, Integer rows) {
        this.command = requireNonNull(command, "command must not be null");
        this.rowId = rowId;
        this.rows = rows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CommandComplete that = (CommandComplete) o;
        return Objects.equals(this.command, that.command) &&
            Objects.equals(this.rowId, that.rowId) &&
            Objects.equals(this.rows, that.rows);
    }

    /**
     * Returns the command that was completed.
     *
     * @return the command that was completed
     */
    public String getCommand() {
        return this.command;
    }

    /**
     * Returns the object ID of the inserted row if a single row is inserted the target table has OIDs. Otherwise is {@code 0}.
     *
     * @return the object ID of the inserted row
     */
    public Integer getRowId() {
        return this.rowId;
    }

    /**
     * Returns the number of rows affected by the command.
     *
     * @return the number of rows affected by the command
     */
    public Integer getRows() {
        return this.rows;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.command, this.rowId, this.rows);
    }

    @Override
    public String toString() {
        return "CommandComplete{" +
            "command=" + this.command +
            ", rowId=" + this.rowId +
            ", rows=" + this.rows +
            '}';
    }

    static CommandComplete decode(ByteBuf in) {
        requireNonNull(in, "in must not be null");

        String tag = readCStringUTF8(in);

        if (tag.startsWith("INSERT")) {
            String[] tokens = tag.split(" ");
            return new CommandComplete(tokens[0], Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
        } else if (Stream.of("COPY", "DELETE", "FETCH", "MOVE", "SELECT", "UPDATE").anyMatch(tag::startsWith)) {
            String[] tokens = tag.split(" ");
            return new CommandComplete(tokens[0], null, Integer.parseInt(tokens[1]));
        } else {
            return new CommandComplete(tag, null, null);
        }
    }

}
