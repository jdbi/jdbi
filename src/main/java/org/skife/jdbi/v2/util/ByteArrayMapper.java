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
 * @deprecated Use {@link ByteArrayColumnMapper} instead.
 */
@Deprecated
public class ByteArrayMapper extends TypedMapper<byte[]>
{

    public ByteArrayMapper()
    {
        super();
    }

    public ByteArrayMapper(int index)
    {
        super(index);
    }

    public ByteArrayMapper(String name)
    {
        super(name);
    }

    @Override
    protected byte[] extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getBytes(name);
    }

    @Override
    protected byte[] extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getBytes(index);
    }

    public static final ByteArrayMapper FIRST = new ByteArrayMapper();
}
