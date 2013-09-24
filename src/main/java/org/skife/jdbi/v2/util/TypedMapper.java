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
package org.skife.jdbi.v2.util;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Convenience base class for implementing typed result set mappers. Provides
 * frequently used functionality.
 */
public abstract class TypedMapper<T> implements ResultSetMapper<T>
{
    private final ResultSetMapper<T> internal;

    /**
     * Create a new instance which extracts the value from the first column
     */
    public TypedMapper()
    {
        this(1);
    }

    /**
     * Create a new instance which extracts the value positionally
     * in the result set
     *
     * @param index 1 based column index into the result set
     */
    public TypedMapper(int index)
    {
        internal = new IndexMapper(index);
    }

    /**
     * Create a new instance which extracts the value by name or alias from the result set
     *
     * @param name The name or alias for the field
     */
    public TypedMapper(String name)
    {
        internal = new StringMapper(name);
    }

    public final T map(int index, ResultSet r, StatementContext ctx) throws SQLException
    {
        return internal.map(index, r, ctx);
    }

    abstract protected T extractByName(ResultSet r, String name) throws SQLException;

    abstract protected T extractByIndex(ResultSet r, int index) throws SQLException;


    private class StringMapper implements ResultSetMapper<T>
    {
        private final String name;

        StringMapper(String name)
        {
            this.name = name;
        }

        public T map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            return extractByName(r, name);
        }
    }

    private class IndexMapper implements ResultSetMapper<T>
    {
        private final int index;

        IndexMapper(int index)
        {
            this.index = index;
        }

        public T map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            return extractByIndex(r, this.index);
        }
    }
}
