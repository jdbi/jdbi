/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import java.util.HashMap;

/**
 * Does *not* abide by the Map contracts. Converts all keys to uppercase strings
 */
class RowMap extends HashMap
{
    public Object put(Object key, Object value)
    {
        return super.put(String.valueOf(key).toUpperCase(), value);
    }

    public Object get(Object key)
    {
        return super.get(String.valueOf(key).toUpperCase());
    }

    public boolean containsKey(Object o)
    {
        return super.containsKey(String.valueOf(o).toUpperCase());
    }
}
