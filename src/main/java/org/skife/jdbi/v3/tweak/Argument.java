/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v3.tweak;

import org.skife.jdbi.v3.StatementContext;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Represents an argument to a prepared statement. It will be called right before the
 * statement is executed to bind the parameter.
 */
public interface Argument
{
    /**
     * Callback method invoked right before statement execution.
     *
     * @param position the position to which the argument should be bound, using the
     *                 stupid JDBC "start at 1" bit
     * @param statement the prepared statement the argument is to be bound to
     * @param ctx
     * @throws SQLException if anything goes wrong
     */
    void apply(final int position, PreparedStatement statement, StatementContext ctx) throws SQLException;
}
