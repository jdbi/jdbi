/*
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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Convenience ResultSetMapper for extracting a single value result
 * from a query.
 *
 * @deprecated Use {@link LongColumnMapper} instead.
 */
@Deprecated
public class LongMapper extends TypedMapper<Long>
{
    /**
     * An instance which extracts value from the first field
     */
    public static final LongMapper FIRST = new LongMapper(1);


    /**
     * Create a new instance which extracts the value from the first column
     */
    public LongMapper()
    {
        super();
    }

    /**
     * Create a new instance which extracts the value positionally
     * in the result set
     *
     * @param index 1 based column index into the result set
     */
    public LongMapper(int index)
    {
        super(index);
    }

    /**
     * Create a new instance which extracts the value by name or alias from the result set
     *
     * @param name The name or alias for the field
     */
    public LongMapper(String name)
    {
        super(name);
    }

    @Override
    protected Long extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getLong(name);
    }

    @Override
    protected Long extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getLong(index);
    }
}
