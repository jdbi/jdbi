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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class NamedStatementRepository
{
    private final Map store = new HashMap();

    public Collection getNames()
    {
        return new ArrayList(store.keySet());
    }

    public boolean contains(String statement)
    {
        return store.containsKey(statement);
    }

    public String get(String statement)
    {
        return (String) store.get(statement);
    }

    public void store(String statement, String sql)
    {
        synchronized (store)
        {
            store.put(statement, sql);
        }
    }

    public Map getStore()
    {
        return new HashMap(store);
    }
}
