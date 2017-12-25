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

import java.util.Objects;

import static com.nebhale.r2dbc.postgresql.message.ByteBufUtils.readCStringUTF8;

public final class CommandComplete implements BackendMessage {

    private final Command command;

    private final Integer rowId;

    private final int rows;

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
        return this.rowId == that.rowId &&
            this.rows == that.rows &&
            this.command == that.command;
    }

    public Command getCommand() {
        return this.command;
    }

    public Integer getRowId() {
        return this.rowId;
    }

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
        String[] tokens = readCStringUTF8(in).split(" ");

        Command command = Command.valueOf(tokens[0]);
        Integer rowId = Command.INSERT == command ? Integer.parseInt(tokens[1]) : null;
        int rows = Integer.parseInt(tokens[tokens.length - 1]);

        return new CommandComplete(command, rowId, rows);
    }

    public enum Command {

        COPY,
        DELETE,
        FETCH,
        INSERT,
        MOVE,
        SELECT,
        UPDATE;

        @Override
        public String toString() {
            return "Command{} " + super.toString();
        }

    }

}
