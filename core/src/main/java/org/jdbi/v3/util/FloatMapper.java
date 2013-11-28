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

public class FloatMapper extends TypedMapper<Float>
{

    public FloatMapper()
    {
        super();
    }

    public FloatMapper(int index)
    {
        super(index);
    }

    public FloatMapper(String name)
    {
        super(name);
    }

    @Override
    protected Float extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getFloat(name);
    }

    @Override
    protected Float extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getFloat(index);
    }

    public static final FloatMapper FIRST = new FloatMapper();
}
