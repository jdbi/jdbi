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
package org.jdbi.v3.util;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @deprecated Use {@link URLColumnMapper} instead.
 */
@Deprecated
public class URLMapper extends TypedMapper<URL>
{

    public URLMapper()
    {
        super();
    }

    public URLMapper(int index)
    {
        super(index);
    }

    public URLMapper(String name)
    {
        super(name);
    }

    @Override
    protected URL extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getURL(name);
    }

    @Override
    protected URL extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getURL(index);
    }

    public static final URLMapper FIRST = new URLMapper();
}
