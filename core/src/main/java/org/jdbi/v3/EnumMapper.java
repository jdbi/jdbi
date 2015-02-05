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
package org.jdbi.v3;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.util.TypedMapper;

/**
 * Convenience ResultSetMapper for extracting a single enum result
 * from a query.
 */
public class EnumMapper<E extends Enum<E>> extends TypedMapper<E>
{
    private final Class<E> enumClass;

    /**
     * Create a new instance which extracts the value positionally
     * in the result set
     *
     * @param index 1 based column index into the result set
     */
    public EnumMapper(int index, Class<E> enumClass)
    {
        super(index);
        this.enumClass = enumClass;
    }

    /**
     * Create a new instance which extracts the value from the first column
     */
    public EnumMapper(Class<E> enumClass)
    {
        this(1, enumClass);
    }

    /**
     * Create a new instance which extracts the value by name or alias from the result set
     *
     * @param name The name or alias for the field
     */
    public EnumMapper(String name, Class<E> enumClass)
    {
        super(name);
        this.enumClass = enumClass;
    }

    @Override
    protected E extractByName(ResultSet r, String name) throws SQLException
    {
        return Enum.valueOf(enumClass, r.getString(name));
    }

    @Override
    protected E extractByIndex(ResultSet r, int index) throws SQLException
    {
        return Enum.valueOf(enumClass, r.getString(index));
    }
}
