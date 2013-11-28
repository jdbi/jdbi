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
package org.jdbi.v3.util;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Convenience ResultSetMapper for extracting a single value result
 * from a query.
 */
public class BooleanMapper extends TypedMapper<Boolean>
{
    /**
     * An instance which extracts value from the first field
     */
    public static final BooleanMapper FIRST = new BooleanMapper(1);


    /**
     * Create a new instance which extracts the value from the first column
     */
    public BooleanMapper()
    {
        super();
    }

    /**
     * Create a new instance which extracts the value positionally
     * in the result set
     *
     * @param index 1 based column index into the result set
     */
    public BooleanMapper(int index)
    {
        super(index);
    }

    /**
     * Create a new instance which extracts the value by name or alias from the result set
     *
     * @param name The name or alias for the field
     */
    public BooleanMapper(String name)
    {
        super(name);
    }

    @Override
    protected Boolean extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getBoolean(name);
    }

    @Override
    protected Boolean extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getBoolean(index);
    }
}
