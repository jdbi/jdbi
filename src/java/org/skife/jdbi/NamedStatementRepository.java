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

import org.skife.jdbi.tweak.StatementLocator;
import org.skife.jdbi.tweak.ClasspathStatementLocator;
import org.skife.jdbi.tweak.CachingStatementLocator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class NamedStatementRepository
{
    private final Map store = new HashMap();

    private StatementLocator locator = new CachingStatementLocator(new ClasspathStatementLocator());

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
        String sql = (String) store.get(statement);
        if (sql == null)
        {
            sql = locator.load(statement);
        }
        return sql;
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

    public StatementLocator getLocator()
    {
        return locator;
    }

    public void setLocator(StatementLocator locator)
    {
        this.locator = locator;
    }
}
