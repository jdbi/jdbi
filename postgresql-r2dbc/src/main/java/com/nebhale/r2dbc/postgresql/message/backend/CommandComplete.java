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

import static com.nebhale.r2dbc.postgresql.message.backend.BackendMessageUtils.readCStringUTF8;


/**
 * The CommandComplete message.
 */
public final class CommandComplete implements BackendMessage {

    private final Command command;

    private final Integer rowId;

    private final int rows;

    /**
     * Creates a new message.
     *
     * @param command the command that was completed
     * @param rowId   the object ID of the inserted row
     * @param rows    the number of rows affected by the command
     * @throws NullPointerException if {@code command} is {@code null}
     */
    public CommandComplete(Command command, Integer rowId, int rows) {
        this.command = Objects.requireNonNull(command, "command must not be null");
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
        return this.rows == that.rows &&
            this.command == that.command &&
            Objects.equals(this.rowId, that.rowId);
    }

    /**
     * Returns the command that was completed.
     *
     * @return the command that was completed
     */
    public Command getCommand() {
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
    public int getRows() {
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
        Objects.requireNonNull(in, "in must not be null");

        String[] tokens = readCStringUTF8(in).split(" ");

        Command command = Command.valueOf(tokens[0]);
        Integer rowId = Command.INSERT == command ? Integer.parseInt(tokens[1]) : null;
        int rows = Integer.parseInt(tokens[tokens.length - 1]);

        return new CommandComplete(command, rowId, rows);
    }

    /**
     * An enumeration of the commands that can be executed.
     */
    public enum Command {

        /**
         * The {@code COPY} command.
         */
        COPY,

        /**
         * The {@code DELETE} command.
         */
        DELETE,

        /**
         * The {@code FETCH} command.
         */
        FETCH,

        /**
         * The {@code INSERT} command.
         */
        INSERT,

        /**
         * The {@code MOVE} command.
         */
        MOVE,

        /**
         * The {@code SELECT} command.
         */
        SELECT,

        /**
         * The {@code UPDATE} command.
         */
        UPDATE;

        @Override
        public String toString() {
            return "Command{} " + super.toString();
        }

    }

}
