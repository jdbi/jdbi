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
package org.skife.jdbi.tweak;

import java.util.HashMap;
import java.util.Map;

/**
 * Statement locator which acts as a caching decorator around another.
 * Is used by default in conjuntion with the classpath statement locator.
 */
public class CachingStatementLocator implements StatementLocator
{
    private final Map cache;
    private StatementLocator locator;

    /**
     * Pass in the underlying locator
     */
    public CachingStatementLocator(StatementLocator locator)
    {
        this.locator = locator;
        cache = new HashMap();
    }

    /**
     * @param key name of the statement
     * @return raw SQL statement (may include named params)
     */
    public String load(String key)
    {
        if (cache.containsKey(key)) return (String) cache.get(key);

        final String sql = locator.load(key);
        if (sql != null)
        {
            synchronized (cache)
            {
                cache.put(key, sql);
            }
        }
        return sql;
    }

    /**
     * Remove all cached statements
     */
    public void clear()
    {
        synchronized (cache)
        {
            cache.clear();
        }
    }

    /**
     * Remove a specific named statement from the cache
     *
     * @param key name of statement to remove
     * @return true if the cache changed, false otherwise
     */
    public boolean remove(String key)
    {
        synchronized(cache)
        {
            return cache.remove(key) != null;
        }
    }
}
