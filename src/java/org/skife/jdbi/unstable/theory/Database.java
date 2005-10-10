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
package org.skife.jdbi.unstable.theory;

import org.skife.jdbi.DBI;
import org.skife.jdbi.Handle;

import java.util.HashMap;
import java.util.Map;

public class Database
{
    private final String connString;
    private final Map baseRelations = new HashMap();

    public Database(String connString)
    {
        this.connString = connString;
    }

    public Relation getRelation(String name)
    {
        synchronized (baseRelations)
        {
            if (baseRelations.containsKey(name))
            {
                return (Relation) baseRelations.get(name);
            }
            else
            {
                Relation r = new Relation(this, name);
                baseRelations.put(name, r);
                return r;
            }
        }
    }

    String getConnString() { return connString; }

    Handle getHandle()
    {
        return DBI.open(connString);
    }
}
